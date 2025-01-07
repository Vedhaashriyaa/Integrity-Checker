import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.*;

public class FileIntegrityChecker {
    private final Path directory;
    private final Map<Path, String> fileHashes;
    private final WatchService watchService;
    private final ExecutorService executorService;
    private final EmailNotifier emailNotifier;
    private static final Logger LOGGER = Logger.getLogger(FileIntegrityChecker.class.getName());
    private volatile boolean running = true;

    public FileIntegrityChecker(String directoryPath) throws IOException {
        this.directory = Paths.get(directoryPath);
        if (!Files.exists(this.directory)) {
            Files.createDirectories(this.directory); // Create the directory if it doesn't exist
            LOGGER.info("Directory created: " + directoryPath);
        } else if (!Files.isDirectory(this.directory)) {
            throw new IOException("Specified path is not a directory: " + directoryPath);
        }
        this.fileHashes = new ConcurrentHashMap<>();
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.emailNotifier = new EmailNotifier(); // Or adjust as needed
        setupLogger();
        initialScan();
    }



    private void setupLogger() throws IOException {
        FileHandler fh = new FileHandler("integrity_checker.log");
        fh.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(fh);
    }

    private void initialScan() throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                calculateAndStoreHash(file);
                return FileVisitResult.CONTINUE;
            }
        });
        directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
    }

    private String calculateHash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            LOGGER.severe("Hash calculation failed for " + file + ": " + e.getMessage());
            return null;
        }
    }

    private void calculateAndStoreHash(Path file) {
        String hash = calculateHash(file);
        if (hash != null) {
            fileHashes.put(file, hash);
            LOGGER.info("Stored hash for: " + file);
        }
    }

    public void startMonitoring() {
        CompletableFuture.runAsync(this::monitorChanges, executorService);
    }

    private void monitorChanges() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    handleFileEvent(event, (Path) key.watchable());
                }
                if (!key.reset()) {
                    LOGGER.severe("Watch key invalid. Stopping monitoring.");
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleFileEvent(WatchEvent<?> event, Path parentDir) {
        Path file = parentDir.resolve((Path) event.context());
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            calculateAndStoreHash(file);
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            String oldHash = fileHashes.get(file);
            String newHash = calculateHash(file);

            if (newHash != null && !newHash.equals(oldHash)) {
                String message = "File modified: " + file;
                LOGGER.warning(message);
                emailNotifier.sendAlert(message);
                fileHashes.put(file, newHash);
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            fileHashes.remove(file);
            String message = "File deleted: " + file;
            LOGGER.warning(message);
            emailNotifier.sendAlert(message);
        }
    }

    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            watchService.close();
        } catch (Exception e) {
            LOGGER.severe("Shutdown error: " + e.getMessage());
            executorService.shutdownNow();
        }
    }

}
