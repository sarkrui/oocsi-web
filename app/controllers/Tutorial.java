package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import akka.actor.ActorSystem;
import model.clients.EchoClient;
import model.generators.TestChannelGenerator;
import model.generators.TimeChannelGenerator;
import nl.tue.id.oocsi.server.OOCSIServer;
import play.Environment;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.ExecutionContext;

@Singleton
public class Tutorial extends Controller {

	private Environment environment;

	@Inject
	public Tutorial(Environment environment, OOCSIServer server, ActorSystem system, ExecutionContext ectx) {
		this.environment = environment;

		// start client
		new EchoClient("echo", server);

		// start generators
		new TestChannelGenerator(server, system, ectx);
		new TimeChannelGenerator(server, system, ectx);
	}

	public Result index() {
		return ok(views.html.Tutorial.index.render(environment.isProd()));
	}

	public Result paint_the_canvas() {
		return ok(views.html.Tutorial.paint_the_canvas.render(environment.isProd()));
	}
}
