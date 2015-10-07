package controllers;

import java.io.IOException;

import model.WebSocketClient;
import nl.tue.id.oocsi.server.OOCSIServer;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

public class Application extends Controller {

	private static OOCSIServer server = null;

	public WebSocket<String> index() {
		WebSocketClient wsc = new WebSocketClient(getServer(), request().remoteAddress());
		return wsc.getHandler();

		// return WebSocket.whenReady((in, out) -> {
		// // For each event received on the socket,
		// in.onMessage(System.out::println);
		//
		// // When the socket is closed.
		// in.onClose(() -> System.out.println("Disconnected"));
		//
		// // Send a single 'Hello!' message
		// out.write("Hello!");
		// });
	}

	public Result dashboard() {
		return ok(views.html.Application.dashboard.render("title", "content"));
	}

	private static OOCSIServer getServer() {
		if (server == null) {
			try {
				server = new OOCSIServer(4444, 1000, false) {
					@Override
					protected void internalLog(String message) {
						Logger.info(message);
					}
				};
			} catch (IOException e) {
				// do nothing for now
				e.printStackTrace();
			}
		}

		return server;
	}
}
