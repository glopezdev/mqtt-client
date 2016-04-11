package com.amchealth.mqtt_client_api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.amchealth.callback.Event;
import com.amchealth.callback.EventEmitter;
import com.amchealth.test.Util;

public class SocketTest {

    private static final TimeUnit UNIT = TimeUnit.SECONDS;
    Long time = System.currentTimeMillis()*10000;
    private Socket socket;

    @Before
    public void setUpTestSocket() {
        socket = Util.getSocket();
    }

    @After
    public void tearDownTestSocket() {
        if (socket.isConnected()) {
            socket.socketClient.eventEmitters.clear();
            socket.disconnect();
        }
    }


    @Test
    public void shouldBeAbleToConnect() {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if (socket.isConnected()) {
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        try {
            signal.await(2, UNIT);
            assertTrue(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldBeAbleToReconnectAfterAuthFail() {
        final CountDownLatch signal = new CountDownLatch(1);
        Socket reconnectiongSocket = Util.getSocket(new AtomicInteger(5));
        EventEmitter<String> connectEmitter = reconnectiongSocket
                .getConnectEmitter();
        if (socket.isConnected()) {
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        try {
            signal.await(5, UNIT);
            assertTrue(reconnectiongSocket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } finally {
            reconnectiongSocket.disconnect();
        }
    }

    @Test
    public void shouldBeAbleToReconnect() {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if (socket.isConnected()) {
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        socket.disconnect();
        Socket socket2 = Util.getSocket();

        try {
            signal.await(2, UNIT);
            assertFalse(socket.isConnected());
            assertTrue(socket2.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldBeAbleToDisconnetAfterConnect() {
        final CountDownLatch signal = new CountDownLatch(2);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if(socket.isConnected()){
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });
        connectEmitter.on("socket::disconnected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        socket.disconnect();

        try {
            signal.await(2, UNIT);
            assertFalse(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void shouldBeAbleToPublishAMessage() {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        connectEmitter.on("defaultTopicResponses", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        socket.connect();
        socket.subscribe("defaultTopicResponses");
        socket.publish("defaultTopic", "test message");

        try {
            signal.await(2, UNIT);
            assertTrue(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void shouldBeAbleToStayConnectedFor15sec() {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if(socket.isConnected()){
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        for (int i = 0; i < 15; i++) {
            System.out.println("subscribing " + (time+i));
            try {
              socket.subscribe("test.obj/state/+/"+(time+i));
              socket.publish("test.obj/signal","{\"agentData\":{\"_id\":\""+(time+i)+"\"},\"data\":{}}");
            } catch (Exception e) {
              System.out.println("FAILED BUT THAT Is OK ********************************************************");
              e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            signal.await(2, UNIT);
            assertTrue(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void shouldBeAbleToStayConnectedWhileTransmitting() {
        socket.connect();
        for (int i = 0; i < 10; i++) {
            System.out.println("publishing");
            socket.publish("agent::event", "{}");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(socket.isConnected());
    }

    @Test
    public void shouldHandleMultipleConnections() throws InterruptedException {
        final CountDownLatch connectState = new CountDownLatch(2);
        final CountDownLatch disconnectState1 = new CountDownLatch(1);
        final CountDownLatch disconnectState2 = new CountDownLatch(1);
        MqttWrapper socketClient = socket.socketClient;
        Socket socket2 = Util.getSocket();
        EventEmitter<String> connectEmitter1 = socket.getConnectEmitter();
        EventEmitter<String> connectEmitter2 = socket2.getConnectEmitter();

        Event<String> cb = new Event<String>() {
            @Override
            public void onEmit(String... data) {
                connectState.countDown();
            }
        };
        Event<String> dcb1 = new Event<String>() {
            @Override
            public void onEmit(String... data) {
                disconnectState1.countDown();
            }
        };
        Event<String> dcb2 = new Event<String>() {
            @Override
            public void onEmit(String... data) {
                disconnectState2.countDown();
            }
        };
        if (socket.isConnected()) {
          cb.onEmit();
          cb.onEmit();
        }
        connectEmitter1.once("socket::connected", cb);
        connectEmitter2.once("socket::connected", cb);
        connectEmitter1.once("socket::disconnected", dcb1);
        connectEmitter2.once("socket::disconnected", dcb2);

        connectState.await(5, UNIT);
        assertEquals(0, connectState.getCount());
        assertEquals(2, socketClient.eventEmitters.size());

        socket.disconnect();

        disconnectState1.await(2, UNIT);
        assertEquals(0, disconnectState1.getCount());
        assertEquals(1, socketClient.eventEmitters.size());
        assertFalse(socket.isConnected());
        assertTrue(socket2.isConnected());

        socket2.disconnect();
        disconnectState2.await(2, UNIT);
        assertEquals(0, disconnectState2.getCount());
        assertEquals(0, socketClient.eventEmitters.size());
        assertFalse(socket2.isConnected());
        assertFalse(socket.isConnected());
    }
}
