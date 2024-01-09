package sick;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class LMS400 extends Thread {

    private final String hostname;
    private final int port;

    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    private boolean isConnected = false;
    private boolean isWorking = false;
    private boolean isReset = false;

    private final ArrayList<String> stringData = new ArrayList<>();
    private final ArrayList<ArrayList<Point>> scanData = new ArrayList<>();

    private static final char STX = 0x02;

    public LMS400() {
        this.hostname = "192.168.0.1";
        this.port = 2111;
    }

    public LMS400(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void setReset(boolean isReset) {
        this.isReset = isReset;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public boolean isReset() {
        return isReset;
    }

    public ArrayList<Point> getScanData(int index) {
        return scanData.get(index);
    }

    public void clearStringData() {
        stringData.clear();
    }

    public void clearScanData() {
        scanData.clear();
    }

    public boolean toggleConnection() {
        if (isConnected) {
            return disconnect();
        } else {
            return connect();
        }
    }

    public boolean connect() {
        if (isConnected) {
            return false; // Already connected
        }

        try (Socket socket = new Socket(hostname, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            this.socket = socket;
            this.out = out;
            this.in = in;
            this.isConnected = true;

            return true;
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
            return false;
        }
    }

    public boolean disconnect() {
        if (!isConnected) {
            return false; // Already disconnected
        }

        try {
            if (in != null) {
                in.close();
            }

            if (out != null) {
                out.close();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
            return false;
        } finally {
            // Set in, out, and socket as null and isConnected as false
            in = null;
            out = null;
            socket = null;
            isConnected = false;
        }

        return true;
    }

    /**
     * Performs the specified number of scans.
     * Uses the startScanning, measure, and stopScanning functions.
     *
     * @param N The number of scans to perform.
     * @return {@code true} if the scans were successful,
     *         {@code false} if not connected or if reset was used.
     */
    public boolean scan(int N) {
        // Check if connected
        if (!isConnected) {
            return false;
        }

        // Activate scanning
        startScanning();

        // Perform the required number of scans
        measure(N);

        // Deactivate scanning
        stopScanning();

        // Check if a reset occurred during scanning
        if (isReset) {
            isReset = false; // Reset the flag for subsequent scans
            return false;    // Scanning interrupted due to reset
        }

        return true; // Scanning successful
    }

    public void startScanning() {
        this.isWorking = command("sMN mLRreqdata 0021", "sMA mLRreqdata", "sAN mLRreqdata 00000000");
    }

    public void stopScanning() {
        boolean success = command("sMN mLRstopdata", "sMA mLRstopdata", "sAN mLRstopdata 00000000");
        this.isWorking = !success;
    }

    /**
     * Sends a SOPAS method by name ("sMN") and receives SOPAS method acknowledged ("sMA") and SOPAS answer ("sAN").
     * Uses send, receive, and check functions.
     *
     * @param cmd The SOPAS method to be sent.
     * @param a1  The expected SOPAS method acknowledged answer.
     * @param a2  The expected SOPAS answer.
     * @return {@code true} if received SOPAS method acknowledged and SOPAS answer,
     *         {@code false} if received SOPAS fault answer ("sFA") or the device is disconnected.
     */
    public boolean command(String cmd, String a1, String a2) {
        if (!isConnected) {
            return false;
        }

        send(cmd);

        String ans1 = receiveTelegram();

        if (check(ans1, a1) != 1) {
            return false;
        }

        String ans2 = receiveTelegram();

        return check(ans2, a2) == 1;
    }

    /**
     * Sends a command frame.
     * The frame begins with 4 times STX, 4 bytes of length in hex, the telegram, and ends with a checksum.
     *
     * @param cmd The command to be sent.
     */
    private void send(String cmd) {
        char checksum = 0;
        int len = cmd.length();

        try (OutputStream outputStream = socket.getOutputStream()) {
            // Send STX
            for (int i = 0; i < 4; i++) {
                outputStream.write(STX);
            }

            // Send length as hex
            for (int i = 0; i < 4; i++) {
                int temp = len / (int) Math.pow(16, (6 - 2 * i));
                outputStream.write(temp);
                len = len % (int) Math.pow(16, (6 - 2 * i));
            }

            // Send telegram and calculate checksum
            for (int i = 0; i < cmd.length(); i++) {
                outputStream.write(cmd.charAt(i));
                checksum = (char) (checksum ^ cmd.charAt(i));
            }

            // Send checksum
            outputStream.write(checksum);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }
    }

    /**
     * Receives a telegram answer.
     * Returns the length and telegram as a string.
     *
     * @return The received telegram as a string.
     */
    private String receiveTelegram() {
        if (!isConnected) {
            return null;
        }

        StringBuilder b = new StringBuilder();
        int c = 0;
        int len = 0;

        // Skip STX
        for (int i = 0; i < 4; i++) {
            try {
                c = this.in.read();
            } catch (IOException e) {
                e.printStackTrace(); // Handle or log the exception
            }
        }

        // Count length and save
        for (int i = 0; i < 4; i++) {
            try {
                c = this.in.read();
                b.append((char) c);
                len = len + (c * (int) Math.pow(16, (6 - 2 * i)));
            } catch (IOException e) {
                e.printStackTrace(); // Handle or log the exception
            }
        }

        // Save telegram
        for (int i = 0; i < len; i++) {
            try {
                c = this.in.read();
                b.append((char) c);
            } catch (IOException e) {
                e.printStackTrace(); // Handle or log the exception
            }
        }

        // Get checksum
        try {
            c = this.in.read();
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }

        // Return received telegram as string
        return b.toString();
    }

    /**
     * Checks the received answer against the expected answer.
     *
     * @param receivedAnswer The received answer from the device.
     * @param expectedAnswer The expected answer.
     * @return 1 if the answer is expected, -1 if it's an error ("sFA FF"), 0 if it's unexpected.
     */
    private int check(String receivedAnswer, String expectedAnswer) {
        if (receivedAnswer.equals(expectedAnswer)) {
            return 1; // Expected answer
        } else if (receivedAnswer.contains("sFA FF")) {
            return -1; // Error
        } else {
            stringData.add(receivedAnswer);
            return 0; // Unexpected answer
        }
    }

    /**
     * Measures data from the device.
     *
     * @return {@code true} if the measurement is successful, {@code false} if an exception occurs.
     */
    public boolean measure() {
        try {
            // Receive string
            String receivedString = receiveTelegram();

            // Add received string to stringData ArrayList
            stringData.add(receivedString);

            // Add measurement logic here if needed

            return true; // Measurement successful
        } catch (Exception e) {
            e.printStackTrace(); // Handle or log the exception
            return false; // Measurement failed
        }
    }

    /**
     * Measures data from the device a specified number of times.
     *
     * @param N The number of measurements to perform.
     * @return {@code true} if all measurements are successful, {@code false} if an exception occurs or reset interrupts.
     */
    public boolean measure(int N) {
        try {
            for (int i = 0; i < N; i++) {
                // Receive string
                String receivedString = receiveTelegram();

                // Add received string to stringData ArrayList
                stringData.add(receivedString);

                if (isReset) {
                    stringData.clear();
                    return false; // Measurement interrupted due to reset
                }
            }

            return true; // All measurements successful
        } catch (Exception e) {
            e.printStackTrace(); // Handle or log the exception
            return false; // Measurement failed due to exception
        }
    }

    /**
     * Processes the received string data by calling {@code processStringData} for each element in stringData.
     */
    public void process() {
        for (String stringDatum : stringData) {
            processStringData(stringDatum);
        }
    }

    /**
     * Processes the received data string and adds it to scanData.
     *
     * @param receivedData The received data string from the device.
     */
    private void processStringData(String receivedData) {
        ArrayList<Point> data = new ArrayList<>();

        int currentIndex = 0;   // Iterator for received string
        int telegramLength = extractLength(receivedData, currentIndex);
        currentIndex += 4;

        // ... (skip format and scaling)

        int startingAngle = extractValue(receivedData, currentIndex, 8);
        currentIndex += 8;

        int angularResolution = extractValue(receivedData, currentIndex, 12);
        currentIndex += 2;

        int numberOfPoints = extractValue(receivedData, currentIndex, 14);
        currentIndex += 2;

        // ... (skip scanning frequency, remission scaling, remission start and end values)
        currentIndex += 8;

        for (int pointIndex = 0; pointIndex < numberOfPoints; pointIndex++) {
            int distance = extractDistance(receivedData, currentIndex);
            currentIndex += 2;

            int angle = startingAngle + (angularResolution * pointIndex);

            // Add new point to ArrayList of Points
            data.add(new Point(distance, angle));
        }

        // Add ArrayList of Points to ArrayList of ArrayLists of Points
        scanData.add(data);
    }

    /**
     * Extracts the length from the received data string.
     *
     * @param receivedData The received data string from the device.
     * @param currentIndex The current index in the received data string.
     * @return The extracted length.
     */
    private int extractLength(String receivedData, int currentIndex) {
        int length = 0;
        for (int i = 0; i < 4; i++) {
            int charValue = receivedData.charAt(currentIndex + i);
            length += (int) (charValue * Math.pow(16, 6 - 2 * i));
        }
        return length;
    }

    /**
     * Extracts a value from the received data string.
     *
     * @param receivedData The received data string from the device.
     * @param currentIndex The current index in the received data string.
     * @param endIndex The end index for extracting the value.
     * @return The extracted value.
     */
    private int extractValue(String receivedData, int currentIndex, int endIndex) {
        int value = 0;
        for (int i = currentIndex; i < endIndex; i++) {
            int j = i - (endIndex - 8);
            int charValue = receivedData.charAt(i);
            value += (int) (charValue * Math.pow(16, 2 * j));
        }
        return value;
    }

    /**
     * Extracts the distance from the received data string.
     *
     * @param receivedData The received data string from the device.
     * @param currentIndex The current index in the received data string.
     * @return The extracted distance.
     */
    private int extractDistance(String receivedData, int currentIndex) {
        int temp1 = receivedData.charAt(currentIndex);
        int temp2 = receivedData.charAt(currentIndex + 1);
        int distance = temp2 * 256 + temp1;

        if (distance > 3000 || distance < 700) {
            distance = 0;
        }

        return distance;
    }


    public boolean printData() {
        if (scanData.isEmpty()) {
            return false; // No scan data available
        }

        for (int i = 0; i < scanData.size(); i++) {
            System.out.print("Scan " + i + " ");

            for (int j = 0; j < scanData.get(i).size(); j++) {
                Point point = scanData.get(i).get(j);
                System.out.print("(" + (int) point.getX() + "," + (int) point.getY() + ") ");
            }

            System.out.println(); // Move to the next line after printing each scan
        }

        return true; // Print completed
    }

    public boolean saveData() {
        String projectDirectory = System.getProperty("user.dir");
        String filePath = projectDirectory + File.separator + "LMS400_scandata.txt";
        return saveData(filePath);
    }

    public boolean saveData(String filename) {
        if (scanData.isEmpty()) {
            return false;
        }

        try {
            String projectDirectory = System.getProperty("user.dir");
            String filePath = projectDirectory + File.separator + filename + ".txt";
            saveScanDataToFile(filePath);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // Handle or log the exception
            return false;
        }
    }

    private void saveScanDataToFile(String filePath) throws FileNotFoundException {
        try (PrintStream ps = new PrintStream(new FileOutputStream(filePath))) {
            for (int i = 0; i < scanData.size(); i++) {
                for (int j = 0; j < scanData.get(i).size(); j++) {
                    Point point = scanData.get(i).get(j);
                    ps.println(point.getX() + " " + (double) i + " " + (0 - point.getY()));
                }
            }
        }
    }

    public boolean simulateScan(int N) {
        final int DEFAULT_DISTANCE = 1500;
        final int STARTING_ANGLE = 550000;
        final int ANGULAR_RESOLUTION = 2500;
        final int NUMBER_OF_POINTS = 280;

        for (int i = 0; i < N; i++) {
            if (this.isReset) {
                break;
            }

            ArrayList<Point> data = new ArrayList<>();

            for (int j = 0; j < NUMBER_OF_POINTS; j++) {
                int angle = STARTING_ANGLE + (ANGULAR_RESOLUTION * j);
                data.add(new Point(DEFAULT_DISTANCE, angle));
            }

            scanData.add(data);
        }

        return true;
    }
}