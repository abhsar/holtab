Feature: Modbus RS485 Device Communication
  As a system integrator
  I want to communicate with a Modulab device using Modbus RTU over RS485
  So that I can read sensor data and write configuration values

  Background:
    Given a Modbus RTU device is available on port "cu.usbserial-1410" with Unit ID 1
    And the RS485 connection parameters are configured to 9600 baud, 8 data bits, no parity, 1 stop bit

  Scenario: Successfully establish a Modbus RTU connection
    When I attempt to connect to the Modbus device
    Then the Modbus connection should be established successfully
    And the Modbus device status should show as "Connected"

  Scenario: Read holding registers from the device
    Given the Modbus connection is established
    When I request to read 2 holding registers starting at reference 0
    Then I should receive an array of 2 registers
    And the values should be valid integers

  Scenario: Write to a single holding register
    Given the Modbus connection is established
    When I request to write the value 42 to holding register at reference 10
    Then the write operation should be successful

  Scenario Outline: Read specific sensor data
    Given the Modbus connection is established
    When I request to read 1 holding register starting at reference <reference>
    Then I should receive an array of 1 registers
    And the register value should be <expectedValue>

    Examples:
      | reference | expectedValue |
      | 5         | 100           |
      | 15        | 25            |

  Scenario: Disconnect from the Modbus device
    Given the Modbus connection is established
    When I disconnect from the device
    Then the Modbus connection should be closed
    And the Modbus device status should show as "Disconnected"