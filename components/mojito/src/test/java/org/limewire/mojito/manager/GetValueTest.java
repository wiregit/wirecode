package org.limewire.mojito.manager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.mojito.ValueKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.exceptions.NoSuchValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.storage.DefaultValue;
import org.limewire.mojito.storage.ValueType;
import org.limewire.mojito.util.IoUtils;
import org.limewire.util.ExceptionUtils;
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
            Value value = new DefaultValue(
                    ValueType.TEXT, Version.ZERO, 
                    StringUtils.toAsciiBytes("Hello World"));
            
            first.put(valueId, value).get();
            
            try {
                ValueKey lookupKey = ValueKey.createValueKey(
                        valueId, ValueType.TEXT);
                
                ValueEntity result = dhts.get(1).get(lookupKey).get();
                ValueTuple[] entities = result.getValues();
                assertEquals(1, entities.length);
                for (ValueTuple entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                ValueKey lookupKey = ValueKey.createValueKey(
                        valueId, ValueType.ANY);
                
                ValueEntity result = dhts.get(1).get(lookupKey).get();
                ValueTuple[] entities = result.getValues();
                assertEquals(1, entities.length);
                for (ValueTuple entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                ValueKey lookupKey = ValueKey.createValueKey(valueId, ValueType.LIME);
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
            Value value = new DefaultValue(
                    ValueType.TEXT, Version.ZERO, 
                    StringUtils.toAsciiBytes("Hello World"));
            
            first.put(valueId, value).get();
            
            ValueKey lookupKey1 = ValueKey.createValueKey(
                    valueId, ValueType.TEXT);
            ValueEntity result1 = dhts.get(1).get(lookupKey1).get();
            
            ValueKey lookupKey2 = ValueKey.createValueKey(
                    valueId, ValueType.ANY);
            ValueEntity result2 = dhts.get(1).get(lookupKey2).get();
            
            assertNotSame(result1, result2);
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
