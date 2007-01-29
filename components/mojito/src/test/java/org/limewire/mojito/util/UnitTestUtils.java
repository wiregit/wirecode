package org.limewire.mojito.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.manager.BootstrapManager;


public class UnitTestUtils {
    
    private UnitTestUtils() {
        
    }
    
    @SuppressWarnings("unchecked")
    public static void setBootstrapping(MojitoDHT dht, KUID nodeId) throws Exception {
        Context context = (Context)dht;
        Field bmField = Context.class.getDeclaredField("bootstrapManager");
        bmField.setAccessible(true);
        
        BootstrapManager bootstrapManager = (BootstrapManager)bmField.get(context);
        Field futureField = BootstrapManager.class.getDeclaredField("future");
        futureField.setAccessible(true);
        
        synchronized (bootstrapManager) {
            if (nodeId != null) {
                Class clazz = Class.forName("org.limewire.mojito.manager.BootstrapManager$BootstrapFuture");
                Constructor con = clazz.getDeclaredConstructor(BootstrapManager.class, Callable.class);
                con.setAccessible(true);
                
                Object future = con.newInstance(bootstrapManager, new Callable() { 
                    public Object call() { 
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
}
