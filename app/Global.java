import nl.tue.id.oocsi.server.OOCSIServer;
import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

	public void onStart(Application app) {
		if (app.configuration().getInt("oocsi.clients") != null) {
			int clients = app.configuration().getInt("oocsi.clients");
			OOCSIServer.setMaxClients(clients);
		}
	}

	public void onStop(Application app) {
		Logger.info("Stopping TCP server...");
		OOCSIServer server = app.injector().instanceOf(OOCSIServer.class);
		server.stop();
	}

}