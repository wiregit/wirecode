package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class XorViewTest extends BaseTestCase {
    public XorViewTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(XorViewTest.class);
    }
    
    public void testGet(){
        BitSet bs1 = new BitSet();
        bs1.set(1);
        
        BitSet bs2 = new BitSet();
        bs2.set(2);
        
        BitSet bs3 = new BitSet();
        bs3.set(3);
        
        BitField bf1 = new BitFieldSet(bs1, 5);//bf1: 01000 
        BitField bf2 = new BitFieldSet(bs2, 5);//bf2: 00100 
        BitField bf3 = new BitFieldSet(bs3, 5);//bf3: 00010
               
        XorView xov = new XorView(bf1, bf2, bf3);//xov: 01110
 
        for(int i = 0 ; i < xov.maxSize(); i++){
            switch(i){
            case 1:
            case 2:
            case 3:
                assertTrue(xov.get(i));
                break;
            case 0:
            case 4:
                assertFalse(xov.get(i));
                break;
            default:
                fail("received wrong index: " + i);
            }            
        }

    }

}
