# SmartMapper Project

This repository contains two Android applications that demonstrate and compare different mobile navigation technologies:

1.  **`DeadReckoning`**: A classic Inertial Dead Reckoning (DR) system built in Kotlin.
2.  **`SLAM`**: A Visual-Inertial SLAM system (ORB-SLAM3) built in C++/NDK.

The goal is to provide a practical comparison between a pure IMU-based approach and a full visual-inertial SLAM solution.

## Application Details

### 1. DeadReckoning

A standard Android (Kotlin) app that uses the device's inertial sensors to calculate position. This app is intended to demonstrate the principles of dead reckoning and the problem of sensor drift.

**For full details, see the `DeadReckoning` README:**

* [**`DeadReckoning/README.md`**](DeadReckoning/README.md)

### 2. SLAM

A high-performance C++/NDK application that uses ORB-SLAM3 to perform visual-inertial tracking, which corrects for the drift seen in the `DeadReckoning` app.

**This is a complex NDK project. For build dependencies and setup instructions, see the `SLAM` README:**

* [**`SLAM/README.M`**](SLAM/README.md)
