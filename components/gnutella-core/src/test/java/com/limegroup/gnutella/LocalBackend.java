package com.limegroup.gnutella;

/**
 * Specialized class that creates a <tt>Backend</tt> that only accepts local
 * connections.
 */

public class LocalBackend extends Backend {

	LocalBackend(ActivityCallback callback, MessageRouter router,
				 int timeout) {
		super(callback, router, timeout);
		SettingsManager settings = SettingsManager.instance();
        settings.setBannedIps(new String[] {"*.*.*.*"});
        settings.setAllowedIps(new String[] {
			"127.*.*.*", "18.239.0.*", "10.254.0.*",
		});		
	}
}
