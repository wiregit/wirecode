package org.limewire.core.impl;

import java.lang.reflect.InvocationTargetException;

import org.limewire.core.api.ActivationTest;
import org.limewire.util.PrivateAccessor;

import com.google.inject.Inject;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.search.QuerySettings;

public class ActivationTestImpl implements ActivationTest {

    private final QuerySettings querySetting;
    private final ManagedDownloader managedDownloader;
    private final ConnectionManager connectionManager;
    
    @Inject
    public ActivationTestImpl(QuerySettings querySetting, ManagedDownloader managedDownloader,
            ConnectionManager connectionManager) {
        this.querySetting = querySetting;
        this.managedDownloader = managedDownloader;
        this.connectionManager = connectionManager;
    }

    @Override
    public int getNumUltraPeers() {
        if(connectionManager instanceof ConnectionManagerImpl) {
            ConnectionManagerImpl impl = (ConnectionManagerImpl) connectionManager;
            try {
                PrivateAccessor accessor = new PrivateAccessor(ConnectionManagerImpl.class, impl, "_preferredConnections");
                int value = (Integer) accessor.getValue();
                return value;
            } catch (Exception e) {
                return 0;
            } 
        }
        return -1;
    }
    
    @Override
    public int getSwarmSpeed() {
//        if(managedDownloader instanceof ManagedDownloaderImpl)
//            return ((ManagedDownloaderImpl)managedDownloader).getSwarmCapacity();
//        else
            return -1;
    }
    
    @Override
    public int getNumResults() {
        return querySetting.getUltrapeerResults();
    }
    
    
}
