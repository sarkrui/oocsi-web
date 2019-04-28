import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import nl.tue.id.oocsi.OOCSICommunicator;
import nl.tue.id.oocsi.OOCSIData;
import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.services.Responder;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.test.WSTestClient;
import play.test.WithServer;

public class WebsocketServiceTest extends WithServer {

	@Test
	public void testServiceIndex() throws InterruptedException {
		OOCSICommunicator oco = new OOCSICommunicator(this, "newService");
		oco.connect("127.0.0.1", 4444);

		assertTrue(oco.isConnected());

		oco.register("serviceOne", new Responder() {
			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				response.data("html", "hello world!");
			}
		});

		oco.register("serviceTwo", new Responder() {
			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				// response.data("html", "hello world!");
				int number = event.getInt("number", -1);
				response.data("html", "hello world with number " + (number + 1));
			}
		});

		Thread.sleep(1000);

		final WSClient ws = WSTestClient.newClient(3333);

		// test service availability
		{
			final WSRequest request = ws
					.url("http://127.0.0.1:" + testServer.getRunningHttpPort().getAsInt() + "/serviceOne");

			CompletionStage<WSResponse> completionStage = request.get();
			WSResponse response;
			try {
				response = completionStage.toCompletableFuture().get();
				String body = response.getBody();
				assertEquals(200, response.getStatus());
				assertTrue(body.contains("serviceOne ok"));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		// test service response
		{
			final WSRequest request = ws.url("http://127.0.0.1:" + testServer.getRunningHttpPort().getAsInt()
					+ "/serviceOne/index.html/somethingelse");

			CompletionStage<WSResponse> completionStage = request.get();
			WSResponse response;
			try {
				response = completionStage.toCompletableFuture().get();
				String body = response.getBody();
				assertEquals(200, response.getStatus());
				assertTrue(body.contains("hello world!"));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		// test service availability 2
		{
			final WSRequest request = ws
					.url("http://127.0.0.1:" + testServer.getRunningHttpPort().getAsInt() + "/serviceTwo");

			CompletionStage<WSResponse> completionStage = request.get();
			WSResponse response;
			try {
				response = completionStage.toCompletableFuture().get();
				String body = response.getBody();
				assertEquals(200, response.getStatus());
				assertTrue(body.contains("serviceTwo ok"));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		// test service response
		{
			final WSRequest request = ws.url("http://127.0.0.1:" + testServer.getRunningHttpPort().getAsInt()
					+ "/serviceTwo/index.html/{\"number\":1}");

			CompletionStage<WSResponse> completionStage = request.get();
			WSResponse response;
			try {
				response = completionStage.toCompletableFuture().get();
				String body = response.getBody();
				assertEquals(200, response.getStatus());
				assertTrue(body.contains("hello world "));
				assertTrue(body.contains("number 2"));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		Thread.sleep(1000);
	}
}
