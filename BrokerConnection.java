// Copyright 2022 Axini B.V. https://www.axini.com, see: LICENSE.txt.

import java.util.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// BrokerConnection: handles WebSocket connection to AMP.
public class BrokerConnection {
    private static Logger logger =
        LoggerFactory.getLogger(BrokerConnection.class);

    private URI serverUri;
    private String token;
    private WebSocketClient connection;
    private AdapterCore adapterCore;

    public BrokerConnection(URI serverUri, String token) {
        this.serverUri = serverUri;
        this.token = token;
    }

    public void close(int code, String message) {
        connection.close(code, message);
    }

    public void send(byte[] bytes) {
        connection.send(bytes);
    }

    public void connect() {
        // Use a separate connection for each test run.
        connection = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                logger.info("Connected to AMP: " + getURI());
                adapterCore.onOpen();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.info("Disconnected from AMP: " + getURI() + "; " +
                            "code: " + code + " " + reason);
                adapterCore.onClose(code, reason, remote);
            }

            @Override
            public void onMessage(String message) {
                logger.error("Text message received from AMP: " + message);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                adapterCore.handleMessage(bytes);
            }

            @Override
            public void onError(Exception ex) {
                logger.info("Exception occurred: " + ex);
                ex.printStackTrace();
            }

            @Override
            public void connect() {
                addHeader("Authorization", "Bearer " + token);
                super.connect();
            }
        };

        connection.connect();
    }

    public void registerAdapterCore(AdapterCore adapterCore) {
        this.adapterCore = adapterCore;
    }
}
