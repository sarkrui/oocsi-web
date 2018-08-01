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
import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;
import play.Play;
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

	public Result index() {
		OOCSIServer server = Play.application().injector().instanceOf(OOCSIServer.class);
		String channels = server.getChannelList().replace("OOCSI_connections,", "").replace("OOCSI_clients,", "")
				.replace("OOCSI_events,", "").replace("OOCSI_metrics,", "").replace("OSC,", "");
		String clients = server.getClientList();

		return ok(views.html.Application.index.render("index", "", request().host(), clients, channels));
	}

	public Result dashboard() {
		return ok(views.html.Application.dashboard.render("dashboard", "", request().host()));
	}

	public Result test() {
		return ok(views.html.Application.test.render("testing", "", request().host()));
	}

	public Result metrics() {
		return ok(views.html.Application.metrics.render("metrics", "", request().host()));
	}

	public LegacyWebSocket<String> ws() {
		return WebSocket.withActor(WebSocketClientActor::props);
	}

	public CompletionStage<Result> serviceIndex(String service) {
		if (Play.application().injector().instanceOf(OOCSIServer.class).getChannel(service) != null) {
			return CompletableFuture.completedFuture(ok(service + " ok"));
		} else {
			return CompletableFuture.completedFuture(notFound(service + " not found"));
		}
	}

	public CompletionStage<Result> serviceCall(String service, String call, String data) {
		return internalServiceCall(service, call, data);
	}

	public CompletionStage<Result> serviceCallPost(String service, String call) {
		return internalServiceCall(service, call, request().body().asText());
	}

	/**
	 * internal dispatch for the incoming calls
	 * 
	 * @param service
	 * @param call
	 * @param data
	 * @return
	 */
	private CompletionStage<Result> internalServiceCall(String service, String call, String data) {
		ActorRef a = system.actorOf(ServerClientActor.props());
		CompletionStage<Result> prom = FutureConverters
				.toJava(ask(a, new ServerClientActor.OOCSIHTTPRequest(service, call, data), 2000))
				.thenApply(response -> {
					if (response == null) {
						return ok();
					} else {
						Map<String, Object> messageData = ((Message) response).data;
						if (messageData.containsKey("html"))
							return ok((String) messageData.get("html")).as("text/html")
									.withHeader("Access-Control-Allow-Origin", "*")
									.withHeader("Access-Control-Allow-Headers", "X-Requested-With")
									.withHeader("Access-Control-Allow-Headers", "Content-Type")
									.withHeader("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
						else if (messageData.containsKey("text"))
							return ok((String) messageData.get("text")).as("text/plain");
						else if (messageData.containsKey("json"))
							return ok((String) messageData.get("json")).as("text/json");
						else if (messageData.containsKey("csv"))
							return ok((String) messageData.get("csv")).as("text/csv");
						else
							return ok();
					}
				}).exceptionally(e -> notFound(service + " not found"));
		return prom;
	}
}
