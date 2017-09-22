import nl.tue.id.oocsi.server.OOCSIServer;
import play.*;

public class Global extends GlobalSettings {

	public void onStart(Application app) {
	}

	public void onStop(Application app) {
		Logger.info("Stopping TCP server...");
		OOCSIServer server = Play.application().injector().instanceOf(OOCSIServer.class);
		server.stop();
	}

}