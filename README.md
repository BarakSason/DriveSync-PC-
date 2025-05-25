# DriveSync

DriveSync is a Java application that synchronizes a local folder with a folder in your Google Drive.

## Prerequisites

- Java 11 or newer
- Google account

## Setup Instructions

### 1. Create a Google Cloud Project and OAuth Credentials

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (or select an existing one).
3. Enable the **Google Drive API** for your project.
4. In the sidebar, go to **APIs & Services > Credentials**.
5. Click **Create Credentials** > **OAuth client ID**.
6. Choose **Desktop app** as the application type.
7. Download the generated `credentials.json` file.

### 2. Place `credentials.json` in the Correct Directory

- Copy your downloaded `credentials.json` file into the following directory in your project:
  ```
  src/main/resources/credentials.json
  ```
- If you are running the packaged `.jar`, place `credentials.json` in the same directory as the `.jar` file, or ensure it is included in the JAR's resources.

### 3. Build and Run the Application

#### Build

Use your preferred build tool (e.g., Maven or Gradle) to build the project and create the `.jar` file.

#### Run

Run the application from the command line, providing the local folder path and the Google Drive folder name as arguments:

```sh
java -jar DriveSync.jar <local_folder_path> <google_drive_folder_name>
```

- `<local_folder_path>`: The path to the folder on your computer you want to sync.
- `<google_drive_folder_name>`: The name of the folder in your Google Drive to sync with.

### 4. First Run Authentication

- On first run, a browser window will open asking you to authorize the app to access your Google Drive.
- After authorization, tokens will be saved in a `tokens` directory for future use.

## Security Notice

**Do NOT share your `credentials.json` file or commit it to any public repository.**  
This file contains sensitive information unique to your Google Cloud project.

## Troubleshooting

- If you see a `NullPointerException` related to `credentials.json`, make sure the file is present in the correct directory.
- If you change your Google Drive folder name, you must re-run the app with the new name.

## License

[MIT](LICENSE)