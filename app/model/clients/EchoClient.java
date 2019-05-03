package model.clients;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;

public class EchoClient extends Client {

	final private OOCSIServer server;

	public EchoClient(String token, OOCSIServer server) {
		super(token, server.getChangeListener());

		this.server = server;

		server.addClient(this);
	}

	@Override
	public void disconnect() {
		server.removeClient(this);
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public void ping() {
		// do nothing
	}

	@Override
	public void send(Message event) {
		if (validate(event.recipient)) {
			Message m = new Message("SERVER", event.sender);
			m.data.putAll(event.data);
			server.send(m);
		}
	}

}
