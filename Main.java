public class Main {
    public static void main(String[] args) {
        try {
            String directoryToMonitor = "path/to/monitor";
            String emailConfigPath = "email.properties";
            
            FileIntegrityChecker checker = new FileIntegrityChecker(
                directoryToMonitor, 
                emailConfigPath
            );
            
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
