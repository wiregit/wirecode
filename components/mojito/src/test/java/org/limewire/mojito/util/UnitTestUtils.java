package org.limewire.mojito.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.limewire.concurrent.OnewayExchanger;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.manager.BootstrapManager;
import org.limewire.mojito.settings.KademliaSettings;


public class UnitTestUtils {
    
    private UnitTestUtils() {
        
    }
    
    @SuppressWarnings("unchecked")
    public static void setBootstrapping(MojitoDHT dht, boolean bootstrapping) throws Exception {
        Context context = (Context)dht;
        Field bmField = Context.class.getDeclaredField("bootstrapManager");
        bmField.setAccessible(true);
        
        BootstrapManager bootstrapManager = (BootstrapManager)bmField.get(context);
        Field futureField = BootstrapManager.class.getDeclaredField("future");
        futureField.setAccessible(true);
        
        synchronized (bootstrapManager) {
            if (bootstrapping) {
                Class clazz = Class.forName("org.limewire.mojito.manager.BootstrapManager$BootstrapFuture");
                Constructor con = clazz.getDeclaredConstructor(BootstrapManager.class, DHTTask.class);
                con.setAccessible(true);
                
                Object future = con.newInstance(bootstrapManager, new DHTTask() {
                    public void cancel() {
                        throw new UnsupportedOperationException();
                    }

                    public long getWaitOnLockTimeout() {
                        throw new UnsupportedOperationException();
                    }

                    public void start(OnewayExchanger exchanger) {
                        throw new UnsupportedOperationException();
                    } 
                });
                
                futureField.set(bootstrapManager, future);
            } else {
                futureField.set(bootstrapManager, null);
            }
        }
    }
    
    public static void setBootstrapped(MojitoDHT dht, boolean bootstrapped) throws Exception {
        Context context = (Context)dht;
        Field bmField = Context.class.getDeclaredField("bootstrapManager");
        bmField.setAccessible(true);
        
        BootstrapManager bootstrapManager = (BootstrapManager)bmField.get(context);
        bootstrapManager.setBootstrapped(bootstrapped);
    }
    
    public static void setNodeID(MojitoDHT dht, KUID nodeId) throws Exception {
        Method m = dht.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
        m.setAccessible(true);
        m.invoke(dht, new Object[]{nodeId});
    }
    
    /**
     * Creates <code>factor</code> * {@link KademliaSettings#REPLICATION_PARAMETER} bootstrapped dhts.
     * 
     * Make sure to close them in a try-finally block.
     */
    public static List<MojitoDHT> createBootStrappedDHTs(int factor) throws Exception {
        if (factor < 1) {
            throw new IllegalArgumentException("only values >= 1");
        }
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        
        for (int i = 0; i < factor * k; i++) {
            MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
            
            dht.bind(new InetSocketAddress(3000 + i));
            dht.start();
            
            if (i > 0) {
                Thread.sleep(100);
                dht.bootstrap(new InetSocketAddress("localhost", 3000)).get();
            }
            dhts.add(dht);
        }
        dhts.get(0).bootstrap(dhts.get(1).getContactAddress()).get();
        return dhts;
    }
    
    /**
     * Creates <code>factor</code> * {@link KademliaSettings#REPLICATION_PARAMETER} bootstrapped dhts
     * and stores each under its node id in a map.
     * 
     * Make sure to close them in a try-finally block.
     */
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(int factor) throws Exception {
        if (factor < 1) {
            throw new IllegalArgumentException("only values >= 1");
        }
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        Map<KUID, MojitoDHT> dhts = new LinkedHashMap<KUID, MojitoDHT>();
        MojitoDHT first = null;
        
        for (int i = 0; i < factor * k; i++) {
            MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
            
            dht.bind(new InetSocketAddress(3000 + i));
            dht.start();
            
            if (i > 0) {
                Thread.sleep(100);
                dht.bootstrap(new InetSocketAddress("localhost", 3000)).get();
            } else {
                first = dht;
            }
            dhts.put(dht.getLocalNodeID(), dht);
        }
        if (first != null) { // unnecessary null check to satisfy compiler
            first.bootstrap(new InetSocketAddress("localhost", 3000 + 1)).get();
        }
        return dhts;
    }
}
