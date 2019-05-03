package model.clients;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;

public class ServiceRequestClient extends Client {

	public Message completedMessage = null;

	public ServiceRequestClient(OOCSIServer server) {
		super("serverclient" + Math.random(), server.getChangeListener());
	}

	@Override
	public void send(Message message) {
		if (validate(message.recipient)) {
			completedMessage = message;
		}
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