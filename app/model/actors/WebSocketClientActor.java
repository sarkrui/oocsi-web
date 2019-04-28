package model.actors;

import com.google.inject.Inject;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import model.WebSocketClient;
import nl.tue.id.oocsi.server.OOCSIServer;
import play.Logger;

public class WebSocketClientActor extends AbstractActor {

	public static Props props(ActorRef out, OOCSIServer server) {
		return Props.create(WebSocketClientActor.class, out, server);
	}

	private final ActorRef out;
	private final OOCSIServer server;
	private WebSocketClient client;

	/**
	 * create a websocket client actor that relays messages between the websocket and the OOCSI server
	 * 
	 * @param out
	 */
	@Inject
	public WebSocketClientActor(ActorRef out, OOCSIServer server) {
		this.out = out;
		this.server = server;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see akka.actor.AbstractActor#createReceive()
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(String.class, event -> {
			if (client == null) {
				client = new WebSocketClient(event, server, this);
				if (server.addClient(client)) {
					Logger.info("WS client " + client.getName() + " connected");
					// status(200, );
					out.tell("{'message' : \"welcome " + client.getName() + "\"}", self());
				} else {
					Logger.info("WS client " + client.getName() + " rejected as existing");
					// status(401, );
					out.tell("{'message' : \"ERROR: client " + client.getName() + " exists already\"}", self());

					// kill self
					kill();
				}
			}

			// anything useful?
			client.receive(event);
		}).build();
	}

	/**
	 * terminate this websocket client (and connection)
	 * 
	 */
	public void kill() {
		self().tell(PoisonPill.getInstance(), self());
	}

	/**
	 * forward message to actor and connected client
	 * 
	 * @param message
	 */
	public void tell(String message) {
		out.tell(message, self());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see akka.actor.AbstractActor#postStop()
	 */
	public void postStop() throws Exception {
		if (client != null) {
			client.disconnect();
			Logger.info("WS client " + client.getName() + " disconnected");
		}
	}
}
