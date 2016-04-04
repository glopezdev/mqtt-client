package com.amchealth.conversation;

import org.json.JSONException;
import org.json.JSONObject;

import com.amchealth.callback.Callback;
import com.amchealth.callback.Event;
import com.amchealth.callback.EventEmitter;
import com.amchealth.mqtt_client_api.Socket;

public class Agent {
	private String agent;
	private Socket socket;
	private EventEmitter<String> eventEmitter;
	private JSONObject agentData;
	private String _id;

	static void jsonPut(JSONObject json, String key, String value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	static void jsonPut(JSONObject json, String key, JSONObject value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	static Object jsonGet(JSONObject json, String key) {
		try {
			return json.get(key);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	static JSONObject wrapData(String data) {
		try {
			return new JSONObject(data);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	static JSONObject wrapId(String _id) {
		JSONObject wrapped = new JSONObject();
		jsonPut(wrapped, "_id", _id);
		return wrapped;
	}

	public Agent(String agent, Socket socket, String _id) {
		this(agent, socket, wrapId(_id));
		this._id=_id;
	}

	private Agent(String agent, Socket socket, JSONObject agentData) {
		this.agent = agent;
		this.socket = socket;
		this.eventEmitter = socket.getConnectEmitter();
		this.agentData = agentData;
	}

	public void init(Callback<String, String> cb) {
		socket.subscribe(agent+"/state:*/"+this._id);
	}

	public void send(String event, JSONObject eventData,
			Callback<String, String> cb) {
		emit(event, eventData, cb);
	}

	public void emit(String event, JSONObject eventData,
			Callback<String, String> cb) {
		String fullEvent = agent + "/" + event;// single /

		JSONObject data = new JSONObject();
		jsonPut(data, "agentData", agentData);
		if (eventData != null) {
			jsonPut(data, "data", eventData);
		}
		socket.publish(fullEvent, data.toString());
	}

	public Callback<String, String> on(String event, final Event<JSONObject> cb) {
		return eventEmitter.on(
				agent + "/state/" + event + "/" + jsonGet(agentData, "_id"),
				eventCB(cb));
	}

	public Callback<String, String> once(String event,
			final Event<JSONObject> cb) {
		return eventEmitter.once(
				agent + "/state/" + event + "/" + jsonGet(agentData, "_id"),
				eventCB(cb));
	}

	private Event<String> eventCB(final Event<JSONObject> cb) {
		return new Event<String>() {

			@Override
			public void onEmit(String... data) {
				if (data.length > 0 && data[0] != null
						&& !"null".equals(data[0])) {
					JSONObject jsonObject = wrapData(data[0]);
					cb.onEmit(jsonObject);
				} else {
					cb.onEmit();
				}
			}

		};
	}

	public void disconnect() {
		eventEmitter.removeAllListeners();
	}

}
