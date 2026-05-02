package stepDefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.holtab.communication.ModbusCommunication;
import org.junit.Assert;

public class ModbusRs485StepDef {

    private static final Logger log = LoggerFactory.getLogger(ModbusRs485StepDef.class);

    private String targetPort;
    private int targetUnitId;
    private ModbusCommunication deviceCommunication;

    // Use {word} or .* or generic matchers to avoid Cucumber interpreting the port as double/int mix
    @Given("a device is available at IP address {}") // Keeping the sentence same as feature file
    public void aDeviceIsAvailableAtIPAddress(String portOrIp) {
        // For Mac OS, ports are usually prefixed with /dev/
        // If the feature file just passes "cu.usbserial-1410", we ensure it has the /dev/ prefix
        this.targetPort = portOrIp.startsWith("/dev/") ? portOrIp : "/dev/" + portOrIp;
        
        // Define a default Modbus Slave Unit ID for the tests (e.g., 1)
        this.targetUnitId = 1;
        
        // Initialize the communication class with 9600 baud rate and the Unit ID
        this.deviceCommunication = new ModbusCommunication(this.targetPort, 9600, this.targetUnitId);
        log.info("Device targeted at port: {} with Modbus Unit ID: {}", targetPort, targetUnitId);
    }

    @Given("I have valid connection credentials")
    public void iHaveValidConnectionCredentials() {
        // Modbus RTU doesn't use username/password, but rather configuration 
        // parameters like baud rate, parity, stop bits (which are set in connect()).
        log.info("Modbus RTU connection parameters configured.");
    }

    @When("I attempt to connect to the device")
    public void iAttemptToConnectToTheDevice() {
        if (this.deviceCommunication != null) {
            log.info("Attempting connection to {}...", targetPort);
            this.deviceCommunication.connect();
        }
    }

    @Then("the connection should be established successfully")
    public void theConnectionShouldBeEstablishedSuccessfully() {
        Assert.assertNotNull("Device communication object should not be null", this.deviceCommunication);
        Assert.assertTrue("The connection was not established successfully.", this.deviceCommunication.isConnected());
    }

    @Then("the device status should show as {string}")
    public void theDeviceStatusShouldShowAs(String expectedStatus) {
        Assert.assertNotNull("Device communication object should not be null", this.deviceCommunication);
        Assert.assertEquals("The device status did not match.", expectedStatus, this.deviceCommunication.getStatus());
    }
}