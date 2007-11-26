package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.lws.server.LWSManager;

/**
 * A class that initializes listeners to the passed in instance of {@link LWSManager}.
 */
public interface LWSIntegrationServices {
    
    /**
     * Initializes this in the start up of the {@link LifecycleManager}
     */
    void init();
}
