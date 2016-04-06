package com.amchealth.mqtt_client_api;

import io.inventit.dev.mqtt.paho.MqttWebSocketAsyncClient;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.amchealth.callback.Callback;
import com.amchealth.callback.EventEmitter;

public class MqttWrapper {
    private static final int MAX_BACKOFF = 100;

    private static final int MIN_BACKOFF = 10000;

    final Set<EventEmitter<String>> eventEmitters = new LinkedHashSet<EventEmitter<String>>();

    MqttWebSocketAsyncClient socketClient;

    private SSLContext sslContext;

    private AuthFunction authFunciton;

    private boolean isConnecting = false;

    private int backoff = MIN_BACKOFF;

    final Timer timer = new Timer(true);

    MqttWrapper(String baseURL) {
        try {
            socketClient = new MqttWebSocketAsyncClient(baseURL,
                    MqttWebSocketAsyncClient.generateClientId(),
                    new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        socketClient.setCallback(new MqttCallback() {
            public void messageArrived(String topic, MqttMessage message)
                    throws Exception {
                String msg = new String(message.getPayload());
                System.out.println("got t:" + topic + " m:" + msg);
                emit(topic, msg);
            }

            public void deliveryComplete(IMqttDeliveryToken arg0) {
                System.out.println("message delivered ");
            }

            public void connectionLost(Throwable arg0) {
                arg0.printStackTrace();
                System.out.println("On connection lost " + arg0.getMessage());
                isConnecting = false;
                connect(null);
            }
        });
    }

    public void connect(EventEmitter<String> emitter) {
        eventEmitters.add(emitter);
        if (!isConnecting) {
            backoff = MIN_BACKOFF;
            doConnect(null);
        } else if (isConnected()) {
            emitter.emit("socket::connected");
        } else {
            System.err.println("UNDEFINED CONNECT STATE");
        }
    }

    private void doConnect(MqttConnectOptions options) {
        isConnecting = true;

        if (options == null)
            options = buildOptions();

        try {
            IMqttToken token = socketClient.connect(options);
            token.waitForCompletion();
            emit("socket::connected");
        } catch (MqttSecurityException e) {
            if (e.getReasonCode() != 5)
                throw new RuntimeException(e);
            reconnect(options);
        } catch (MqttException e) {
            if (e.getReasonCode() == 6)// connection refuse
                reconnect(options);
            else if (e.getReasonCode() != 32100)// already connected
                throw new RuntimeException(e);
        }
    }

    private MqttConnectOptions buildOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        if (sslContext != null)
            options.setSocketFactory(sslContext.getSocketFactory());
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        return options;
    }

    private void reconnect(final MqttConnectOptions options) {
        System.out.println("reconnecting");
        authFunciton.auth(new Callback<String, String>() {
            @Override
            public void call(String error, String... args) {
                if (error != null) {
                    System.err.println(error);
                    retryReconnect();
                    return;
                }
                final String token = args[0];
                final String sessionId = args[1];

                options.setUserName("Bearer");
                options.setPassword(token.toCharArray());
                System.out.println("going to connect with token:" + token
                        + " sessionId:" + sessionId);
                doConnect(options);
            }

        });
    }

    private void retryReconnect() {
        backoff = Math.min(MAX_BACKOFF, backoff * 2);
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                doConnect(null);
            }
        };
        timer.schedule(t, backoff);
    }

    public void subscribe(String topic) {
        try {
            socketClient.subscribe(topic, 0);
        } catch (MqttException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isConnected() {
        return socketClient.isConnected();
    }

    public void publish(String topic, MqttMessage message) {
        if (!isConnected()) {
            connect(null);
        }
        try {
            socketClient.publish(topic, message);
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (MqttException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void disconnect(EventEmitter<String> eventEmitter) {
        eventEmitters.remove(eventEmitter);
        if (eventEmitters.size() == 0) {
            try {
                IMqttToken token = socketClient.disconnect();
                token.waitForCompletion();
                backoff = MIN_BACKOFF;
                isConnecting = false;
            } catch (MqttException e) {
                if (e.getReasonCode() != 32102)// already disconnected
                    e.printStackTrace();
            }
        }
        eventEmitter.emit("socket::disconnected");
        eventEmitter.removeAllListeners();

    }

    private void emit(String message, String... data) {
        for (EventEmitter<String> e : eventEmitters) {
            e.emit(message, data);
        }
    }

    public void setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public void setAuthFunction(AuthFunction authFunciton) {
        this.authFunciton = authFunciton;
    }

}
