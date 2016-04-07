package com.amchealth.test;

import java.util.concurrent.atomic.AtomicInteger;

import com.amchealth.callback.Callback;
import com.amchealth.mqtt_client_api.AuthFunction;
import com.amchealth.mqtt_client_api.Socket;

public class Util {

	public static final String URL = "ws://localhost:3000/mqtt";
	public static final String HTTP = "http://localhost:3000";

	public static Socket getSocket() {
		return getSocket(new AtomicInteger(0));
	}

	public static Socket getSocket(final AtomicInteger failures) {
		Socket s = new Socket(Util.URL, Util.getAuth(failures));
		return s;
	}

	public static AuthFunction getAuth(final AtomicInteger failures) {
		return new AuthFunction() {

			@Override
			public void auth(Callback<String, String> cb) {
					cb.call(null, "token", "whocares");
			}
		};
	}
}
