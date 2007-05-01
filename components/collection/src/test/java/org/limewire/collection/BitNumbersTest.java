package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BitNumbersTest extends BaseTestCase {
            
    public BitNumbersTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BitNumbersTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testNumberConstruction() {
        BitNumbers bn = new BitNumbers(15);
        assertTrue(bn.isEmpty());
        assertEquals(15, bn.getMax());
        for(int i = 0; i < 32; i++)
            assertFalse(bn.isSet(i));        
        assertEquals("", bn.toHexString());
        assertEquals(new byte[0], bn.toByteArray());
        
        bn.set(0);
        bn.set(3);
        bn.set(5);
        bn.set(10);
        assertFalse(bn.isEmpty());
        assertEquals(15, bn.getMax());
        for(int i = 0; i < 15; i++)
            assertEquals(i == 0 || i == 3 || i == 5 || i == 10, bn.isSet(i));
        assertEquals("942", bn.toHexString());
        assertEquals(new byte[] { (byte)0x94, 0x20 }, bn.toByteArray());
    }
    
    public void testByteArrayConstruction() {
        byte[] b = new byte[] { (byte)0x94, 0x20 };
        BitNumbers bn = new BitNumbers(b);
        assertFalse(bn.isEmpty());
        assertEquals(16, bn.getMax());
        for(int i = 0; i < 15; i++)
            assertEquals(i == 0 || i == 3 || i == 5 || i == 10, bn.isSet(i));
        assertEquals("942", bn.toHexString());
        assertEquals(new byte[] { (byte)0x94, 0x20 }, bn.toByteArray());
        
        bn.set(1);
        bn.set(11);
        bn.set(15);
        for(int i = 0; i < 16; i++)
            assertEquals(i == 0 || i == 1 || i == 3 || i == 5 || i == 10 || i == 11 || i == 15, bn.isSet(i));
        assertEquals("D431", bn.toHexString());
        assertEquals(new byte[] { (byte)0xD4, 0x31 }, bn.toByteArray());
    }
    
    public void testStringConstructionWhole() {
        BitNumbers bn = new BitNumbers("9420");
        assertFalse(bn.isEmpty());
        assertEquals(16, bn.getMax());
        for(int i = 0; i < 15; i++)
            assertEquals(i == 0 || i == 3 || i == 5 || i == 10, bn.isSet(i));
        assertEquals("942", bn.toHexString());
        assertEquals(new byte[] { (byte)0x94, 0x20 }, bn.toByteArray());
        
        bn.set(1);
        bn.set(11);
        bn.set(15);
        for(int i = 0; i < 16; i++)
            assertEquals(i == 0 || i == 1 || i == 3 || i == 5 || i == 10 || i == 11 || i == 15, bn.isSet(i));
        assertEquals("D431", bn.toHexString());
        assertEquals(new byte[] { (byte)0xD4, 0x31 }, bn.toByteArray());
    }
    
    public void testStringConstructionNibble() {
        BitNumbers bn = new BitNumbers("942");
        assertFalse(bn.isEmpty());
        assertEquals(16, bn.getMax());
        for(int i = 0; i < 15; i++)
            assertEquals(i == 0 || i == 3 || i == 5 || i == 10, bn.isSet(i));
        assertEquals("942", bn.toHexString());
        assertEquals(new byte[] { (byte)0x94, 0x20 }, bn.toByteArray());
        
        bn.set(1);
        bn.set(11);
        bn.set(15);
        for(int i = 0; i < 16; i++)
            assertEquals(i == 0 || i == 1 || i == 3 || i == 5 || i == 10 || i == 11 || i == 15, bn.isSet(i));
        assertEquals("D431", bn.toHexString());
        assertEquals(new byte[] { (byte)0xD4, 0x31 }, bn.toByteArray());
    }
    
    public void testToHexStringCutsExtra() {
        BitNumbers bn = new BitNumbers("100000");
        assertFalse(bn.isEmpty());
        assertEquals(24, bn.getMax());
        for(int i = 0; i < 32; i++) {
            assertEquals(i == 3, bn.isSet(i));
        }
        assertEquals("1", bn.toHexString());
    }
    
    public void testToByteArrayDoesntCutExtra() {
        BitNumbers bn = new BitNumbers("100000");
        assertFalse(bn.isEmpty());
        assertEquals(24, bn.getMax());
        for(int i = 0; i < 32; i++) {
            assertEquals(i == 3, bn.isSet(i));
        }
        assertEquals(new byte[] { 0x10, 0, 0 }, bn.toByteArray() );
    }
}
