package com.limegroup.gnutella.dht.db;

import java.util.Collection;
import java.util.Collections;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.util.DatabaseUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;

@Singleton
public class PrivateGroupsModel implements StorableModel{
    
    
    private NetworkManager networkManager;
    private PrivateGroupsValueFactory privateGroupsValueFactory;
    private ApplicationServices applicationServices;
    private DHTManager dhtManager;
    
    
    
    @Inject
    PrivateGroupsModel(DHTManager dhtManager, NetworkManager networkManager, 
            PrivateGroupsValueFactory privateGroupsValueFactory, ApplicationServices applicationServices){  
        this.networkManager = networkManager;
        this.privateGroupsValueFactory = privateGroupsValueFactory;
        this.applicationServices = applicationServices;
        this.dhtManager = dhtManager;
        
    }


    public Collection<Storable> getStorables() {
        
        MojitoDHT dht = dhtManager.getMojitoDHT();
        
        //KUID kuid = dht.getLocalNodeID();
        
        byte[] publishName = "elven".getBytes();
        byte[] publishArray = new byte[20];
        
        if (publishName.length < 20){
            //pad the byte array until there are 20 characters
            for (int i = 0;i<publishName.length;i++)
                publishArray[i] = publishName[i];
        }
        else if (publishName.length >20){
            //truncate name
            for(int i = 0; i<20; i++)
                publishArray[i] = publishName[i];
        }
        
        KUID kuid = KUID.createWithBytes(publishArray);
        
        
        System.out.println("********************************************KUID in getStorables: " + kuid);
        
        Storable newItem = new Storable(kuid, (privateGroupsValueFactory.createDHTValueForSelf()));        
        if(DatabaseUtils.isPublishingRequired(newItem))
            return Collections.singleton(newItem);
        return Collections.emptySet();
    }

    public void handleContactChange() {
        // TODO Auto-generated method stub
        
    }

    public void handleStoreResult(Storable value, StoreResult result) {
        // TODO Auto-generated method stub
        
    }   
}
