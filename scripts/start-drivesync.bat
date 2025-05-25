REM scripts/start-drivesync.bat
REM === EDIT THE PATHS BELOW BEFORE USING ===

REM Path to your DriveSync.jar
set JAR_PATH=<path-to-your-DriveSync.jar>

REM Path to your local folder to sync
set LOCAL_FOLDER=<path-to-your-local-folder>

REM Name of your Google Drive folder
set DRIVE_FOLDER=<your-google-drive-folder-name>

REM Path to log file (optional)
set LOG_FILE=<path-to-your-log-file>

java -jar "%JAR_PATH%" "%LOCAL_FOLDER%" %DRIVE_FOLDER% > "%LOG_FILE%" 2>&1