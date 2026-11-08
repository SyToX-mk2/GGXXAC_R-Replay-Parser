@echo off
setlocal EnableDelayedExpansion

:: ===== CONFIGURATION =====
set JAVA_HOME=C:\Program Files\Java\jdk-26
set PATH_TO_FX=D:\_programming\_java\javafx-sdk-23.0.2\lib
set PATH_TO_FX_JMODS=D:\_programming\_java\javafx-jmods-26
set PROJECT_DIR=D:\_programming\_java_eclipse\ReplayCruncher
set SQLITE_JAR=%PROJECT_DIR%\sqlite-jdbc-3.51.2.0.jar
set ICON_FILE=D:\_programming\_java_eclipse\ReplayCruncher\src\resources\icon.ico

:: ===== STEP 1: CLEAN =====
echo [1/6] Cleaning...
if exist "%PROJECT_DIR%\out" rmdir /s /q "%PROJECT_DIR%\out"
if exist "%PROJECT_DIR%\dist" rmdir /s /q "%PROJECT_DIR%\dist"
mkdir "%PROJECT_DIR%\out"
mkdir "%PROJECT_DIR%\out\resources"
mkdir "%PROJECT_DIR%\dist\input"

:: ===== STEP 2: COMPILE =====
echo [2/6] Compiling...
"%JAVA_HOME%\bin\javac" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,java.sql,java.desktop -d "%PROJECT_DIR%\out" --source-path "%PROJECT_DIR%\src" "%PROJECT_DIR%\src\module-info.java"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

"%JAVA_HOME%\bin\javac" --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,java.sql,java.desktop -d "%PROJECT_DIR%\out" --source-path "%PROJECT_DIR%\src" "%PROJECT_DIR%\src\application\Main.java" "%PROJECT_DIR%\src\controllers\SceneController.java" "%PROJECT_DIR%\src\controllers\ConfigSceneController.java" "%PROJECT_DIR%\src\controllers\ActivityCalendar.java"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

:: ===== STEP 3: COPY RESOURCES =====
echo [3/6] Copying resources...
copy "%PROJECT_DIR%\src\controllers\*.fxml" "%PROJECT_DIR%\out\controllers\"
copy "%PROJECT_DIR%\src\application\*.css" "%PROJECT_DIR%\out\application\"
copy "%PROJECT_DIR%\src\resources\*.png" "%PROJECT_DIR%\out\resources\"
copy "%PROJECT_DIR%\src\resources\*.ico" "%PROJECT_DIR%\out\resources\"

:: ===== STEP 4: CREATE JAR =====
echo [4/6] Creating JAR...
"%JAVA_HOME%\bin\jar" --create --file="%PROJECT_DIR%\dist\input\ReplayCruncheR.jar" --main-class=application.Main -C "%PROJECT_DIR%\out" .

copy "%SQLITE_JAR%" "%PROJECT_DIR%\dist\input\"

:: ===== STEP 5: JLINK =====
echo [5/6] Creating custom runtime...
"%JAVA_HOME%\bin\jlink" --module-path "%PATH_TO_FX_JMODS%;%JAVA_HOME%\jmods" --add-modules javafx.controls,javafx.fxml,java.sql,java.desktop --output "%PROJECT_DIR%\dist\jre" --strip-debug --compress zip-6 --no-header-files --no-man-pages
if errorlevel 1 (
    echo jlink failed!
    pause
    exit /b 1
)

:: ===== STEP 6: JPACKAGE =====
echo [6/6] Packaging...
"%JAVA_HOME%\bin\jpackage" --name "ReplayCruncheR" --input "%PROJECT_DIR%\dist\input" --main-jar "ReplayCruncheR.jar" --main-class "application.Main" --runtime-image "%PROJECT_DIR%\dist\jre" --type "app-image" --icon "%ICON_FILE%" --dest "%PROJECT_DIR%\dist"
if errorlevel 1 (
    echo jpackage failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build complete!
echo Output: %PROJECT_DIR%\dist\ReplayCruncheR
echo ========================================
pause