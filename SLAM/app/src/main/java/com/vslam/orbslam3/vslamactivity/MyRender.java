package com.vslam.orbslam3.vslamactivity;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.vslam.orbslam3.vslamactivity.obj.Ball;
import com.vslam.orbslam3.vslamactivity.obj.Cube;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

public class MyRender implements Renderer {

    public static boolean flag = false;
    private Context context;

    private static boolean isSelected = true;

    Cube cube;
    Ball ball;
    private TrajectoryRenderer trajectoryRenderer;
    private int screenWidth = 0;
    private int screenHeight = 0;

    public MyRender(Context context) {
        this.context = context;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w("MyRender", "onSurfaceCreated");
        glClearColor(0.01f, 0.01f, 0.01f, 0.01f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        cube = new Cube(context);
        ball = new Ball(context);

        trajectoryRenderer = new TrajectoryRenderer();
        trajectoryRenderer.initialize();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        screenWidth = width;
        screenHeight = height;
    }

    public void onDrawFrame(GL10 gl) {
        glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        if (flag) {
            cube.draw();
            ball.draw();
            if (trajectoryRenderer != null) {
                trajectoryRenderer.draw(screenWidth, screenHeight);
            }
        }
    }

    public void updateTrajectory(float x, float z) {
        if (trajectoryRenderer != null) {
            trajectoryRenderer.updatePosition(x, z);
        }
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        if (Math.abs(normalizedX - MatrixState.x) <= 1 && Math.abs(normalizedY - MatrixState.y) <= 1)
            isSelected = true;
    }

    public void handleTouchDrag(float normalizedX, float normalizedY) {
        if (isSelected) {
            MatrixState.x = normalizedX;
            MatrixState.y = normalizedY;
        }
    }

    public void handleTouchUp(float normalizedX, float normalizedY) {
        isSelected = false;
    }
}