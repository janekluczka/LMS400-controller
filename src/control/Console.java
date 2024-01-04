package control;

import sick.LMS400;

public class Console {

    public static void main(String[] args) {
        LMS400 lms = new LMS400();

        System.out.println("Attempting to connect: " + java.time.LocalTime.now());

        boolean isConnected = lms.connect();

        if (!isConnected) {
            System.out.println("Failed to start scanning.");
            disconnect(lms);
            return;
        }

        System.out.println("Connected successfully.");
        System.out.println("Starting scanning: " + java.time.LocalTime.now());

        lms.startScanning();

        System.out.println("Scanning in progress...");
        System.out.println("Performing a single scan: " + java.time.LocalTime.now());

        boolean scanCompleted = lms.measure(1);

        if (!scanCompleted) {
            System.out.println("Failed to perform the scan.");
            disconnect(lms);
            return;
        }

        System.out.println("Scan completed successfully.");
        System.out.println("Stopping scanning: " + java.time.LocalTime.now());

        lms.stopScanning();

        System.out.println("Processing scan data: " + java.time.LocalTime.now());

        lms.process();

        String dataFileName = "BOXANDDOC";

        System.out.println("Saving data to file: " + java.time.LocalTime.now());

        boolean saveSuccessful = lms.saveData(dataFileName);

        if (!saveSuccessful) {
            System.out.println("Failed to save data.");
        } else {
            System.out.println("Data saved successfully to file: " + dataFileName);
        }

        disconnect(lms);
    }

    private static void disconnect(LMS400 lms) {
        System.out.println("Disconnecting: " + java.time.LocalTime.now());
        lms.disconnect();
    }
}
