package se.holtab.communication;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;

/**
 * A manager for Modulab RS485 communication using jSerialComm.
 */
public class ModulabCommunication {

    // Manually declare the logger to resolve compilation issues
    private static final Logger log = LoggerFactory.getLogger(ModulabCommunication.class);

    private final String portName; // e.g., "COM3" or "/dev/ttyUSB0"
    private final int baudRate;
    private SerialPort serialPort;
    private boolean isConnected;
    private String currentStatus;

    public ModulabCommunication(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.isConnected = false;
        this.currentStatus = "Disconnected";
    }

    /**
     * Connects to the Modulab device via RS485 Serial Port.
     * @return true if connection is successful, false otherwise.
     */
    public boolean connect() {
        log.info("Attempting to connect to serial port {} at {} baud...", portName, baudRate);
        
        try {
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);
            
            // Set blocking mode to allow timeouts on read/write operations
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);

            if (serialPort.openPort()) {
                this.isConnected = true;
                this.currentStatus = "Connected";
                log.info("Successfully connected to port {}.", portName);
                return true;
            } else {
                this.isConnected = false;
                this.currentStatus = "Failed to open port";
                log.error("Failed to open serial port: {}. It might be in use or doesn't exist.", portName);
                return false;
            }
        } catch (Exception e) {
            this.isConnected = false;
            this.currentStatus = "Error: " + e.getMessage();
            log.error("Exception while connecting to {}: {}", portName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Disconnects from the Modulab device.
     */
    public void disconnect() {
        if (isConnected && serialPort != null) {
            try {
                serialPort.closePort();
                this.isConnected = false;
                this.currentStatus = "Disconnected";
                log.info("Disconnected from port {}.", portName);
            } catch (Exception e) {
                log.error("Error while disconnecting from {}: {}", portName, e.getMessage(), e);
            }
        }
    }

    /**
     * Sends a command to the Modulab device.
     * @param command The string command to send.
     * @return true if sent successfully.
     */
    public boolean sendCommand(String command) {
        if (!isConnected || serialPort == null) {
            log.warn("Cannot send command: Device is not connected.");
            return false;
        }
        
        try {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            int bytesWritten = serialPort.writeBytes(bytes, bytes.length);
            
            if (bytesWritten == bytes.length) {
                log.info("Sent {} bytes to {}: '{}'", bytesWritten, portName, command.trim());
                return true;
            } else {
                log.error("Failed to write all bytes. Wrote {} of {}.", bytesWritten, bytes.length);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception while sending command: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reads the response from the Modulab device.
     * @param timeoutMillis Maximum time to wait for a response (overrides default if needed).
     * @return A string containing the response data, or null if timeout/error.
     */
    public String readResponse(int timeoutMillis) {
        if (!isConnected || serialPort == null) {
            log.warn("Cannot read response: Device is not connected.");
            return null;
        }
        
        try {
            log.info("Waiting for response from {}...", portName);
            
            // Adjust timeout temporarily for this read
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, timeoutMillis, 1000);
            
            byte[] readBuffer = new byte[1024];
            int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
            
            if (numRead > 0) {
                String response = new String(readBuffer, 0, numRead, StandardCharsets.UTF_8);
                log.info("Received {} bytes: {}", numRead, response.trim());
                return response;
            } else {
                log.warn("Read timeout. No data received.");
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while reading response: {}", e.getMessage(), e);
            return null;
        } finally {
            // Restore default timeouts
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public String getStatus() {
        return currentStatus;
    }

    /**
     * Main method to demonstrate and test the communication class independently.
     */
    public static void main(String[] args) {
        log.info("--- Starting Modulab RS485 Communication Test ---");
        
        // List available ports for debugging
        SerialPort[] availablePorts = SerialPort.getCommPorts();
        log.info("Available serial ports:");
        for (SerialPort port : availablePorts) {
            log.info(" - {} ({})", port.getSystemPortName(), port.getDescriptivePortName());
        }

        if (availablePorts.length == 0) {
            log.error("No serial ports found! Please connect an RS485 adapter.");
            return;
        }

        // Use the first available port for the test (or hardcode e.g., "COM3" / "/dev/ttyUSB0")
        String testPort = availablePorts[0].getSystemPortName(); 
        int baudRate = 9600; // Common RS485 baud rate
        
        ModulabCommunication device = new ModulabCommunication(testPort, baudRate);
        
        if (device.connect()) {
            
            // Send a test command (replace with actual Modulab protocol command)
            device.sendCommand("READ_STATUS\r\n");
            
            // Wait for and read the response
            String response = device.readResponse(2000);
            log.info("Final processed response: {}", response);
            
            // Safely disconnect
            device.disconnect();
        } else {
            log.error("Failed to connect. Exiting test.");
        }
        
        log.info("--- Modulab RS485 Communication Test Complete ---");
    }
}