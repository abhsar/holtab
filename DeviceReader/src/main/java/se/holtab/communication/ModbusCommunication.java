package se.holtab.communication;

import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A manager for Modulab communication using Modbus RTU over RS485 (via j2mod).
 */
public class ModbusCommunication {

    private static final Logger log = LoggerFactory.getLogger(ModbusCommunication.class);

    private final String portName; // e.g., "COM3" or "/dev/cu.usbserial-XXXX"
    private final int baudRate;
    private final int unitId;      // The Modbus slave/unit ID of the target device
    
    private ModbusSerialMaster modbusMaster;
    private boolean isConnected;
    private String currentStatus;

    /**
     * Constructor for ModbusCommunication.
     * @param portName The serial port name.
     * @param baudRate The communication speed.
     * @param unitId   The Modbus slave unit ID (typically 1-247).
     */
    public ModbusCommunication(String portName, int baudRate, int unitId) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.unitId = unitId;
        this.isConnected = false;
        this.currentStatus = "Disconnected";
    }

    /**
     * Connects to the Modulab device using Modbus RTU over RS485.
     * @return true if connection is successful, false otherwise.
     */
    public boolean connect() {
        log.info("Attempting to connect via Modbus RTU to port {} at {} baud (Unit ID: {})...", portName, baudRate, unitId);
        
        try {
            // Configure serial parameters for Modbus RTU
            SerialParameters params = new SerialParameters();
            params.setPortName(portName);
            params.setBaudRate(baudRate);
            params.setDatabits(8);
            params.setParity("None");
            params.setStopbits(1);
            params.setEncoding(Modbus.SERIAL_ENCODING_RTU); // Important for RS485 Modbus
            params.setEcho(false);

            // Initialize the Modbus master
            modbusMaster = new ModbusSerialMaster(params);
            modbusMaster.setTimeout(2000); // Set timeout to 2000ms

            modbusMaster.connect();
            
            this.isConnected = true;
            this.currentStatus = "Connected";
            log.info("Successfully established Modbus connection to {}.", portName);
            return true;
            
        } catch (Exception e) {
            this.isConnected = false;
            this.currentStatus = "Error: " + e.getMessage();
            log.error("Failed to connect to Modbus port {}: {}", portName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Disconnects the Modbus master.
     */
    public void disconnect() {
        if (isConnected && modbusMaster != null) {
            try {
                modbusMaster.disconnect();
                this.isConnected = false;
                this.currentStatus = "Disconnected";
                log.info("Disconnected Modbus master from port {}.", portName);
            } catch (Exception e) {
                log.error("Error while disconnecting Modbus from {}: {}", portName, e.getMessage(), e);
            }
        }
    }

    /**
     * Reads holding registers from the Modulab device.
     * @param startReference The starting register address (0-based).
     * @param count The number of registers to read.
     * @return Array of Register objects, or null if reading fails.
     */
    public Register[] readHoldingRegisters(int startReference, int count) {
        if (!isConnected || modbusMaster == null) {
            log.warn("Cannot read registers: Device is not connected.");
            return null;
        }

        try {
            log.info("Reading {} holding registers starting from reference {}...", count, startReference);
            Register[] registers = modbusMaster.readMultipleRegisters(unitId, startReference, count);
            log.info("Successfully read {} registers.", registers.length);
            return registers;
        } catch (Exception e) {
            log.error("Exception while reading holding registers: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Writes a single holding register to the Modulab device.
     * @param reference The register address to write to (0-based).
     * @param value The value to write.
     * @return true if successful, false otherwise.
     */
    public boolean writeSingleRegister(int reference, int value) {
        if (!isConnected || modbusMaster == null) {
            log.warn("Cannot write register: Device is not connected.");
            return false;
        }

        try {
            log.info("Writing value {} to register reference {}...", value, reference);
            // j2mod facade creates a simple register internally for this call
            modbusMaster.writeSingleRegister(unitId, reference, new com.ghgande.j2mod.modbus.procimg.SimpleRegister(value));
            log.info("Successfully wrote to register.");
            return true;
        } catch (Exception e) {
            log.error("Exception while writing single register: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
    
    public String getStatus() {
        return currentStatus;
    }

    /**
     * Main method to demonstrate and test the Modbus RTU communication independently.
     * Includes logic to automatically find Mac OS serial ports.
     */
    public static void main(String[] args) {
        log.info("--- Starting Modulab Modbus RTU Test ---");
        
        // 1. Automatically find available serial ports using jSerialComm
        SerialPort[] availablePorts = SerialPort.getCommPorts();
        log.info("Available serial ports:");
        
        String testPort = null;
        
        for (SerialPort port : availablePorts) {
            String portName = port.getSystemPortName();
            log.info(" - {} ({})", portName, port.getDescriptivePortName());
            
            // On Mac, USB serial adapters usually show up as "cu.usbserial-..." or "tty.usbserial-..."
            if (testPort == null && (portName.contains("cu.usbserial") || portName.contains("tty.usbserial"))) {
                testPort = "/dev/" + portName;
            }
        }

        if (availablePorts.length == 0) {
            log.error("No serial ports found! Please connect your RS485 USB adapter.");
            return;
        }

        // If no specific USB serial port was identified, default to the first one available
        if (testPort == null) {
            log.warn("Could not automatically identify a USB serial adapter. Defaulting to the first available port.");
            // On Mac, the full path to the device file is required by j2mod/RXTX under the hood
            testPort = "/dev/" + availablePorts[0].getSystemPortName();
        }
        
        log.info("Selected port for testing: {}", testPort);

        // Parameters
        int baudRate = 9600;
        int targetUnitId = 1;     // The Modbus slave ID of the Modulab device

        ModbusCommunication device = new ModbusCommunication(testPort, baudRate, targetUnitId);
        
        if (device.connect()) {
            
            // Example: Read 2 holding registers starting at address 0
            Register[] registers = device.readHoldingRegisters(0, 2);
            if (registers != null) {
                for (int i = 0; i < registers.length; i++) {
                    log.info("Register {}: Value = {}", i, registers[i].getValue());
                }
            }
            
            // Safely disconnect
            device.disconnect();
        } else {
            log.error("Failed to connect. Exiting test.");
        }
        
        log.info("--- Modulab Modbus RTU Test Complete ---");
    }
}