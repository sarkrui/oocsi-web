package controllers.actors;

import java.util.Map;

import akka.actor.Props;
import akka.actor.UntypedActor;
import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.protocol.Protocol;
import play.Play;

@SuppressWarnings("deprecation")
public class ServerClientActor extends UntypedActor {

	private static final String MESSAGE_HANDLE = "_MESSAGE_HANDLE";
	private static final String WEBCALL_DATA = "webcall_data";
	private static final String WEBCALL_ACTION = "webcall";

	public static Props props() {
		return Props.create(ServerClientActor.class);
	}

	private final ServerClient requestClient = new ServerClient();
	private OOCSIServer server = Play.application().injector().instanceOf(OOCSIServer.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OOCSIHTTPRequest) {
			OOCSIHTTPRequest request = (OOCSIHTTPRequest) message;
			server.addClient(requestClient);
			Channel serviceClient = server.getChannel(request.service);
			if (serviceClient != null) {
				Message serviceMessage = new Message(requestClient.getName(), request.service);

				// try to parse the webcall_data
				if (request.data != null && request.data.length() > 0) {
					Map<String, Object> map = Protocol.parseJSONMessage(request.data);
					// if data could be parsed, use it directly
					if (map.size() > 0) {
						serviceMessage.data = map;
					}
					// include data verbatim if cannot be parsed as JSON
					else {
						serviceMessage.addData(WEBCALL_DATA, map);
					}
					serviceMessage.addData(WEBCALL_ACTION, request.call);
				}

				// add message handle
				serviceMessage.addData(MESSAGE_HANDLE, request.service);

				// send out to responder
				serviceClient.send(serviceMessage);

				int i = 0;
				while (!requestClient.completed() && i < 5) {
					i++;
					Thread.sleep(500);
				}
				if (requestClient.completedMessage != null) {
					sender().tell(requestClient.completedMessage, self());
				}
			}
		}
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
			super("serverclient" + Math.random());
		}

		@Override
		public void send(Message message) {
			completedMessage = message;
		}

		@Override
		public void ping() {
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public void disconnect() {
		}

		public boolean completed() {
			return completedMessage != null;
		}
	}
}
