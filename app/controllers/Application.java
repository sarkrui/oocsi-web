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
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import play.Play;
import play.data.DynamicForm;
import play.data.FormFactory;
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

	@Inject
	FormFactory formFactory;

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

	/**
	 * return list of channels for GET request
	 * 
	 * @return
	 */
	public Result channels() {
		return ok(Play.application().injector().instanceOf(OOCSIServer.class).getChannelList());
	}

	/**
	 * handle POST request to send a message to a channel
	 * 
	 * @param channel
	 * @return
	 */
	public Result send(String channel) {
		DynamicForm dynamicForm = formFactory.form().bindFromRequest();

		// check server available
		String sender = dynamicForm.get("sender");
		if (sender == null || sender.trim().isEmpty()) {
			return badRequest("ERROR: sender missing");
		}

		// // check channel available
		if (channel == null || channel.trim().isEmpty()) {
			return badRequest("ERROR: channel missing");
		}

		return internalSend(sender, channel, dynamicForm.data());
	}

	/**
	 * internal handling of web send request
	 * 
	 * @param sender
	 * @param channel
	 * @param messageData
	 * @return
	 */
	private Result internalSend(String sender, String channel, Map<String, String> messageData) {
		// start message processing
		OOCSIServer server = Play.application().injector().instanceOf(OOCSIServer.class);

		// check whether there is another client with same name (-> abort)
		Client serverClient = server.getClient(sender);
		if (serverClient != null) {
			return unauthorized("ERROR: different client with same name exists");
		}

		// check whether recipient channel is active
		Channel serverChannel = server.getChannel(channel);
		if (serverChannel != null) {
			// create message
			Message message = new Message(sender, channel);

			// fill message
			for (String key : messageData.keySet()) {
				if (!key.equals("sender") || !key.equals("channel") || !key.equals("recipient")
						|| !key.equals("timestamp") || !key.equals("")) {
					message.addData(key, messageData.get(key));
				}
			}

			// send message
			serverChannel.send(message);
		} else {
			return notFound("ERROR: channel not registered");
		}

		return ok("Message delivered to " + channel);
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
