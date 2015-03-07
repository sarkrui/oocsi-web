package controllers;

import java.io.IOException;

import model.WebSocketClient;
import nl.tue.id.oocsi.server.OOCSIServer;
import play.mvc.Controller;
import play.mvc.WebSocket;

public class Application extends Controller {

	private static OOCSIServer server = null;

	public static WebSocket<String> index() {
		WebSocketClient wsc = new WebSocketClient(getServer(), request().remoteAddress());
		return wsc.getHandler();
	}

	private static OOCSIServer getServer() {
		if (server == null) {
			try {
				server = new OOCSIServer(4444, 1000, false);
			} catch (IOException e) {
				// do nothing for now
				e.printStackTrace();
			}
		}

		return server;
	}
}
