package com.amchealth.mqtt_client_api;

import io.inventit.dev.mqtt.paho.MqttWebSocketAsyncClient;

import java.util.LinkedHashSet;
import java.util.Set;

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
	final Set<EventEmitter<String>> eventEmitters = new LinkedHashSet<EventEmitter<String>>();

	MqttWebSocketAsyncClient socketClient;

	private SSLContext sslContext;

	private AuthFunction authFunciton;

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
				System.out.println("got t:"+topic+" m:"+msg);
				emit(topic, msg);
			}

			public void deliveryComplete(IMqttDeliveryToken arg0) {
				System.out.println("message delivered ");
			}

			public void connectionLost(Throwable arg0) {
				arg0.printStackTrace();
				System.out.println("On connection lost " + arg0.getMessage());
				connect(null);
				//emit("socket::disconnected");
			}
		});
	}

	public void connect(EventEmitter<String> emitter) {
		if (!isConnected()) {
			MqttConnectOptions options = new MqttConnectOptions();
			if (sslContext != null)
				options.setSocketFactory(sslContext.getSocketFactory());
			options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
		//	options.setConnectionTimeout(2);
		//	options.setKeepAliveInterval(2);

			doConnect(emitter, options);

		} else {
			addConnectedEmitter(emitter);
		}

	}

	private void addConnectedEmitter(EventEmitter<String> emitter) {
		if ( emitter != null ) {
			eventEmitters.add(emitter);
			emitter.emit("socket::connected");
			System.out.println("add connected emitted");
		}
	}

	private void doConnect(EventEmitter<String> emitter,
			MqttConnectOptions options) {
		try {
			System.out.println("doConnect");
			IMqttToken token = socketClient.connect(options);
			token.waitForCompletion();
			addConnectedEmitter(emitter);
		} catch (MqttSecurityException e) {
			if (e.getReasonCode() != 5)
				throw new RuntimeException(e);
			reconnect(emitter, options);
		} catch (MqttException e) {
			if(e.getReasonCode()!=32100)//already connected
				//reason 6 connection refuse
				//TODO dont crash the app when server unrecheable
				throw new RuntimeException(e);
		}
	}

	private void reconnect(final EventEmitter<String> emitter,
			final MqttConnectOptions options) {
		System.out.println("reconnecting");
		authFunciton.auth(new Callback<String, String>() {
			@Override
			public void call(String error, String... args) {
				if (error != null) {
					System.err.println(error);
					//TODO backoff logic
					reconnect(emitter, options);
					return;
				}
				final String token = args[0];
				final String sessionId = args[1];
				// final String url = urlPrefix + "/ws/" + sessionId +
				// "/auth/" + token;

				options.setUserName("Bearer");
				options.setPassword(token.toCharArray());
				System.out.println("going to connect with token:" + token
						+ " sessionId:" + sessionId);
				doConnect(emitter, options);
			}
		});
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
			} catch (MqttException e) {
				if(e.getReasonCode()!=32102)//already disconnected
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
