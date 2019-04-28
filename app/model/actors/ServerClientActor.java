package model.actors;

import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;

import akka.actor.AbstractActor;
import akka.actor.Props;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.protocol.Protocol;

public class ServerClientActor extends AbstractActor {

	private static final String WEBCALL_DATA = "webcall_data";
	private static final String WEBCALL_ACTION = "webcall";

	public static Props props(OOCSIServer server) {
		return Props.create(ServerClientActor.class, server);
	}

	private final ServerClient requestClient;
	private final OOCSIServer server;

	@Inject
	public ServerClientActor(OOCSIServer server) {
		this.server = server;
		this.requestClient = new ServerClient();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(OOCSIHTTPRequest.class, request -> {
			server.addClient(requestClient);
			Channel serviceClient = server.getChannel(request.service);

			if (serviceClient != null) {

				Message serviceMessage = new Message(requestClient.getName(), request.service);

				// add webcall action
				serviceMessage.addData(WEBCALL_ACTION, request.call);

				// add message handle
				serviceMessage.addData(OOCSICall.MESSAGE_HANDLE, request.service);
				String uuid = UUID.randomUUID().toString();
				serviceMessage.addData(OOCSICall.MESSAGE_ID, uuid);

				// try to parse the webcall_data
				if (request.data != null && request.data.length() > 0) {
					Map<String, Object> map = Protocol.parseJSONMessage(request.data);
					// if data could be parsed, use it directly
					if (map.size() > 0) {
						serviceMessage.data.putAll(map);
					}
					// include data verbatim if cannot be parsed as JSON
					else {
						serviceMessage.addData(WEBCALL_DATA, request.data);
					}
				}

				// send out to responder
				serviceClient.send(serviceMessage);

				int i = 0;
				while (!requestClient.completed() && i < 9) {
					i++;
					Thread.sleep(200);
				}

				if (requestClient.completedMessage != null) {
					sender().tell(requestClient.completedMessage, self());
				}
			}
		}).build();
	}

	@Override
	public void postStop() throws Exception {
		server.removeClient(requestClient);

		super.postStop();
	}

	public static class OOCSIHTTPRequest {

		String service;
		String call;
		String data;

		public OOCSIHTTPRequest(String service, String call, String data) {
			this.service = service;
			this.call = call;
			this.data = data;
		}
	}

	class ServerClient extends Client {

		Message completedMessage = null;

		public ServerClient() {
			super("serverclient" + Math.random(), server.getChangeListener());
		}

		@Override
		public void send(Message message) {
			completedMessage = message;
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public void disconnect() {
		}

		@Override
		public void ping() {
		}

		public boolean completed() {
			return completedMessage != null;
		}
	}
}
