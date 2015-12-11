package model;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import play.Logger;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class WebSocketClient extends Client {

	private static final Gson GSON = new Gson();

	OOCSIServer server;
	String address;
	WebSocket<String> websocket;
	play.mvc.WebSocket.Out<String> outputStream;

	public WebSocketClient(OOCSIServer server, String ipAddress) {
		super("");

		this.server = server;
		this.address = ipAddress;
	}

	public WebSocket<String> getHandler() {
		websocket = new WebSocket<String>() {
			public void onReady(play.mvc.WebSocket.In<String> in, final play.mvc.WebSocket.Out<String> out) {
				// For each event received on the socket,
				in.onMessage(new Callback<String>() {
					public void invoke(String event) {

						// on first message --> set the name
						if (token == null || token.length() == 0) {
							token = event;
							outputStream = out;

							// check for existing client
							if (server.addClient(WebSocketClient.this)) {
								Logger.info("WS client " + token + " connected");
								status(200, "Welcome " + token);
							} else {
								Logger.info("WS client " + token + " rejected as existing");
								status(401, "ERROR: client exists already");
								out.close();
							}

							return;
						}

						// anything useful?
						parseMessage(event);

						// // log events to the console
						// Logger.info(event);
					}
				});

				// When the socket is closed.
				in.onClose(new Callback0() {
					public void invoke() {
						disconnect();
						Logger.info("WS client " + token + " disconnected");
					}
				});
			}
		};

		return websocket;
	}

	@Override
	public void disconnect() {
		if (outputStream != null) {
			outputStream.close();
			outputStream = null;
		}
		server.removeClient(this);
	}

	@Override
	public boolean isConnected() {
		return outputStream != null;
	}

	@Override
	public void send(Message message) {
		if (outputStream != null) {
			try {
				outputStream.write(toJson(message));
			} catch (Exception e) {
				// problem writing: ignore
			}
		}
	}

	public void status(int code, String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("code", code);
		map.put("status", message);
		send(new Message("server", token, new Date(), map));
	}

	/**
	 * internal message parsing (will ignore all except for sendjson, subscribe, and unsubscribe)
	 * 
	 * @param inputLine
	 */
	private void parseMessage(String inputLine) {
		if (inputLine.startsWith("sendjson")) {
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String data = tokens[2];
				try {
					JsonNode node = Json.parse(data);
					if (node.isObject()) {
						Channel c = server.getChannel(recipient);
						if (c != null) {
							Map<String, Object> map = new HashMap<String, Object>();
							ObjectNode on = (ObjectNode) node;
							for (Iterator<Entry<String, JsonNode>> iterator = on.fields(); iterator.hasNext();) {
								Entry<String, JsonNode> entry = iterator.next();
								JsonNode val = entry.getValue();
								if (val.isBoolean()) {
									map.put(entry.getKey(), val.booleanValue());
								} else if (val.isInt()) {
									map.put(entry.getKey(), val.intValue());
								} else if (val.isFloat()) {
									map.put(entry.getKey(), val.floatValue());
								} else if (val.isDouble()) {
									map.put(entry.getKey(), val.doubleValue());
								} else if (val.isLong()) {
									map.put(entry.getKey(), val.longValue());
								} else if (val.isTextual()) {
									map.put(entry.getKey(), val.textValue());
								} else if (val.isObject()) {
									ObjectNode object = (ObjectNode) val;
									map.put(entry.getKey(), object.toString());
								}
							}
							c.send(new Message(token, recipient, new Date(), map));
						}
					}
				} catch (Exception e) {
					outputStream.write("ERROR: parse exception");
					Logger.warn("JSON parse exeption");
				}
			}
		} else if (inputLine.startsWith("subscribe")) {
			String[] tokens = inputLine.split(" ", 2);
			if (tokens.length == 2) {
				String channel = tokens[1];
				server.subscribe(this, channel);
			}
		} else if (inputLine.startsWith("unsubscribe")) {
			String[] tokens = inputLine.split(" ", 2);
			if (tokens.length == 2) {
				String channel = tokens[1];
				server.unsubscribe(this, channel);
			}
		} else {
			// ignore all other messages, do nothing
		}

		// update last action
		lastAction = System.currentTimeMillis();
	}

	private String toJson(Message m) {
		JsonObject jo = new JsonObject();
		jo.addProperty("sender", m.sender);
		jo.addProperty("recipient", m.recipient);
		jo.addProperty("timestamp", m.timestamp.getTime());
		jo.add("data", GSON.toJsonTree(m.data));

		return jo.toString();
	}

	@Override
	public void ping() {
		if (outputStream != null) {
			try {
				outputStream.write("ping");
			} catch (Exception e) {
				// problem writing: ignore
			}
		}
	}
}
