package stepDefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import se.holtab.communication.ModulabCommunication;
import org.junit.Assert;

@Slf4j
public class ModulabRs485StepDef {

    private String targetPort;
    private ModulabCommunication deviceCommunication;

    // Use {word} or .* or generic matchers to avoid Cucumber interpreting the port as double/int mix
    @Given("a device is available at IP address {}") // Keeping the sentence same as feature file
    public void aDeviceIsAvailableAtIPAddress(String portOrIp) {
        // In the context of RS485, this is typically a COM port (e.g., COM3) or TTY (e.g., /dev/ttyUSB0)
        // rather than an IP address. We will map the feature file parameter to a port string.
        this.targetPort = portOrIp;
        
        // Initialize the communication class with 9600 baud rate (common for RS485)
        this.deviceCommunication = new ModulabCommunication(this.targetPort, 9600);
        log.info("Device targeted at port: {}", targetPort);
    }

    @Given("I have valid connection credentials")
    public void iHaveValidConnectionCredentials() {
        // RS485 typically doesn't use username/password, but rather configuration 
        // parameters like baud rate, parity, stop bits (which are set in connect()).
        log.info("RS485 connection parameters configured.");
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