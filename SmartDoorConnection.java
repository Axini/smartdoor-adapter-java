// Copyright 2022 Axini B.V. https://www.axini.com, see: LICENSE.txt.

import java.util.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// SmartDoorConnection: WebSocket connection to the SmartDoor SUT.
public class SmartDoorConnection extends WebSocketClient {
    private static Logger logger =
        LoggerFactory.getLogger(SmartDoorConnection.class);

    private SmartDoorHandler handler;

    public SmartDoorConnection(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("Connected to SUT: " + getURI());
        handler.sendResetToSut();
        handler.sendReadyToAmp();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        String message = "Disconnected from SUT: " + getURI() + "; " +
                         "code: " + code + " " + reason;
        logger.info(message);
    }

    @Override
    public void onMessage(String message) {
        logger.info("received from SUT: " + message);
        if (handler != null)
            handler.sendResponseToAmp(message);
    }

    @Override
    public void onError(Exception ex) {
        String message = "Exception occurred: " + ex;
        logger.info(message);
        if (handler != null)
            handler.sendError(message);
        ex.printStackTrace();
    }

    public void registerHandler(SmartDoorHandler handler) {
        this.handler = handler;
    }
}
