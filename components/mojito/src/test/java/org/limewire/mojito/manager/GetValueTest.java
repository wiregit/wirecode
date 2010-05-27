package org.limewire.mojito.manager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito.exceptions.NoSuchValueException;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.ExceptionUtils;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.util.StringUtils;

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
    
    public void testGetValueByType() 
            throws IOException, InterruptedException, ExecutionException {
        
        List<MojitoDHT> dhts = Collections.emptyList();
        try {        
            dhts = MojitoUtils.createBootStrappedDHTs(1);
            MojitoDHT first = dhts.get(0);
            Thread.sleep(250);
            
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEXT, Version.ZERO, 
                    StringUtils.toAsciiBytes("Hello World"));
            
            first.put(valueId, value).get();
            
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(
                        valueId, DHTValueType.TEXT);
                
                ValueEntity result = dhts.get(1).get(lookupKey).get();
                DHTValueEntity[] entities = result.getEntities();
                assertEquals(1, entities.length);
                for (DHTValueEntity entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(
                        valueId, DHTValueType.ANY);
                
                ValueEntity result = dhts.get(1).get(lookupKey).get();
                DHTValueEntity[] entities = result.getEntities();
                assertEquals(1, entities.length);
                for (DHTValueEntity entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(valueId, DHTValueType.LIME);
                dhts.get(1).get(lookupKey).get();
                fail("Should have failed!");
            } catch (ExecutionException err) {
                if (!ExceptionUtils.isCausedBy(
                        err, NoSuchValueException.class)) {
                    fail(err);
                }
            }
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
    
    public void testNotSameReference() throws IOException, InterruptedException, ExecutionException {
        List<MojitoDHT> dhts = Collections.emptyList();
        try {
            dhts = MojitoUtils.createBootStrappedDHTs(1);
            MojitoDHT first = dhts.get(0);
            Thread.sleep(250);
            
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEXT, Version.ZERO, 
                    StringUtils.toAsciiBytes("Hello World"));
            
            first.put(valueId, value).get();
            
            EntityKey lookupKey1 = EntityKey.createEntityKey(
                    valueId, DHTValueType.TEXT);
            ValueEntity result1 = dhts.get(1).get(lookupKey1).get();
            
            EntityKey lookupKey2 = EntityKey.createEntityKey(
                    valueId, DHTValueType.ANY);
            ValueEntity result2 = dhts.get(1).get(lookupKey2).get();
            
            assertNotSame(result1, result2);
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
