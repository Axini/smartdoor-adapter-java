// Copyright 2022 Axini B.V. https://www.axini.com, see: LICENSE.txt.

import java.util.*;

import java.nio.ByteBuffer;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import PluginAdapter.Api.MessageOuterClass.*;
import PluginAdapter.Api.AnnouncementOuterClass.*;
import PluginAdapter.Api.ConfigurationOuterClass.*;
import PluginAdapter.Api.LabelOuterClass.*;
import PluginAdapter.Api.LabelOuterClass.Label.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// AdapterCore: keeps state of the adapter.
public class AdapterCore {
    private static Logger logger = LoggerFactory.getLogger(AdapterCore.class);

    enum State { DISCONNECTED, CONNECTED, ANNOUNCED, CONFIGURED, READY, ERROR }

    private String name;
    private BrokerConnection brokerConnection;
    private Handler handler;
    private State state;

    public AdapterCore(String name, BrokerConnection brokerConnection, Handler handler) {
        this.name = name;
        this.brokerConnection = brokerConnection;
        this.handler = handler;
        this.state = State.DISCONNECTED;
    }

    // Start the AdapterCore.
    // * let the BrokerConnection connect to AMP.
    public void start() {
        if (state == State.DISCONNECTED) {
            logger.info("Connecting to AMP's broker.");
            brokerConnection.connect();
        } else {
            String message = "Adapter started while already connected.";
            logger.info(message);
            sendError(message);
        }
    }

    // BrokerConnection: connection is opened.
    // * send announcement to AMP.
    public void onOpen() {
        if (state == State.DISCONNECTED) {
            state = State.CONNECTED;
            logger.info("Announcing.");

            List<Label> supportedLabels = handler.getSupportedLabels();
            Configuration configuration = handler.getConfiguration();
            sendAnnouncement(name, supportedLabels, configuration);
            state = State.ANNOUNCED;

        } else {
            String message = "Connection openend while already connected";
            logger.info(message);
            sendError(message);
        }
    }

    // BrokerConnection: connection is closed.
    // * stop the handler
    public void onClose(int code, String reason, boolean remote) {
        state = State.DISCONNECTED;

        String message = "Connection closed with code " + code +
                         " and reason: " + reason + ".";
        if (code == 1006)
            message = message + " The server may not be reachable.";
        logger.info(message);

        handler.stop();
        start(); // reconnect to AMP - keep the adapter alive
    }

    // Configuration received from AMP.
    // * configure the handler,
    // * start the handler,
    // * send ready to AMP (should be done by handler).
    public void onConfiguration(Configuration configuration) {
        if (state == State.ANNOUNCED) {
            handler.setConfiguration(configuration);
            state = State.CONFIGURED;

            logger.info("Connecting to the SUT.");
            handler.start();

            // The handler should call sendReady() as it knows when it is ready.

        } else if (state == State.CONNECTED) {
            String message = "Configuration received from AMP while not yet announced.";
            logger.info(message);
            sendError(message);

        } else {
            String message = "Configuration received from AMP while already configured.";
            logger.info(message);
            sendError(message);
        }
    }

    // Label (stimulus) received from AMP.
    // * make handler offer the stimulus to the SUT,
    // * acknowledge the actual stimulus to AMP.
    public void onLabel(Label label, long correlationId) {
        if (state == State.READY) {
            // We do not check that the label is a stimulus.
            ByteString physicalLabel = handler.stimulate(label);
            long timestamp = AxiniProtobuf.timestamp();
            sendStimulus(label, physicalLabel, timestamp, correlationId);

        } else {
            String message = "Label received from AMP while not ready.";
            logger.info(message);
            sendError(message);
        }
    }

    // Reset message received from AMP.
    // * reset the handler,
    // * send ready to AMP (should be done by handler).
    public void onReset() {
        if (state == State.READY) {
            handler.reset();
            // The handler should call sendReady() as it knows when it is ready.

        } else {
            String message = "Reset received from AMP while not ready.";
            logger.info(message);
            sendError(message);
        }
    }

    // Error message received from AMP.
    // * close the connection to AMP
    public void onError(String message) {
        state = State.ERROR;
        String msg = "Error message received from AMP: " + message + ".";
        logger.info(msg);

        brokerConnection.close(1000, message); // 1000 is normal closure...
    }

    // Parse the ByteBuffer message from AMP to a Protobuf message and call
    // the appropriate method of this AdapterCore.
    public void handleMessage(ByteBuffer bytes) {
        Message msg = null;

        try {
            msg = Message.parseFrom(bytes.array());
        } catch (InvalidProtocolBufferException ex) {
            logger.error("InvalidProtocolBufferException: " + ex.getMessage());
        }

        if (msg.hasConfiguration()) {
            logger.info("Configuration received from AMP");
            Configuration configuration = msg.getConfiguration();
            onConfiguration(configuration);
        }

        else if (msg.hasLabel()) {
            Label label = msg.getLabel();
            // two extra spaces to allign log with SmartDoorConnection
            logger.info("Label received from AMP: " + label.getLabel());
            long correlation_id = label.getCorrelationId();
            onLabel(label, correlation_id);
        }

        else if (msg.hasError()) {
            String error_msg = msg.getError().getMessage();
            logger.info("Error received from AMP: " + error_msg);
            onError(error_msg);
        }

        else if (msg.hasReset()) {
            logger.info("Reset received from AMP");
            onReset();
        }

        else if (msg.hasAnnouncement()) {
            logger.error("Message type 'Announcement' not supported");
        }

        else if (msg.hasReady()) {
            logger.error("Message type 'Ready' not supported");
        }

        else {
            logger.error("Unknown message type");
        }
    }

    // Send response to AMP (callback for Handler).
    // We do not check whether the label is actual a response.
    public void sendResponse(Label label, ByteString physicalLabel,
                             long timestamp) {
        logger.info("Sending response to AMP: " + label.getLabel());
        Label new_label =
            AxiniProtobuf.createLabel(label, physicalLabel, timestamp);
        sendLabel(new_label);
    }

    // Send ready message to AMP.
    public void sendReady() {
        logger.info("Sending Ready to AMP.");
        sendMessage(AxiniProtobuf.createMsgReady());
        state = State.READY;
    }

    // Send Error message to AMP (also callback for Handler).
    public void sendError(String errorMessage) {
        logger.info("Sending Error to AMP and closing the connection.");
        Message message = AxiniProtobuf.createMsgError(errorMessage);
        sendMessage(message);
        brokerConnection.close(1000, errorMessage); // 1000 is normal closure
    }

    // Send stimulus to AMP.
    // We do not check that the label is a stimulus.
    private void sendStimulus(Label label, ByteString physicalLabel,
                              long timestamp, long correlationId) {
        logger.info("Sending stimulus (back) to AMP: " + label.getLabel());
        Label new_label = AxiniProtobuf.createLabel(label, physicalLabel,
                                                    timestamp, correlationId);
        sendLabel(new_label);
    }

    private void sendLabel(Label label) {
        // logger.info("Sending label to AMP: " + label.getLabel());
        Message message = AxiniProtobuf.createMsgLabel(label);
        sendMessage(message);
    }

    // Send announcement to AMP.
    private void sendAnnouncement(String name, List<Label> supportedLabels,
                                  Configuration configuration) {
        Message message = AxiniProtobuf.createMsgAnnouncement(name,
                                            supportedLabels, configuration);
        sendMessage(message);
    }

    // Send a Protobuf Message as a byte[] to AMP.
    private void sendMessage(Message message) {
        byte[] bytes = message.toByteArray();
        brokerConnection.send(bytes);
    }
}
