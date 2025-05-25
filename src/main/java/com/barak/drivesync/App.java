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

public class App {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java -jar DriveSync.jar <local_folder_path> <google_drive_folder_name>");
            System.exit(1);
        }

        String localFolderPath = args[0];
        String googleDriveFolderName = args[1];

        final var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final var JSON_FACTORY = GsonFactory.getDefaultInstance();

        InputStreamReader credentialsReader = new InputStreamReader(
                App.class.getResourceAsStream("/credentials.json"));

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
            JSON_FACTORY,
            credentialsReader
        );

        File tokenFolder = new File("tokens");
        if (!tokenFolder.exists()) {
            tokenFolder.mkdirs();
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            clientSecrets,
            Collections.singleton(DriveScopes.DRIVE))
            .setDataStoreFactory(new FileDataStoreFactory(tokenFolder))
            .setAccessType("offline")
            .build();

        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");

        Drive driveService = new Drive.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                credential)
                .setApplicationName("DriveSync")
                .build();

        FolderMonitor folderMonitor = new FolderMonitor(localFolderPath);

        GoogleDriveSync googleDriveSync;
        try {
            googleDriveSync = new GoogleDriveSync(driveService, googleDriveFolderName);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        // Initial sync before monitoring
        folderMonitor.initialSync(googleDriveSync);

        // Start monitoring the folder
        folderMonitor.startMonitoring(googleDriveSync);
    }
}