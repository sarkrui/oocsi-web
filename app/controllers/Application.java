package controllers;

import static akka.pattern.Patterns.ask;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.stream.Materializer;
import model.actors.ServiceClientActor;
import model.actors.WebSocketClientActor;
import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import play.Environment;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.inject.ApplicationLifecycle;
import play.libs.streams.ActorFlow;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.compat.java8.FutureConverters;

@Singleton
public class Application extends Controller {

	private final ActorSystem system;
	private final Materializer materializer;
	private final FormFactory formFactory;
	private final OOCSIServer server;
	private final Environment environment;

	@Inject
	public Application(ActorSystem as, Materializer m, ApplicationLifecycle lifecycle, FormFactory f, Environment env,
			OOCSIServer server, Config configuration) {

		this.system = as;
		this.materializer = m;
		this.formFactory = f;
		this.environment = env;
		this.server = server;

		// configure the number of maximal clients
		if (configuration.hasPath("oocsi.clients")) {
			try {
				int clients = configuration.getInt("oocsi.clients");
				OOCSIServer.setMaxClients(clients);
			} catch (Exception e) {
				Logger.warn("Property 'oocsi.clients' not readable or parseable in configuration");
			}
		}

		// register shutdown hook
		lifecycle.addStopHook(() -> {
			server.stop();
			return CompletableFuture.completedFuture(null);
		});
	}

	/**
	 * action to show the landing page for the OOCSI server
	 * 
	 * @return
	 */
	public Result index() {
		String channels = server.getChannelList().replace("OOCSI_connections,", "").replace("OOCSI_clients,", "")
				.replace("OOCSI_events,", "").replace("OOCSI_metrics,", "").replace("OSC,", "");
		if (channels.length() > 160) {
			channels = channels.substring(0, 160) + "...";
		}

		String clients = server.getClientList();
		if (clients.length() > 160) {
			clients = clients.substring(0, 160) + "...";
		}

		return ok(views.html.Application.index.render("index", "", request().host(), environment.isProd(), clients,
				channels));
	}

	/**
	 * action to show a page to make your own dashboard
	 * 
	 * @return
	 */
	public Result dashboard() {
		return ok(views.html.Application.dashboard.render("dashboard", "", request().host(), environment.isProd()));
	}

	/**
	 * action to show a test page for websocket experiments with OOCSI
	 * 
	 * @return
	 */
	public Result test() {
		return ok(views.html.Application.test.render("testing", "", request().host(), environment.isProd()));
	}

	/**
	 * action to show server metrics page
	 * 
	 * @return
	 */
	public Result metrics() {
		return ok(views.html.Application.metrics.render("metrics", "", request().host(), environment.isProd()));
	}

	/**
	 * action to show OOCSI mote page
	 * 
	 * @return
	 */
	public Result mote() {
		return ok(views.html.Application.mote.render("OOCSImote", "", request().host(), environment.isProd()));
	}

	/**
	 * action to show OOCSI mote sharing page
	 * 
	 * @return
	 */
	public Result moteShare() {
		return ok(views.html.Application.moteShare.render("OOCSImote", "", request().host(), environment.isProd()));
	}

	/**
	 * websocket connections end up here
	 * 
	 * @return
	 */
	public WebSocket ws() {
		return WebSocket.Text.accept(
				request -> ActorFlow.actorRef(out -> WebSocketClientActor.props(out, server), system, materializer));
	}

	/**
	 * return list of channels for GET request
	 * 
	 * @return
	 */
	public Result channels() {
		return ok(server.getChannelList());
	}

	/**
	 * handle GET request to send a message to a channel
	 * 
	 * @param channel
	 * @param data
	 * @return
	 */
	public Result sendData(String channel, String data) {
		if (channel == null || channel.trim().length() == 0) {
			return badRequest("ERROR: channel missing");
		} else if (server.getChannel(channel) == null) {
			return notFound("ERROR: channel not found");
		} else {
			Message m = new Message("WEB-REQUEST", channel);
			m.addData("parameter", data);
			server.getChannel(channel).send(m);

			return ok();
		}
	}

	/**
	 * track GET request to send a message to a channel
	 * 
	 * @param channel
	 * @param data
	 * @return
	 */
	public Result track(String channel, String data) {
		if (channel == null || channel.trim().length() == 0) {
			return badRequest("ERROR: channel missing");
		} else if (server.getChannel(channel) == null) {
			return notFound("ERROR: channel not found");
		} else {
			Message m = new Message("WEB-REQUEST", channel);
			m.addData("parameter", data);
			m.addData("User-Agent", request().header("User-Agent").orElse(""));
			m.addData("Referer", request().header("Referer").orElse(""));
			m.addData("Origin", request().header("Origin").orElse(""));

			server.getChannel(channel).send(m);

			return ok();
		}
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

		return internalSend(sender, channel, dynamicForm.rawData());
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

	/**
	 * respond to service check
	 * 
	 * @param service
	 * @return
	 */
	public CompletionStage<Result> serviceIndex(String service) {
		if (server.getChannel(service) != null) {
			return CompletableFuture.completedFuture(ok(service + " ok"));
		} else {
			return CompletableFuture.completedFuture(notFound(service + " not found"));
		}
	}

	/**
	 * respond to GET request to service (forward, return)
	 * 
	 * @param service
	 * @param call
	 * @param data
	 * @return
	 */
	public CompletionStage<Result> serviceCall(String service, String call, String data) {
		if (server.getChannel(service) != null) {
			return internalServiceCall(service, call, data);
		} else {
			return CompletableFuture.completedFuture(notFound(service + " not found"));
		}
	}

	/**
	 * respond to POST request to service (forward, return)
	 * 
	 * @param service
	 * @param call
	 * @return
	 */
	public CompletionStage<Result> serviceCallPost(String service, String call) {
		if (server.getChannel(service) != null) {
			return internalServiceCall(service, call, request().body().asText());
		} else {
			return CompletableFuture.completedFuture(notFound(service + " not found"));
		}
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
		final ActorRef a = system.actorOf(ServiceClientActor.props(server));
		CompletionStage<Result> prom = FutureConverters
				.toJava(ask(a, new ServiceClientActor.ServiceRequest(service, call, data), 5000))
				.thenApply(response -> {

					// kill actor
					a.tell(PoisonPill.getInstance(), a);

					if (response == null) {
						return noContent();
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
				}).exceptionally(e -> {

					// kill actor
					a.tell(PoisonPill.getInstance(), a);

					return notFound(service + " not found");
				});
		return prom;
	}
}
