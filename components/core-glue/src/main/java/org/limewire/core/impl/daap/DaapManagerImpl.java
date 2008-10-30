package org.limewire.core.impl.daap;

import java.io.IOException;

import org.limewire.core.api.daap.DaapManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DaapManagerImpl implements DaapManager {

    private com.limegroup.gnutella.daap.DaapManager coreDaapManager;
    
    @Inject
    public DaapManagerImpl(com.limegroup.gnutella.daap.DaapManager coreDaapManager) {
        this.coreDaapManager = coreDaapManager;
    }
    
    @Override
    public void disconnectAll() {
        coreDaapManager.disconnectAll();
    }

    @Override
    public void restart() throws IOException {
        coreDaapManager.restart();
    }

    @Override
    public void stop() {
        coreDaapManager.stop();
    }

    @Override
    public void updateService() throws IOException {
        coreDaapManager.updateService();
    }

    @Override
    public boolean isServerRunning() {
        return coreDaapManager.isServerRunning();
    }
}
