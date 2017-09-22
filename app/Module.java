import java.io.IOException;

import com.google.inject.AbstractModule;

import nl.tue.id.oocsi.server.OOCSIServer;

public class Module extends AbstractModule {

	@Override
	protected void configure() {
		try {
			bind(OOCSIServer.class).toInstance(new OOCSIServer(4444, 100, true));

		} catch (IOException e) {
		}
	}
}
