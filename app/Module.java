import java.io.IOException;

import com.google.inject.AbstractModule;

import nl.tue.id.oocsi.server.OOCSIServer;

public class Module extends AbstractModule {

	@Override
	protected void configure() {
		try {
			bind(OOCSIServer.class).toInstance(new OOCSIServer(4444, 1000, true) {
				@Override
				protected void internalLog(String message) {
					play.Logger.info(message);
				}
			});
		} catch (IOException e) {
		}
	}
}
