package com.limegroup.gnutella.dht.db;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.dht.DHTManager;




@Singleton
public class PrivateGroupsFinder {

    private DHTManager manager;
    private ApplicationServices applicationServices;

    
    @Inject
    public PrivateGroupsFinder(DHTManager manager, ApplicationServices applicationServices){
        this.manager = manager;
        this.applicationServices = applicationServices;
    }
       
    /**
     * Retrieves "PrivateGroupsValues" in the DHT
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public FindValueResult find(EntityKey lookupKey) throws InterruptedException, ExecutionException{

            MojitoDHT dht = manager.getMojitoDHT();
            DHTFuture<FindValueResult> future;
            FindValueResult result = null;
            
            if(dht == (null)|| !dht.isBootstrapped())
                System.out.println("dht is null or not bootstrapped");
            
            else{
                    future = dht.get(lookupKey);
                    future.addDHTFutureListener(new futureGetListener());
                    result = future.get();
                    
                }
                
            if (result==null || result.getEntities().isEmpty()){
                System.out.println("result is null or empty");
            }
                /*
                // Values that were returned with the response
                for (DHTValueEntity entity : result.getEntities()) {
                    //System.out.println(entity);
                }
                
                // Pull remaining values from the remote Node (if any)
                for (EntityKey keyRemaining : result.getEntityKeys()) {
                    DHTFuture<FindValueResult> fut = dht.get(keyRemaining);
                    FindValueResult res;
                    //res.addDHTFutureListener(new futureGetListener());
                    res = fut.get();
                    
                    for (DHTValueEntity entity : res.getEntities()) {
                        //System.out.println(entity);
                    }
                }*/
                                
            return result;
            //}
    }
    
    
    class futureGetListener implements DHTFutureListener<FindValueResult>{

        public void handleCancellationException(CancellationException e) {   
            System.out.println("OH NO CANCELLED");
        }

        public void handleExecutionException(ExecutionException e) {
            System.out.println("TIMED OUT");           
        }

        public void handleFutureSuccess(FindValueResult result) {   
        }

        public void handleInterruptedException(InterruptedException e) {
            System.out.println("INTERRUPTED");
            
        }
    }
    
    
    
    
}
