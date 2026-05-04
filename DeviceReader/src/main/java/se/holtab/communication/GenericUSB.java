package se.holtab.communication;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GenericUSB {
    private static final Logger log = LoggerFactory.getLogger(GenericUSB.class);

    public static void main(String[] args) {
        log.info("--- Scanning for attached USB/Serial Communication Ports ---");
        scanSerialPorts();

        log.info("\n--- Scanning for Physical USB Storage Devices on Ports (Mac OS) ---");
        scanPhysicalStoragePorts();
    }

    /**
     * Scans for USB-to-Serial adapters (like RS485 interfaces).
     * Note: Standard USB Flash Drives do NOT show up here because they are not serial communication devices.
     */
    public static void scanSerialPorts() {
        SerialPort[] availablePorts = SerialPort.getCommPorts();
        
        if (availablePorts.length == 0) {
            log.warn("No serial ports or USB-to-Serial adapters found.");
            return;
        }
        
        for (int i = 0; i < availablePorts.length; i++) {
            SerialPort port = availablePorts[i];
            log.info("Serial Device {}:", i + 1);
            String path = port.getSystemPortName().startsWith("/") ? port.getSystemPortName() : "/dev/" + port.getSystemPortName();
            log.info("  System Path  : {}", path);
            log.info("  Description  : {}", port.getDescriptivePortName());
            log.info("  Port Desc    : {}", port.getPortDescription());
            log.info("--------------------------------------------------");
        }
    }

    /**
     * Uses macOS native commands to find physical external storage devices
     * plugged into the physical USB/Thunderbolt ports.
     */
    public static void scanPhysicalStoragePorts() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) {
            log.warn("Physical port scanning in this example is optimized for macOS.");
            return;
        }

        try {
            // "diskutil list external physical" asks the Mac OS for any actual 
            // hardware storage devices plugged into external ports.
            ProcessBuilder pb = new ProcessBuilder("diskutil", "list", "external", "physical");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            boolean foundDrives = false;
            
            while ((line = reader.readLine()) != null) {
                // Ignore empty lines from the terminal output
                if (line.trim().isEmpty()) continue;
                
                // Print the raw output from diskutil which perfectly formats the physical drives
                log.info("  {}", line);
                foundDrives = true;
            }

            if (!foundDrives) {
                log.warn("  No external USB storage drives detected on any physical port.");
            }
            
            process.waitFor();
        } catch (Exception e) {
            log.error("Failed to query physical ports for storage devices: {}", e.getMessage());
        }
    }
}