package com.limegroup.gnutella.messages;

import junit.framework.*;
import java.io.*;

public class VendorMessageTest extends TestCase {
    public VendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(VendorMessageTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testCreationConstructor() {
        VendorMessage vm = null;
        byte[] payload = null;
        byte[] vendorID = null;
        try {
            //test messed up vendor ID
            vendorID = new byte[5];
            vm = new VendorMessage(vendorID, 1, 1, new byte[0]);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {
        }
        try {
            // test bad selector
            vm = new VendorMessage(new byte[4], 0x10000000, 1, new byte[0]);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {
        }
        try {
            // test bad version
            vm = new VendorMessage(vendorID, 1, 0x00020101, new byte[0]);
            assertTrue(false);
        }
        catch (IllegalArgumentException expected) {
        }
        try {
            // test bad payload
            vm = new VendorMessage(new byte[4], 1, 1, null);
            assertTrue(false);
        }
        catch (NullPointerException expected) {
        }
        catch (IllegalArgumentException why) {
            assertTrue(false);
        }
    }

    
    public void testWriteAndRead() {
        try {
            VendorMessage vm = new VendorMessage("LIME".getBytes(),
                                                 0x31, 1, new byte[0]);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            assertTrue(vm.getVendorID().equals("LIME"));
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            VendorMessage vmRead = (VendorMessage) Message.read(bais);
            assertTrue(vm.getVendorID().equals(vmRead.getVendorID()));
        }
        catch (Exception crap) {
            crap.printStackTrace();
            assertTrue(false);
        }
    }

}
