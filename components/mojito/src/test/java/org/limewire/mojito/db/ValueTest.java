package org.limewire.mojito.db;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.Test;

import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.IoUtils;
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
