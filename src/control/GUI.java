package control;

import sick.LMS400;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI implements ActionListener {

    private JButton connectButton;
    private JButton scanButton;
    private JButton resetButton;
    private JButton saveButton;
    private JButton deleteButton;
    private JButton oneScanButton;
    private JButton scan01sButton;
    private JButton scan10sButton;
    private JButton scan30sButton;
    private JButton scan01minButton;

    private JTextField filenameTextField;

    private int numberOfScans = 1;
    private final LMS400 lms;

    public GUI() {
        this.lms = new LMS400();
    }

    public void createWindow() {
        JFrame frame = new JFrame();

        JPanel communicationPanel = new JPanel();
        JPanel scanPanel = new JPanel();
        JPanel statusPanel = new JPanel();

        connectButton = new JButton("Connect");
        scanButton = new JButton("Start Scan");
        resetButton = new JButton("Reset");
        saveButton = new JButton("Save Data");
        deleteButton = new JButton("Delete Data");

        oneScanButton = new JButton("Single Scan");
        scan01sButton = new JButton("1 Second Scan");
        scan10sButton = new JButton("10 Seconds Scan");
        scan30sButton = new JButton("30 Seconds Scan");
        scan01minButton = new JButton("1 Minute Scan");

        filenameTextField = new JTextField(20);

        // Set up action listeners
        connectButton.addActionListener(this);
        scanButton.addActionListener(this);
        resetButton.addActionListener(this);
        saveButton.addActionListener(this);
        deleteButton.addActionListener(this);

        oneScanButton.addActionListener(this);
        scan01sButton.addActionListener(this);
        scan10sButton.addActionListener(this);
        scan30sButton.addActionListener(this);
        scan01minButton.addActionListener(this);

        // Initial button states
        scanButton.setEnabled(false);
        resetButton.setEnabled(false);
        saveButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Set up frame layout and components
        frame.getContentPane().add(BorderLayout.NORTH, communicationPanel);
        frame.getContentPane().add(BorderLayout.CENTER, scanPanel);
        frame.getContentPane().add(BorderLayout.SOUTH, statusPanel);

        communicationPanel.setLayout(new GridLayout(1, 3, 10, 0));
        scanPanel.setLayout(new GridLayout(1, 5, 10, 0));
        statusPanel.setLayout(new GridLayout(3, 1, 0, 10));

        communicationPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        scanPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        statusPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        communicationPanel.add(connectButton);
        communicationPanel.add(scanButton);
        communicationPanel.add(resetButton);

        scanPanel.add(oneScanButton);
        scanPanel.add(scan01sButton);
        scanPanel.add(scan10sButton);
        scanPanel.add(scan30sButton);
        scanPanel.add(scan01minButton);

        statusPanel.add(filenameTextField);
        statusPanel.add(saveButton);
        statusPanel.add(deleteButton);

        frame.setTitle("LMS400 Control Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == connectButton) {
            handleConnectButton();
        }

        if (event.getSource() == scanButton) {
            handleScanButton();
        }

        if (event.getSource() == resetButton) {
            lms.setReset(true);
        }

        if (event.getSource() == saveButton) {
            handleSaveButton();
        }

        if (event.getSource() == deleteButton) {
            handleDeleteButton();
        }

        if (event.getSource() == oneScanButton) {
            numberOfScans = 1;
        }

        if (event.getSource() == scan01sButton) {
            numberOfScans = 190;
        }

        if (event.getSource() == scan10sButton) {
            numberOfScans = 1900;
        }

        if (event.getSource() == scan30sButton) {
            numberOfScans = 5700;
        }

        if (event.getSource() == scan01minButton) {
            numberOfScans = 11400;
        }
    }

    private void handleConnectButton() {
        if (lms.toggleConnection()) {
            scanButton.setEnabled(true);
            resetButton.setEnabled(true);
            JOptionPane.showMessageDialog(null, "Connected successfully.");
        } else {
            scanButton.setEnabled(false);
            resetButton.setEnabled(false);
            JOptionPane.showMessageDialog(null, "Connection failed.");
        }
    }

    private void handleScanButton() {
        if (lms.scan(numberOfScans)) {
            lms.process();
            scanButton.setEnabled(false);
            resetButton.setEnabled(false);
            saveButton.setEnabled(true);
            deleteButton.setEnabled(true);
            JOptionPane.showMessageDialog(null, "Scan completed successfully.");
        } else {
            JOptionPane.showMessageDialog(null, "Failed to perform the scan.");
        }
    }

    private void handleSaveButton() {
        String name = filenameTextField.getText();

        if (name == null || name.isEmpty()) {
            lms.saveData();
        } else {
            lms.saveData(name);
        }

        lms.clearScanData();
        lms.clearStringData();
        scanButton.setEnabled(true);
        resetButton.setEnabled(true);
        saveButton.setEnabled(false);
        deleteButton.setEnabled(false);
        JOptionPane.showMessageDialog(null, "Data saved successfully.");
    }

    private void handleDeleteButton() {
        lms.clearScanData();
        lms.clearStringData();
        scanButton.setEnabled(true);
        resetButton.setEnabled(true);
        saveButton.setEnabled(false);
        deleteButton.setEnabled(false);
        JOptionPane.showMessageDialog(null, "Data deleted.");
    }
}