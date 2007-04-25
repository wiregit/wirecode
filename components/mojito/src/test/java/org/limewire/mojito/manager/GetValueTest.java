package org.limewire.mojito.manager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestSuite;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;

public class GetValueTest extends MojitoTestCase {
    
    public GetValueTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(GetValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }
    
    public void testGetValueByType() throws Exception {
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        
        MojitoDHT first = null;
        try {
            for (int i = 0; i < k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                
                dht.bind(new InetSocketAddress(3000 + i));
                dht.start();
                
                if (i > 0) {
                    dht.bootstrap(new InetSocketAddress("localhost", 3000)).get();
                } else {
                    first = dht;
                }
                dhts.add(dht);
            }
            first.bootstrap(new InetSocketAddress("localhost", 3001)).get();
            Thread.sleep(250);
            
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEXT, Version.ZERO, "Hello World".getBytes());
            first.put(valueId, value).get();
            
            try {
                FindValueResult result = dhts.get(1).get(valueId, DHTValueType.TEXT).get();
                Collection<? extends DHTValueEntity> entities = result.getEntities();
                assertEquals(1, entities.size());
                for (DHTValueEntity entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                FindValueResult result = dhts.get(1).get(valueId, DHTValueType.ANY).get();
                Collection<? extends DHTValueEntity> entities = result.getEntities();
                assertEquals(1, entities.size());
                for (DHTValueEntity entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                FindValueResult result = dhts.get(1).get(valueId, DHTValueType.LIME).get();
                Collection<? extends DHTValueEntity> entities = result.getEntities();
                assertEquals("Got " + entities, 0, entities.size());
            } catch (Exception err) {
                fail(err);
            }
            
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
    
    public void testNotSameReference() throws Exception {
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        MojitoDHT first = null;
        try {
            for (int i = 0; i < k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                
                dht.bind(new InetSocketAddress(3000 + i));
                dht.start();
                
                if (i > 0) {
                    dht.bootstrap(new InetSocketAddress("localhost", 3000)).get();
                } else {
                    first = dht;
                }
                dhts.add(dht);
            }
            first.bootstrap(new InetSocketAddress("localhost", 3001)).get();
            Thread.sleep(250);
            
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEXT, Version.ZERO, "Hello World".getBytes());
            first.put(valueId, value).get();
            
            FindValueResult result1 = dhts.get(1).get(valueId, DHTValueType.TEXT).get();
            FindValueResult result2 = dhts.get(1).get(valueId, DHTValueType.ANY).get();
            
            assertNotSame(result1, result2);
            
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
}
