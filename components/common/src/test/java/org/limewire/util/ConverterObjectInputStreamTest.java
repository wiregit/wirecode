package org.limewire.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Test;

public class ConverterObjectInputStreamTest extends BaseTestCase {

    public ConverterObjectInputStreamTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ConverterObjectInputStreamTest.class);
    }

    private ConverterObjectInputStream getSerializedInputStream(Serializable s) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(out);
        
        objectStream.writeObject(s);
        objectStream.flush();
        objectStream.close();
        
        return getSerializedInputStream(out);
    }
    
    private ConverterObjectInputStream getSerializedInputStream(byte[] out) throws Exception {
        return new ConverterObjectInputStream(new ByteArrayInputStream(out));
    }
    
    private ConverterObjectInputStream getSerializedInputStream(ByteArrayOutputStream out) throws Exception {
        return getSerializedInputStream(out.toByteArray());
    }
    
    @SuppressWarnings("unchecked")
    public void testLookupOfUnregisteredClass() throws Exception {
        ArrayList<SerializableClass> list = new ArrayList<SerializableClass>();
        list.add(new SerializableClass());
        
        ConverterObjectInputStream in = getSerializedInputStream(list);
        
        ArrayList<SerializableClass> list2 = (ArrayList<SerializableClass>)in.readObject();
        assertEquals(list.size(), list2.size());
    }
    
    public void testAddedClassMapping() throws Exception {
        ConverterObjectInputStream in = getSerializedInputStream(new SerializableClass());
        in.addLookup(SerializableClass.class.getName(), NewSerializableClass.class.getName());
        
        Object instance = in.readObject();
        assertTrue(instance instanceof NewSerializableClass);
    }
    
    public void testOldCollectionClasses() throws Exception {
        byte[] hashMapInsideOfArrayList = new byte[] { -84, -19, 0, 5, 115, 114, 0, 39, 99, 111, 109, 46, 115, 117, 110, 46, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 99, 111, 108, 108, 101, 99, 116, 105, 111, 110, 115, 46, 65, 114, 114, 97, 121, 76, 105, 115, 116, 77, -94, -80, -33, 64, 10, 78, -108, 3, 0, 1, 73, 0, 4, 115, 105, 122, 101, 120, 112, 0, 0, 0, 1, 119, 4, 0, 0, 0, 10, 115, 114, 0, 37, 99, 111, 109, 46, 115, 117, 110, 46, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 99, 111, 108, 108, 101, 99, 116, 105, 111, 110, 115, 46, 72, 97, 115, 104, 77, 97, 112, 80, 0, 33, 58, -95, -15, 79, 40, 3, 0, 2, 70, 0, 10, 108, 111, 97, 100, 70, 97, 99, 116, 111, 114, 73, 0, 9, 116, 104, 114, 101, 115, 104, 111, 108, 100, 120, 112, 63, 64, 0, 0, 0, 0, 0, 75, 119, 8, 0, 0, 0, 101, 0, 0, 0, 0, 120, 120 };
        
        ConverterObjectInputStream in = getSerializedInputStream(hashMapInsideOfArrayList);
        
        Object instance = in.readObject();
        assertTrue(instance instanceof ArrayList);
        ArrayList list = (ArrayList)instance;
        assertEquals(1, list.size());
        instance = list.get(0);
        assertTrue(instance instanceof HashMap);
    }
    
    private static class SerializableClass implements Serializable {
        
    }
    
    private static class NewSerializableClass implements Serializable {
        
    }
    
}
