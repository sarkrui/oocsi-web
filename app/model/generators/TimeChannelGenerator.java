package model.generators;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorSystem;
import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

public class TimeChannelGenerator {

	private static final String CHANNEL = "timechannel";

	final private OOCSIServer server;
	final private ActorSystem actorSystem;
	final private ExecutionContext executionContext;
	final private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

	public TimeChannelGenerator(OOCSIServer server, ActorSystem system, ExecutionContext ectx) {
		this.server = server;
		this.actorSystem = system;
		this.executionContext = ectx;

		this.initialize();
	}

	private void initialize() {
		this.actorSystem.scheduler().schedule(Duration.create(2, TimeUnit.SECONDS),
				Duration.create(1, TimeUnit.SECONDS), () -> publish(), this.executionContext);
	}

	private void publish() {
		Message m = new Message("SERVER", CHANNEL);

		m.addData("timestamp", System.currentTimeMillis());
		m.addData("datetime", sdf.format(new Date()));

		server.send(m);
	}
}
