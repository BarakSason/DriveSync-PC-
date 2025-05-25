package com.barak.drivesync;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardWatchEventKinds.*;

public class FolderMonitor {
    private final Path folderToMonitor;
    // Tracks last modified time to debounce ENTRY_MODIFY events
    private final Map<Path, Long> lastUploadedModifiedTime = new HashMap<>();

    public FolderMonitor(String folderPath) {
        this.folderToMonitor = Paths.get(folderPath);
    }

    public void initialSync(GoogleDriveSync googleDriveSync) {
        System.out.println("Performing initial sync...");

        // Check local folder exists and is accessible
        if (!Files.exists(folderToMonitor) || !Files.isDirectory(folderToMonitor)) {
            System.err.println("Local folder does not exist or is not a directory: " + folderToMonitor);
            return;
        }

        // Check remote (Google Drive) folder is accessible
        if (!googleDriveSync.isRemoteFolderAccessible()) {
            System.err.println("Google Drive folder is not accessible or does not exist.");
            return;
        }
        
        try {
            // 1. List all local files and their last modified times
            Map<String, Path> localFiles = new HashMap<>();
            Map<String, Long> localModifiedTimes = new HashMap<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderToMonitor)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        String fileName = entry.getFileName().toString();
                        localFiles.put(fileName, entry);
                        long lastModified = Files.getLastModifiedTime(entry).toMillis();
                        localModifiedTimes.put(fileName, lastModified);
                    }
                }
            }

            // 2. List all files in the Drive folder and their modified times
            Map<String, Long> driveFiles = googleDriveSync.listFileNamesAndModifiedTimesInDriveFolder();

            // 3. Upload new or changed files
            for (String fileName : localFiles.keySet()) {
                Long localTime = localModifiedTimes.get(fileName);
                Long driveTime = driveFiles.get(fileName);
                if (driveTime == null || (localTime != null && localTime > driveTime)) {
                    Path localPath = localFiles.get(fileName);
                    waitForFileReady(localPath);
                    try {
                        googleDriveSync.uploadFile(localPath, localTime);
                        System.out.println("Uploaded to Google Drive: " + fileName);
                    } catch (IOException e) {
                        System.err.println("Error uploading file " + fileName + ": " + e.getMessage());
                    }
                }
            }

            // 4. Delete files from Drive that were deleted locally
            for (String fileName : driveFiles.keySet()) {
                if (!localFiles.containsKey(fileName)) {
                    try {
                        googleDriveSync.deleteFileByName(fileName);
                        System.out.println("Deleted from Google Drive (not found locally): " + fileName);
                    } catch (IOException e) {
                        System.err.println("Error deleting file from Drive: " + fileName + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error during initial sync: " + e.getMessage());
        }
    }

    public void startMonitoring(GoogleDriveSync googleDriveSync) {
        System.out.println("Monitoring folder: " + folderToMonitor);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            folderToMonitor.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path fileName = (Path) event.context();
                    Path filePath = folderToMonitor.resolve(fileName);

                    if (kind == ENTRY_CREATE) {
                        System.out.println("File created: " + filePath);
                        try {
                            waitForFileReady(filePath);
                            long lastModified = Files.getLastModifiedTime(filePath).toMillis();
                            googleDriveSync.uploadFile(filePath, lastModified);
                        } catch (IOException e) {
                            System.err.println("Error uploading new file " + filePath.getFileName() + ": " + e.getMessage());
                        }
                    } else if (kind == ENTRY_MODIFY) {
                        try {
                            long currentModified = Files.getLastModifiedTime(filePath).toMillis();
                            Long lastUploaded = lastUploadedModifiedTime.get(filePath);
                            if (lastUploaded == null || currentModified > lastUploaded) {
                                waitForFileReady(filePath);
                                googleDriveSync.uploadFile(filePath, currentModified);
                                lastUploadedModifiedTime.put(filePath, currentModified);
                                System.out.println("File modified and uploaded: " + filePath.getFileName());
                            } else {
                                System.out.println("Modification ignored (already uploaded): " + filePath.getFileName());
                            }
                        } catch (IOException e) {
                            System.err.println("Error uploading modified file " + filePath.getFileName() + ": " + e.getMessage());
                        }
                    } else if (kind == ENTRY_DELETE) {
                        System.out.println("File deleted: " + filePath);
                        try {
                            googleDriveSync.deleteFileByName(fileName.toString());
                        } catch (IOException e) {
                            System.err.println("Error deleting file from Drive: " + fileName + ": " + e.getMessage());
                        }
                    }
                }
                // Reset the key to continue watching
                if (!key.reset()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error monitoring folder: " + e.getMessage());
        }
    }

    // Waits until the file is ready for reading (not locked)
    private void waitForFileReady(Path filePath) {
        int maxTries = 10;
        int tries = 0;
        while (tries < maxTries) {
            try {
                if (Files.exists(filePath)) {
                    Files.newInputStream(filePath).close();
                    return;
                } else {
                    return;
                }
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
            tries++;
        }
    }
}