package org.limewire.mojito.db;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.mojito.ValueKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.SecurityTokenEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.storage.DefaultValue;
import org.limewire.mojito.storage.ValueType;
import org.limewire.mojito.util.IoUtils;
import org.limewire.mojito.util.UnitTestUtils;
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
            DHTFuture<SecurityTokenEntity> future 
                = dht1.getSecurityToken(dht2.getLocalNode(), 
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
            
            Value value = new DefaultValue(
                    ValueType.TEST, 
                    Version.ZERO, 
                    StringUtils.toUTF8Bytes("Hello World"));
            
            // STORE
            dhts.get(0).put(key, value).get();
            
            // FIND_VALUE (check every Node)
            ValueKey lookupKey = ValueKey.createEntityKey(
                    key, ValueType.ANY);
            for (MojitoDHT dht : dhts) {
                ValueEntity entity = dht.get(lookupKey).get();
                ValueTuple[] values = entity.getValues();
                
                assertEquals(1, values.length);
                
                Value v = values[0].getValue();
                assertEquals(value, v);
            }
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
