Feature: Device Connection
  As a system user
  I want to establish a connection to a hardware device
  So that I can read data from it or send commands

  Scenario Outline: Successfully connect to a device with valid credentials
    Given a device is available at IP address <ipAddress>
    And I have valid connection credentials
    When I attempt to connect to the device
    Then the connection should be established successfully
    And the device status should show as "Connected"

    Examples:
      |ipAddress|
      |192.168.1.100|


