package com.limegroup.mojito.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.manager.BootstrapManager;

public class UnitTestUtils {
    
    private UnitTestUtils() {
        
    }
    
    public static void setBootstrapping(MojitoDHT dht) throws Exception {
        Context context = (Context)dht;
        Field bmField = Context.class.getDeclaredField("bootstrapManager");
        bmField.setAccessible(true);
        
        BootstrapManager bootstrapManager = (BootstrapManager)bmField.get(context);
        Field futureField = BootstrapManager.class.getDeclaredField("future");
        futureField.setAccessible(true);
        
        Class clazz = Class.forName("com.limegroup.mojito.manager.BootstrapManager$BootstrapFuture");
        Constructor con = clazz.getDeclaredConstructor(BootstrapManager.class, Callable.class);
        con.setAccessible(true);
        
        Object future = con.newInstance(bootstrapManager, new Callable() { 
            public Object call() { 
                throw new UnsupportedOperationException();
            }
        });
        
        futureField.set(bootstrapManager, future);
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
