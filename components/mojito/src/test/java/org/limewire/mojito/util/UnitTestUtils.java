package org.limewire.mojito.util;

import java.lang.reflect.Field;

import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.BootstrapEntity;


public class UnitTestUtils {
    
    private UnitTestUtils() {
        
    }
    
    public static void setBooting(MojitoDHT dht, boolean booting) throws Exception {
        Field field = Context.class.getDeclaredField("bootstrap");
        field.setAccessible(true);
        
        DHTFuture<BootstrapEntity> future = null;
        if (booting) {
            future = new DHTValueFuture<BootstrapEntity>();
        }
        
        Context context = dht.getContext();
        field.set(context, future);
    }
    
    public static void setReady(MojitoDHT dht, boolean ready) throws Exception {
        Field field = Context.class.getDeclaredField("bootstrap");
        field.setAccessible(true);
        
        DHTFuture<BootstrapEntity> future = null;
        if (ready) {
            future = new DHTValueFuture<BootstrapEntity>(
                    (BootstrapEntity)null);
        }
        
        Context context = dht.getContext();
        field.set(context, future);
    }
}
