package com.barak.drivesync;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;

public class DriveSync {

    public static void main(String[] args) throws Exception {
        // Check for required command-line arguments
        if (args.length < 2) {
            System.err.println("Usage: java -jar DriveSync.jar <local_folder_path> <google_drive_folder_name>");
            System.exit(1);
        }

        // Parse command-line arguments
        String localFolderPath = args[0];
        String googleDriveFolderName = args[1];

        // Initialize HTTP transport and JSON factory for Google API client
        final var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final var JSON_FACTORY = GsonFactory.getDefaultInstance();

        // Load OAuth 2.0 client secrets from the resource file
        InputStreamReader credentialsReader = new InputStreamReader(
                DriveSync.class.getResourceAsStream("/credentials.json"));

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
            JSON_FACTORY,
            credentialsReader
        );

        // Store OAuth tokens in the user's home directory to avoid permission issues
        File tokenFolder = new File(System.getProperty("user.home"), ".drivesync_tokens");
        if (!tokenFolder.exists()) {
            tokenFolder.mkdirs();
        }

        // Build the authorization flow for Google Drive API with offline access
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            clientSecrets,
            Collections.singleton(DriveScopes.DRIVE))
            .setDataStoreFactory(new FileDataStoreFactory(tokenFolder))
            .setAccessType("offline")
            .build();

        // Authorize the user and obtain credentials
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");

        // Build the Drive service client
        Drive driveService = new Drive.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                credential)
                .setApplicationName("DriveSync")
                .build();

        // Initialize the folder monitor for the local directory
        FolderMonitor folderMonitor = new FolderMonitor(localFolderPath);

        // Initialize the GoogleDriveSync helper with the Drive service and target folder
        GoogleDriveSync googleDriveSync;
        try {
            googleDriveSync = new GoogleDriveSync(driveService, googleDriveFolderName);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        // Perform an initial sync between the local and remote folders
        folderMonitor.initialSync(googleDriveSync);

        // Start monitoring the local folder for changes and sync with Google Drive
        folderMonitor.startMonitoring(googleDriveSync);
    }
}