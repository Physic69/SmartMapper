@echo off
echo ==========================================================
echo SmartMapper SLAM - Local Dependency Setup Script
echo ==========================================================
echo Copying complete third-party dependencies from 8.Testing_final...

set SOURCE_DIR=..\..\8.Testing_final\ORB_SLAM3_AR-for-Android

if not exist "%SOURCE_DIR%" (
    echo Error: Source directory %SOURCE_DIR% not found.
    echo Please follow the instructions in README.md to download dependencies.
    exit /b 1
)

echo [1/4] Setting up boost headers...
xcopy /E /I /Y "%SOURCE_DIR%\app\src\main\cpp\boost\boost-1_72_0" "app\src\main\cpp\boost\boost-1_72_0\" > nul

echo [2/4] Setting up Eigen headers...
xcopy /E /I /Y "%SOURCE_DIR%\app\src\main\cpp\Eigen\eigen-3.4.0" "app\src\main\cpp\Eigen\eigen-3.4.0\" > nul

echo [3/4] Setting up Sophus...
xcopy /E /I /Y "%SOURCE_DIR%\app\src\main\cpp\ORB\Thirdparty\Sophus" "app\src\main\cpp\ORB\Thirdparty\Sophus\" > nul

echo [4/4] Setting up OpenCV SDK...
xcopy /E /I /Y "%SOURCE_DIR%\app\src\main\cpp\opencv\opencv-4.12.0\include" "app\src\main\cpp\opencv\opencv-4.12.0\include\" > nul
xcopy /E /I /Y "%SOURCE_DIR%\opencvLibrary\java" "opencvLibrary\java\" > nul
xcopy /E /I /Y "%SOURCE_DIR%\opencvLibrary\libcxx_helper" "opencvLibrary\libcxx_helper\" > nul

echo.
echo ==========================================================
echo Dependency setup complete! You can now run:
echo   gradlew assembleDebug
echo ==========================================================
