package com.vslam.orbslam3.vslamactivity;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple 2D minimap renderer for SLAM trajectory
 * Shows trajectory line and current position in top-left corner
 * Thread-safe for concurrent access from camera and OpenGL threads
 */
public class TrajectoryRenderer {

    private static final String TAG = "TrajectoryRenderer";

    // Shader programs
    private int shaderProgram;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    // Trajectory data (synchronized access)
    private final Object lock = new Object();
    private List<Float> trajectoryPoints = new ArrayList<>();
    private float currentX = 0f;
    private float currentZ = 0f;
    private float lastAddedX = Float.MAX_VALUE;  // ← ADD THIS
    private float lastAddedZ = Float.MAX_VALUE;  // ← ADD THIS

    // Minimap settings
    private static final float MINIMAP_SIZE = 0.4f;
    private static final float MINIMAP_SCALE = 0.1f;

    // Transformation matrices
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    /**
     * Initialize the renderer
     */
    public void initialize() {
        // Create shader program
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  gl_PointSize = 10.0;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        // Get handles
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        // Set up projection matrix for minimap (orthographic 2D)
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f);

        Log.i(TAG, "TrajectoryRenderer initialized");
    }

    /**
     * Update trajectory with new camera position
     * Thread-safe - can be called from camera thread
     */
// Minimum distance to move before adding new point (in meters)
    private static final float MOVEMENT_THRESHOLD = 0.05f; // 5cm

    public void updatePosition(float x, float z) {
        synchronized (lock) {
            currentX = x;
            currentZ = z;

            // Calculate distance from last added point
            float dx = x - lastAddedX;
            float dz = z - lastAddedZ;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            // Only add point if moved significantly OR first point
            if (trajectoryPoints.size() == 0 || distance >= MOVEMENT_THRESHOLD) {
                trajectoryPoints.add(x);
                trajectoryPoints.add(z);

                lastAddedX = x;
                lastAddedZ = z;

                // Limit trajectory points to last 500 positions
                if (trajectoryPoints.size() > 1000) {
                    trajectoryPoints.remove(0);
                    trajectoryPoints.remove(0);
                    // Also update the first point reference if we removed it
                    if (trajectoryPoints.size() >= 2) {
                        // Don't update lastAdded since we want threshold from current end
                    }
                }
            }
        }
    }


    /**
     * Draw the minimap
     * Thread-safe - called from OpenGL thread
     */
    public void draw(int screenWidth, int screenHeight) {
        // Create snapshot of trajectory data
        float[] pointsSnapshot;
        float snapX, snapZ;

        synchronized (lock) {
            if (trajectoryPoints.size() < 2) {
                return; // Need at least one point
            }

            // Copy data to avoid holding lock during rendering
            pointsSnapshot = new float[trajectoryPoints.size()];
            for (int i = 0; i < trajectoryPoints.size(); i++) {
                pointsSnapshot[i] = trajectoryPoints.get(i);
            }
            snapX = currentX;
            snapZ = currentZ;
        }

        GLES20.glUseProgram(shaderProgram);

        // Set up viewport for minimap (top-left corner)
        int minimapPixelSize = (int)(Math.min(screenWidth, screenHeight) * 0.25f);
        GLES20.glViewport(20, screenHeight - minimapPixelSize - 20, minimapPixelSize, minimapPixelSize);

        // Set MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw trajectory line (green)
        drawTrajectory(pointsSnapshot);

        // Draw current position (red dot)
        drawCurrentPosition(snapX, snapZ);

        // Restore viewport
        GLES20.glViewport(0, 0, screenWidth, screenHeight);
    }

    /**
     * Draw trajectory line from snapshot
     */
    private void drawTrajectory(float[] points) {
        if (points.length < 4) return;

        // Scale points
        float[] scaledPoints = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            scaledPoints[i] = points[i] * MINIMAP_SCALE;
        }

        // Create buffer
        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(scaledPoints.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(scaledPoints);
        vertexBuffer.position(0);

        // Set color (green)
        GLES20.glUniform4f(colorHandle, 0.0f, 1.0f, 0.0f, 1.0f);

        // Draw line strip
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glLineWidth(3.0f);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, scaledPoints.length / 2);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Draw current position marker
     */
    private void drawCurrentPosition(float x, float z) {
        // Scale current position
        float[] currentPos = new float[] {
                x * MINIMAP_SCALE,
                z * MINIMAP_SCALE
        };

        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(currentPos.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(currentPos);
        vertexBuffer.position(0);

        // Set color (red)
        GLES20.glUniform4f(colorHandle, 1.0f, 0.0f, 0.0f, 1.0f);

        // Draw point
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Clear trajectory data
     */
    public void clearTrajectory() {
        synchronized (lock) {
            trajectoryPoints.clear();
            currentX = 0f;
            currentZ = 0f;
        }
        Log.i(TAG, "Trajectory cleared");
    }

    /**
     * Load shader helper
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}