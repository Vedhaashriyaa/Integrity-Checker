public class Main {
    public static void main(String[] args) {
        try {
            String directoryToMonitor = "C:\\Users\\user\\Documents\\MonitorDirectory"; // Replace with a valid path
            // Specify the path to the directory

            // Update the call to the constructor to pass only the directory path
            FileIntegrityChecker checker = new FileIntegrityChecker(directoryToMonitor);

            checker.startMonitoring();

            // Shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(checker::shutdown));

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Application failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
