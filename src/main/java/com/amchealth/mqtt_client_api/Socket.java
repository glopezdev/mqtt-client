package com.amchealth.mqtt_client_api;

import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.amchealth.callback.EventEmitter;

public class Socket {
	static final Map<String, MqttWrapper> globalSockets = new Hashtable<String, MqttWrapper>();

	private EventEmitter<String> emitter = null;
	private String baseURL;
	MqttWrapper socketClient;

	public Socket(String baseURL,final AuthFunction authFunciton) {
		this(baseURL,authFunciton,null);
	}

	public Socket(String baseURL,final AuthFunction authFunciton, SSLContext sslContext) {
		this.baseURL = baseURL;
		this.socketClient = getMQTTClient();
		this.socketClient.setSSLContext(sslContext);
		this.socketClient.setAuthFunction(authFunciton);
		this.emitter = new EventEmitter<String>();
	}

	private synchronized MqttWrapper getMQTTClient() {
		MqttWrapper socket = globalSockets.get(baseURL);
		if (socket == null) {
			socket = new MqttWrapper(baseURL);
			globalSockets.put(baseURL, socket);
		}
		return socket;
	}

	public void connect() {
		socketClient.connect(emitter);
	}

	public boolean isConnected() {
		return socketClient != null && socketClient.isConnected();
	}

	public void publish(String topic,String string) {
		System.out.println("publishing "+topic+"->"+string);
		MqttMessage message = new MqttMessage(string.getBytes());
		socketClient.publish(topic, message);
	}

	public EventEmitter<String> getConnectEmitter() {
		return emitter;
	}

	public void disconnect() {
		socketClient.disconnect(emitter);
		socketClient = null;
	}

	public void subscribe(String topic) {
		socketClient.subscribe(topic);
	}

}
