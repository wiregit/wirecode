package org.limewire.mojito.db;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito.util.UnitTestUtils;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.SecurityTokenEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.security.SecurityToken;
import org.limewire.util.StringUtils;

public class ValueTest extends MojitoTestCase {

    public ValueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testGetSecurityToken() throws IOException, 
            SecurityException, IllegalArgumentException, 
            NoSuchFieldException, IllegalAccessException, 
            InterruptedException, ExecutionException {
        
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            
            dht1 = MojitoFactory.createDHT("DHT-1", 2000);
            dht2 = MojitoFactory.createDHT("DHT-2", 3000);
            
            // Setup the first instance so that it thinks it's bootstrapping
            UnitTestUtils.setBooting(dht1, true);            
            assertFalse(dht1.isReady());
            assertTrue(dht1.isBooting());
            
            // And setup the second instance so that it thinks it's bootstrapped 
            UnitTestUtils.setReady(dht2, true);
            assertTrue(dht2.isReady());
            assertFalse(dht2.isBooting());
            
            // Get the SecurityToken...
            Context context1 = dht1.getContext();
            
            DHTFuture<SecurityTokenEntity> future 
                = context1.getSecurityToken(dht2.getLocalNode(), 
                    500, TimeUnit.MILLISECONDS);
            
            SecurityTokenEntity entity = future.get();
            SecurityToken token = entity.getSecurityToken();
            
            assertNotNull(token);
            
        } finally {
            IoUtils.closeAll(dht1, dht2);
        }
    }

    public void testStoreAndLookupValue() 
            throws InterruptedException, ExecutionException, IOException {
        
        List<MojitoDHT> dhts = null;
        try {
            dhts = MojitoUtils.createBootStrappedDHTs(10);
            
            KUID key = KUID.createRandomID();
            
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEST, 
                    Version.ZERO, 
                    StringUtils.toUTF8Bytes("Hello World"));
            
            // STORE
            dhts.get(0).put(key, value).get();
            
            // FIND_VALUE (check every Node)
            EntityKey lookupKey = EntityKey.createEntityKey(
                    key, DHTValueType.ANY);
            for (MojitoDHT dht : dhts) {
                ValueEntity entity = dht.get(lookupKey).get();
                DHTValueEntity[] values = entity.getEntities();
                
                assertEquals(1, values.length);
                
                DHTValue v = values[0].getValue();
                assertEquals(value, v);
            }
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
