package com.limegroup.gnutella;
import com.limegroup.gnutella.settings.*;

/**
 * Specialized class that creates a <tt>Backend</tt> that only accepts local
 * connections.
 */

public class LocalBackend extends Backend {

	LocalBackend(ActivityCallback callback, MessageRouter router,
				 int timeout, int port) {
		super(callback, router, timeout, port);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		SettingsManager settings = SettingsManager.instance();
        settings.setBannedIps(new String[] {"*.*.*.*"});
        settings.setAllowedIps(new String[] {
			"127.*.*.*", "10.254.0.*",
		});		
	}
}
