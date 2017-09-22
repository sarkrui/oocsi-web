package controllers;

import static akka.pattern.Patterns.ask;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import controllers.actors.ServerClientActor;
import controllers.actors.WebSocketClientActor;
import javax.inject.Inject;
import javax.inject.Singleton;
import nl.tue.id.oocsi.server.protocol.Message;
import play.mvc.Controller;
import play.mvc.LegacyWebSocket;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.compat.java8.FutureConverters;

@SuppressWarnings("deprecation")
@Singleton
public class Application extends Controller {

	@Inject
	ActorSystem system;

	public LegacyWebSocket<String> index() {
		return ws();
	}

	public LegacyWebSocket<String> ws() {
		return WebSocket.withActor(WebSocketClientActor::props);
	}

	public CompletionStage<Result> serviceIndex(String service) {
		return CompletableFuture.completedFuture(ok(service));
	}

	public CompletionStage<Result> serviceCall(String service, String call, String data) {

		ActorRef a = system.actorOf(ServerClientActor.props());

		CompletionStage<Result> prom = FutureConverters
				.toJava(ask(a, new ServerClientActor.OOCSIHTTPRequest(service, call, data), 2000))
				.thenApply(response -> {
					if (response == null) {
						return ok();
					} else {
						Map<String, Object> messageData = ((Message) response).data;
						if (messageData.containsKey("html"))
							return ok((String) messageData.get("html")).as("text/html");
						else if (messageData.containsKey("text"))
							return ok((String) messageData.get("text")).as("text/plain");
						else if (messageData.containsKey("json"))
							return ok((String) messageData.get("json")).as("text/json");
						else if (messageData.containsKey("csv"))
							return ok((String) messageData.get("csv")).as("text/csv");
						else
							return ok();
					}
				}).exceptionally(e -> notFound("service not found"));
		return prom;

		// CompletionStage<Result> promiseOfInt = CompletableFuture.supplyAsync(() -> intensiveComputation());
		// return CompletableFuture.completedFuture(ok(service + call));
	}

	// private Result intensiveComputation() {
	// try {
	// Thread.sleep(2000);
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return ok();
	// }

	public Result dashboard() {
		return ok(views.html.Application.dashboard.render("title", "content", request().host() + "/ws"));
	}

}
