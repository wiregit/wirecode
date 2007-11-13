package com.limegroup.gnutella.downloader;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AbstractPrivateGroupsValue;
import com.limegroup.gnutella.dht.db.PrivateGroupsFinder;
import com.limegroup.gnutella.dht.db.PrivateGroupsValue;

@Singleton
public class PrivateGroupsManager implements DHTEventListener{

    private static PrivateGroupsFinder finder;
    private Set<EntityKey> privateGroupKeyList = new HashSet<EntityKey>();
    

    @Inject
    public PrivateGroupsManager(DHTManager manager, ApplicationServices applicationServices){
        PrivateGroupsManager.finder = new PrivateGroupsFinder(manager, applicationServices);
        
    }
    
    /**
     * Adds a buddy to the buddy list 
     */
    public void addEntityKey(EntityKey key){
        privateGroupKeyList.add(key);
    }
    
    /**
     * Removes a buddy from the buddy list 
     */
    public void removeEntityKey(EntityKey key){
        privateGroupKeyList.remove(key);
    }
       
    /**
     * Checks if buddy's are found
     */
    public boolean findBuddy(){
        
        return true;
    }
    
    public FindValueResult getResult() throws InterruptedException, ExecutionException{
        EntityKey nextKey;
        FindValueResult result = null;
        
        for(Iterator i = privateGroupKeyList.iterator(); i.hasNext();){
            nextKey = (EntityKey) i.next();
            result = finder.find(nextKey);
        } 
        return result;      
    }
    
    public byte[] findIPAddress(FindValueResult result){
        PrivateGroupsValue pValue = null;
        if(result != null&&result.getEntities().isEmpty()==false){
            
            //get ip address from the result
            for(Iterator j = result.getEntities().iterator(); j.hasNext();){
                DHTValueEntity value = (DHTValueEntity)j.next();
                if (value.getValue().getValueType().equals(AbstractPrivateGroupsValue.PRIVATE_GROUPS)){
                     pValue = (PrivateGroupsValue) value.getValue();
                     return pValue.getIPAddress();
                }   
            }
        }
        return null;
    }
    
    
    
    public void handleDHTEvent(DHTEvent evt) {
        // TODO Auto-generated method stub
    }   
       
}
