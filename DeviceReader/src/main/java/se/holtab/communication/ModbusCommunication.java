package se.holtab.communication;

import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles communication with Modulab devices using Modbus RTU over an RS485 serial connection.
 * It uses the j2mod library to handle the Modbus protocol.
 */
public class ModbusCommunication {

    private static final Logger log = LoggerFactory.getLogger(ModbusCommunication.class);

    private final String portName; // The serial port (e.g., "/dev/cu.usbserial-1410" or "COM3")
    private final int baudRate;    // Communication speed (e.g., 9600)
    private final int unitId;      // The specific Modbus slave ID of the hardware
    
    private ModbusSerialMaster modbusMaster; // The j2mod object that manages the connection
    private boolean isConnected;

    public ModbusCommunication(String portName, int baudRate, int unitId) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.unitId = unitId;
        this.isConnected = false;
    }

    /**
     * Opens the serial port and establishes the Modbus RTU connection.
     * @return true if successful, false if it fails to connect.
     */
    public boolean connect() {
        log.info("Connecting to Modbus RTU (Port: {}, Baud: {}, Unit ID: {})...", portName, baudRate, unitId);
        
        try {
            // 1. Set up standard Modbus RTU serial parameters (8 Data bits, No Parity, 1 Stop bit)
            SerialParameters params = new SerialParameters();
            params.setPortName(portName);
            params.setBaudRate(baudRate);
            params.setDatabits(8);
            params.setParity("None");
            params.setStopbits(1);
            params.setEncoding(Modbus.SERIAL_ENCODING_RTU); 
            params.setEcho(false);

            // 2. Create the Modbus master instance and set a timeout of 2 seconds
            modbusMaster = new ModbusSerialMaster(params);
            modbusMaster.setTimeout(2000);

            // 3. Open the connection
            modbusMaster.connect();
            
            this.isConnected = true;
            log.info("Modbus connection established.");
            return true;
            
        } catch (Exception e) {
            this.isConnected = false;
            log.error("Failed to connect to Modbus port: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Closes the Modbus connection and frees the serial port.
     */
    public void disconnect() {
        if (isConnected && modbusMaster != null) {
            try {
                modbusMaster.disconnect();
                this.isConnected = false;
                log.info("Modbus connection closed.");
            } catch (Exception e) {
                log.error("Error closing Modbus connection: {}", e.getMessage());
            }
        }
    }

    /**
     * Reads one or more "Holding Registers" (which typically contain configuration or sensor data).
     * @param startAddress The memory address to start reading from (0-based).
     * @param numberOfRegisters How many registers to read sequentially.
     * @return An array of registers, or null if the read fails.
     */
    public Register[] readRegisters(int startAddress, int numberOfRegisters) {
        if (!isConnected) {
            log.warn("Cannot read: Not connected.");
            return null;
        }

        try {
            // Tell j2mod to request data from the specific Unit ID at the given address
            return modbusMaster.readMultipleRegisters(unitId, startAddress, numberOfRegisters);
        } catch (Exception e) {
            log.error("Failed to read Modbus registers: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Writes a value to a single "Holding Register".
     * @param address The memory address to write to (0-based).
     * @param value The value to write.
     * @return true if successful, false otherwise.
     */
    public boolean writeRegister(int address, int value) {
        if (!isConnected) {
            log.warn("Cannot write: Not connected.");
            return false;
        }

        try {
            // j2mod requires the value to be wrapped in a SimpleRegister object
            modbusMaster.writeSingleRegister(unitId, address, new com.ghgande.j2mod.modbus.procimg.SimpleRegister(value));
            return true;
        } catch (Exception e) {
            log.error("Failed to write Modbus register: {}", e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getStatus() {
        return isConnected ? "Connected" : "Disconnected";
    }

    /**
     * A standalone test method to check if the code can find and connect to your USB RS485 adapter.
     */
    public static void main(String[] args) {
        log.info("--- Starting Modbus Hardware Test ---");
        
        String portToUse = findMacUsbSerialPort();
        
        if (portToUse == null) {
            log.error("Could not find a USB Serial Adapter on this Mac.");
            return;
        }

        // Try connecting to Unit ID 1 at 9600 baud
        ModbusCommunication device = new ModbusCommunication(portToUse, 9600, 1);

        if (device.connect()) {
            log.info("Successfully opened the port! Attempting to read register 0...");

            Register[] registers = device.readRegisters(0, 1);
            if (registers != null && registers.length > 0) {
                log.info("Value at Register 0: {}", registers[0].getValue());
            } else {
                log.warn("Opened port, but failed to read data. Is the Modulab connected to the RS485 wires correctly?");
            }

            device.disconnect();
        }
    }

    /**
     * Helper method to scan the Mac for connected USB-to-Serial adapters.
     */
    private static String findMacUsbSerialPort() {
        for (SerialPort port : SerialPort.getCommPorts()) {
            String name = port.getSystemPortName();
            // Look for typical Mac USB serial driver names
            if (name.contains("cu.usbserial") || name.contains("tty.usbserial")) {
                return "/dev/" + name;
            }
        }
        return null;
    }
}