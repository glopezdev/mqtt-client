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
		socketClient = globalSockets.get(baseURL);
		emitter = new EventEmitter<String>();
		if (socketClient == null) {
		  socketClient = new MqttWrapper(baseURL,sslContext,authFunciton,emitter);
		  globalSockets.put(baseURL, socketClient);
		}
		socketClient.connect(emitter);
	}

	@Deprecated
	public void connect() {
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

	synchronized
	public void disconnect() {
		if (socketClient!=null){
			if(socketClient.disconnect(emitter)){
			  globalSockets.remove(baseURL);
			};
			socketClient = null;
		}
	}

	public void subscribe(String topic) {
		socketClient.subscribe(topic);
	}

}
