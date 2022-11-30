// Copyright 2022 Axini B.V. https://www.axini.com, see: LICENSE.txt.

import java.util.*;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.protobuf.ByteString;

import PluginAdapter.Api.LabelOuterClass.*;
import PluginAdapter.Api.LabelOuterClass.Label.*;
import PluginAdapter.Api.ConfigurationOuterClass.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// SmartDoorHandler: SUT specific implementation of Handler.
public class SmartDoorHandler extends Handler {
    private static Logger logger = LoggerFactory.getLogger(SmartDoorHandler.class);

    public static final String RESET           = "RESET";
    public static final String RESET_PERFORMED = "RESET_PERFORMED";

    // WebSocket connection to the SUT.
    private SmartDoorConnection connection;

    public SmartDoorHandler() {
        this.connection = null;
    }

    private static final String SMARTDOOR_URL          = "ws://localhost:3001";
    private static final String SMARTDOOR_MANUFACTURER = "Axini";

    // The default configuration for this adapter.
    public Configuration defaultConfiguration() {
        Configuration.Item url =
            AxiniProtobuf.createItem("url",
                "WebSocket URL of SmartDoor SUT", SMARTDOOR_URL);

        Configuration.Item manufacturer  =
            AxiniProtobuf.createItem("manufacturer",
                "SmartDoor manufacturer to test", SMARTDOOR_MANUFACTURER);

        List<Configuration.Item> items = new ArrayList<Configuration.Item>();
        items.add(url);
        items.add(manufacturer);

        return AxiniProtobuf.createConfiguration(items);
    }

    // Prepare to start testing.
    public void start() {
        if (connection == null) {
            try {
                Configuration config = getConfiguration();
                String url = AxiniProtobuf.getStringFromConfig(config, "url");
                logger.info("Trying to connect to SUT @ " + url);
                URI serverUri = new URI(url);
                connection = new SmartDoorConnection(serverUri);
                connection.registerHandler(this);
                connection.connect();
            }
            catch (URISyntaxException ex) {
                System.err.println("URISyntaxException exception: " +
                                   ex.getMessage());
                System.exit(1);
            }
        }
    }

    // Stop testing.
    public void stop() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    // Prepare for the next test case.
    public void reset() {
        // Try to reuse the WebSocket connection to the SUT.
        if (connection != null) {
            sendResetToSut();
            sendReadyToAmp();
        } else {
            stop();
            start();
        }
    }

    // Stimulate the System Under Test with the stimulus.
    // Return the physical label.
    public ByteString stimulate(Label stimulus) {
        logger.info("Sending stimulus to SUT: " + stimulus.getLabel());
        String sutMessage = labelToSutMessage(stimulus);
        connection.send(sutMessage);
        return ByteString.copyFromUtf8(sutMessage);
    }

    // Send a response to AMP.
    public void sendResponseToAmp(String response) {
        // ignore the response "RESET_PERFORMED"
        if (!response.equals(RESET_PERFORMED)) {
            Label label = sutMessageToLabel(response);
            long timestamp = AxiniProtobuf.timestamp();
            ByteString physicalLabel = ByteString.copyFromUtf8(response);
            adapterCore.sendResponse(label, physicalLabel, timestamp);
        }
    }

    // Send Ready to AMP.
    public void sendReadyToAmp() {
        adapterCore.sendReady();
    }

    // Send an error to AMP.
    public void sendErrorToAmp(String message) {
        adapterCore.sendError(message);
    }

    // Send reset to SUT.
    public void sendResetToSut() {
        Configuration config = getConfiguration();
        String manufacturer =
            AxiniProtobuf.getStringFromConfig(config, "manufacturer");
        String resetString = RESET + ":" + manufacturer;
        connection.send(resetString);
        logger.info("Sent '" + resetString + "' to SUT");
    }

    // ----- Supported labels.

    public static final String[] STIMULI          = { "open", "close"  };
    public static final String[] STIMULI_PASSCODE = { "lock", "unlock" };
    public static final String[] RESPONSES = {
        "opened", "closed", "locked", "unlocked",
        "invalid_command", "invalid_passcode", "incorrect_passcode",
        "shut_off"
    };

    // The labels supported by the adapter.
    public List<Label> getSupportedLabels() {
        String channel = "door";
        List<Label> labels = new ArrayList<Label>();

        for (String name: STIMULI)
            labels.add(stimulus(name));

        for (String name: STIMULI_PASSCODE)
            labels.add(stimulus(name, passcodeParameter()));

        for (String name: RESPONSES)
            labels.add(response(name));

        // extra stimulus to reset the SUT
        labels.add(stimulus("reset", manufacturerParameter()));

        return labels;
    }

    // Some helper methods to construct Protobuf objects for supported labels.

    private static Label stimulus(String name) {
        return AxiniProtobuf.createStimulus(name, "door");
    }

    private static Label stimulus(String name, List<Label.Parameter> params) {
        return AxiniProtobuf.createStimulus(name, "door", params);
    }

    private static Label response(String name) {
        return AxiniProtobuf.createResponse(name, "door");
    }

    private static List<Label.Parameter> passcodeParameter() {
        List<Label.Parameter> list = new ArrayList<Label.Parameter>();
        Label.Parameter parameter =
            AxiniProtobuf.createParameter("passcode",
                                          AxiniProtobuf.createIntValue(0));
        list.add(parameter);
        return list;
    }

    private static List<Label.Parameter> manufacturerParameter() {
        List<Label.Parameter> list = new ArrayList<Label.Parameter>();
        Label.Parameter parameter =
            AxiniProtobuf.createParameter("manufacturer",
                                          AxiniProtobuf.createStringValue(""));
        list.add(parameter);
        return list;
    }

    // ----- Converters

    // For the SmartDoor SUT the conversion between Protobuf Labels and
    // SUT messages is simple (upper <-> lower). Hence, these converters
    // can be part of the SmartDoorHandler. For practical SUTs, we typically
    // introduce special classes for theses converters.

    // Message to label converter.
    private static Label sutMessageToLabel(String message) {
        String response = message.toLowerCase();
        return AxiniProtobuf.createResponse(response, "door");
    }

    // Label to message converter.
    private static String labelToSutMessage(Label label) {
        String name = label.getLabel();
        List<Label.Parameter> params;
        String sut_label = name.toUpperCase();
        String result = null;
        switch (name) {
            case "open":
            case "close":
                result = sut_label;
                break;
            case "lock":
            case "unlock":
                params = label.getParametersList();
                long passcode = params.get(0).getValue().getInteger();
                result = sut_label + ":" + passcode;
                break;
            case "reset":
                params = label.getParametersList();
                String manufacturer = params.get(0).getValue().getString();
                result = sut_label + ":" + manufacturer;
                break;
            default:
                // This allows to send bad weather stimuli to the SUT.
                result = sut_label;
                break;
        }
        return result;
    }
}
