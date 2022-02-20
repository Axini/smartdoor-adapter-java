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

// BrokerConnection: WebSocket connection to AMP.
public class BrokerConnection extends WebSocketClient {
    private static Logger logger =
        LoggerFactory.getLogger(BrokerConnection.class);

    private String token;
    private AdapterCore adapterCore;

    public BrokerConnection(URI serverUri, String token) {
        super(serverUri);
        this.token = token;
    }

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
        logger.info("text message received from AMP: " + message);
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

    public void registerAdapterCore(AdapterCore adapterCore) {
        this.adapterCore = adapterCore;
    }
}
