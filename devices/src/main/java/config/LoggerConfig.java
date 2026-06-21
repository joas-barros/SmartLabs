package config;

import java.io.*;

public class LoggerConfig {

    public static void setup(String logFileName) {
        try {
            // Find the best logs directory path (supports running from root or module dir)
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                File parentLogsDir = new File("../logs");
                if (parentLogsDir.exists() && parentLogsDir.isDirectory()) {
                    logsDir = parentLogsDir;
                } else {
                    logsDir.mkdirs();
                }
            }

            File logFile = new File(logsDir, logFileName);
            // Overwrite mode is used to clear log files every execution
            FileOutputStream fos = new FileOutputStream(logFile, false);

            // Capture original stdout and stderr
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            // Redirect stdout and stderr to both the console and the log file
            System.setOut(new PrintStream(new DualOutputStream(originalOut, fos), true));
            System.setErr(new PrintStream(new DualOutputStream(originalErr, fos), true));

        } catch (Exception e) {
            System.err.println("Failed to setup logging to file: " + e.getMessage());
        }
    }

    /**
     * Helper OutputStream that duplicates bytes to two target streams.
     */
    private static class DualOutputStream extends OutputStream {
        private final OutputStream consoleStream;
        private final OutputStream fileStream;

        public DualOutputStream(OutputStream consoleStream, OutputStream fileStream) {
            this.consoleStream = consoleStream;
            this.fileStream = fileStream;
        }

        @Override
        public void write(int b) throws IOException {
            consoleStream.write(b);
            fileStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            consoleStream.write(b, off, len);
            fileStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            consoleStream.flush();
            fileStream.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                consoleStream.close();
            } finally {
                fileStream.close();
            }
        }
    }
}
