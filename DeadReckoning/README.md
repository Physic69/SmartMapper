# DeadReckoning for SmartMapper

This Android application uses the device's inertial sensors to perform dead reckoning and track the device's position in real-time. It's a component of the larger SmartMapper project.

## Description

The DeadReckoning app captures data from the accelerometer and rotation vector sensors to calculate the device's velocity and position. It operates as a foreground service, allowing it to track movement even when the app is in the background. The user interface displays the calculated 3D position (X, Y, Z), the variance of the sensor readings, and a visual representation of the path traveled.

## Features

* **Background Tracking**: A foreground service ensures continuous tracking of the device's movement.
* **Real-time Position Estimation**: The app uses an enhanced velocity and position estimation algorithm to provide real-time updates.
* **Path Visualization**: A custom view on the main screen draws the path of the device's movement.
* **Status Updates**: The app provides status updates, including "Calibrating," "Tracking Active," and "Idle."
* **Sensor Data Preprocessing**: The app includes a sensor preprocessor to calibrate the accelerometer and reduce noise.

## How It Works

1.  **Calibration**: When the service starts, it calibrates the accelerometer by collecting a number of samples while the device is held still.
2.  **Sensor Data Collection**: The `MotionSensorService` listens for updates from the accelerometer and rotation vector sensor.
3.  **Data Preprocessing**: The raw sensor data is passed through a `SensorPreprocessor` to remove bias.
4.  **Position Estimation**: The preprocessed data is fed into the `EnhancedVelocityPositionEstimator`, which uses a motion state detection and a gravity filter to calculate the device's velocity and position.
5.  **UI Updates**: The `MainActivity` receives position and status updates from the service via a `BroadcastReceiver` and updates the UI accordingly. The `PathView` custom view draws the path based on the received coordinates.

## Key Classes

* **`MainActivity.kt`**: The main entry point of the application, responsible for handling user interactions and displaying data.
* **`MotionSensorService.kt`**: A foreground service that manages sensor listeners and the position estimation process.
* **`EnhancedVelocityPositionEstimator.kt`**: Contains the core logic for calculating the device's position from sensor data.
* **`SensorPreprocessor.kt`**: A utility class for calibrating the accelerometer and correcting for bias.
* **`PathView.kt`**: A custom `View` that draws the 2D path of the device on a canvas.
* **`PathViewModel.kt`**: A `ViewModel` that stores the path points and survives configuration changes.

## Permissions

The application requires the following permissions, which are declared in the `AndroidManifest.xml` file:

* `FOREGROUND_SERVICE_HEALTH`
* `HIGH_SAMPLING_RATE_SENSORS`
* `FOREGROUND_SERVICE`
* `BODY_SENSORS`
* `ACTIVITY_RECOGNITION`

## How to Run

1.  Open the `DeadReckoning` project in Android Studio.
2.  Build the project to resolve all dependencies.
3.  Run the app on an Android device with the required sensors (accelerometer and gyroscope/rotation vector).
4.  Grant the necessary permissions when prompted.
5.  Press the "Start Service" button to begin tracking. Hold the device still for a few moments to allow for calibration.
6.  Move the device around to see the path being drawn on the screen.
7.  Press the "Stop Service" button to stop tracking.

## Dependencies

The project uses the following dependencies:

* AndroidX Core KTX
* AndroidX AppCompat
* Material Components for Android
* JUnit
* AndroidX Test JUnit
* AndroidX Espresso Core
* Activity KTX
