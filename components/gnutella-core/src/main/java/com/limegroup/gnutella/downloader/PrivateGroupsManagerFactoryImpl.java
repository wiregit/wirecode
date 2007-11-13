package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.PrivateGroupsFinder;

@Singleton
public class PrivateGroupsManagerFactoryImpl implements PrivateGroupsManagerFactory {

    private DHTManager dhtManager;
    private ApplicationServices applicationServices;
    

    @Inject
    public PrivateGroupsManagerFactoryImpl(DHTManager dhtManager, ApplicationServices applicationServices){
        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;

    }
    
    public PrivateGroupsManager createPrivateGroupsManager() {   
        return new PrivateGroupsManager(dhtManager, applicationServices);
    }
    
    

}
