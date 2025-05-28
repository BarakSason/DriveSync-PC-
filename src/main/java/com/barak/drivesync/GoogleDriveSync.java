package com.barak.drivesync;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GoogleDriveSync {
    private final Drive driveService;
    private final String driveFolderId;

    /**
     * Constructs a GoogleDriveSync instance and resolves the Google Drive folder ID by name.
     * Throws IllegalArgumentException if the folder is not found.
     */
    public GoogleDriveSync(Drive driveService, String folderName) {
        this.driveService = driveService;
        this.driveFolderId = findFolderIdByName(folderName);
        if (this.driveFolderId == null) {
            throw new IllegalArgumentException("Google Drive folder not found: " + folderName);
        }
    }

    /**
     * Uploads a file to the Google Drive folder.
     * If the file exists, it updates it; otherwise, it creates a new file.
     * Sets the modified time to match the local file.
     */
    public void uploadFile(Path filePath, long lastModified) throws java.io.IOException {
        String fileName = filePath.getFileName().toString();
        // Query for a file with the same name in the target Drive folder
        String query = "'" + driveFolderId + "' in parents and name='" + fileName + "' and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name, modifiedTime)")
                .execute();

        // Prepare the local file and its metadata for upload
        java.io.File localFile = filePath.toFile();
        com.google.api.client.http.FileContent mediaContent =
                new com.google.api.client.http.FileContent(null, localFile);

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setModifiedTime(new com.google.api.client.util.DateTime(lastModified));

        File uploadedFile;
        if (!result.getFiles().isEmpty()) {
            // File exists in Drive: update it (do NOT set parents)
            String fileId = result.getFiles().get(0).getId();
            uploadedFile = driveService.files().update(fileId, fileMetadata, mediaContent)
                    .setFields("id, modifiedTime")
                    .execute();
            System.out.println("File updated: " + fileName + " (ID: " + uploadedFile.getId() + ")");
        } else {
            // File does not exist in Drive: create it (set parents)
            fileMetadata.setParents(java.util.Collections.singletonList(driveFolderId));
            uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, modifiedTime")
                    .execute();
            System.out.println("File uploaded: " + fileName + " (ID: " + uploadedFile.getId() + ")");
        }
    }

    /**
     * Lists all file names in the Google Drive folder.
     */
    public Set<String> listFileNamesInDriveFolder() throws java.io.IOException {
        Set<String> fileNames = new HashSet<>();
        String query = "'" + driveFolderId + "' in parents and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(name)")
                .execute();
        for (File file : result.getFiles()) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    /**
     * Lists all file names and their modified times in the Google Drive folder.
     * Returns a map of file name to last modified time (in ms).
     */
    public Map<String, Long> listFileNamesAndModifiedTimesInDriveFolder() throws java.io.IOException {
        Map<String, Long> fileMap = new HashMap<>();
        String query = "'" + driveFolderId + "' in parents and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(name, modifiedTime)")
                .execute();
        for (File file : result.getFiles()) {
            if (file.getModifiedTime() != null) {
                fileMap.put(file.getName(), file.getModifiedTime().getValue());
            }
        }
        return fileMap;
    }

    /**
     * Deletes a file by name in the Google Drive folder.
     * If multiple files with the same name exist, deletes all of them.
     */
    public void deleteFileByName(String fileName) throws java.io.IOException {
        String query = "'" + driveFolderId + "' in parents and name='" + fileName + "' and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();
        for (File file : result.getFiles()) {
            driveService.files().delete(file.getId()).execute();
            System.out.println("Deleted from Google Drive: " + fileName);
        }
    }

    /**
     * Finds a Google Drive folder ID by its name.
     * Returns the folder ID if found, or null otherwise.
     */
    private String findFolderIdByName(String folderName) {
        try {
            String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
            FileList result = driveService.files().list()
                    .setQ(query)
                    .setFields("files(id, name, parents)")
                    .execute();

            if (result.getFiles().isEmpty()) {
                System.out.println("No folders found with name: " + folderName);
                return null;
            }

            File folder = result.getFiles().get(0);
            System.out.println("Using folder: " + folder.getName() + " (ID: " + folder.getId() + ")");
            return folder.getId();
        } catch (Exception e) {
            System.err.println("Error finding folder: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if the remote Google Drive folder is accessible.
     * Returns true if accessible, false otherwise.
     */
    public boolean isRemoteFolderAccessible() {
        try {
            com.google.api.services.drive.model.File folder = driveService.files().get(driveFolderId)
                    .setFields("id, name")
                    .execute();
            return folder != null && folder.getId() != null;
        } catch (Exception e) {
            System.err.println("Error accessing Google Drive folder: " + e.getMessage());
            return false;
        }
    }
}