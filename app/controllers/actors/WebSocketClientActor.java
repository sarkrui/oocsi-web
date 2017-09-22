package controllers.actors;

import com.google.inject.Inject;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import model.WebSocketClient;
import nl.tue.id.oocsi.server.OOCSIServer;
import play.Logger;
import play.Play;

@SuppressWarnings("deprecation")
public class WebSocketClientActor extends UntypedActor {

	public static Props props(ActorRef out) {
		return Props.create(WebSocketClientActor.class, out);
	}

	private final ActorRef out;

	@Inject
	OOCSIServer server = Play.application().injector().instanceOf(OOCSIServer.class);

	private WebSocketClient client;

	/**
	 * create a websocket client actor that relays messages between the websocket and the OOCSI server
	 * 
	 * @param out
	 */
	public WebSocketClientActor(ActorRef out) {
		this.out = out;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
	 */
	public void onReceive(Object message) throws Exception {
		if (message instanceof String) {
			String event = (String) message;

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
			client.message(event);

			// // log events to the console
			// Logger.info(event);
		}
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
	 * @see akka.actor.UntypedActor#postStop()
	 */
	public void postStop() throws Exception {
		if (client != null) {
			client.disconnect();
			Logger.info("WS client " + client.getName() + " disconnected");
		}
	}

}
