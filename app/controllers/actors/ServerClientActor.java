package controllers.actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import play.Play;

@SuppressWarnings("deprecation")
public class ServerClientActor extends UntypedActor {

	public static Props props() {
		return Props.create(ServerClientActor.class);
	}

	private final ServerClient c = new ServerClient();
	private OOCSIServer server = Play.application().injector().instanceOf(OOCSIServer.class);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OOCSIHTTPRequest) {
			OOCSIHTTPRequest request = (OOCSIHTTPRequest) message;
			server.addClient(c);
			Channel c1 = server.getChannel(request.service);
			if (c1 != null) {
				Message m = new Message(c.getName(), request.service);
				m.addData("webcall", request.call);
				m.addData("webcall_data", request.data);
				// m.addData("_MESSAGE_HANDLE", request.call);
				// m.addData("_MESSAGE_ID", c.getName());
				c1.send(m);

				int i = 0;
				while (!c.completed() && i < 5) {
					i++;
					Thread.sleep(500);
				}
				if (c.completedMessage != null) {
					sender().tell(c.completedMessage, self());
				}
			}
		}
	}

	@Override
	public void postStop() throws Exception {
		server.removeClient(c);

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
