package com.limegroup.gnutella.messages;

import junit.framework.*;
import java.io.*;
import com.limegroup.gnutella.ByteOrder;

/** Tests the important MessagesSupportedVMP.
 */
public class MessagesSupportedVMPTest extends TestCase {
    public MessagesSupportedVMPTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MessagesSupportedVMPTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testStaticConstructor() {
        MessagesSupportedVMP vmp = MessagesSupportedVMP.instance();
        assertTrue(vmp.supportsTCPConnectBack() > -1);
        assertTrue(vmp.supportsUDPConnectBack() > -1);
        
        VendorMessage vm = vmp.getVendorMessage();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            vm = (VendorMessage) Message.read(bais);
            MessagesSupportedVMP vmpRead = 
                (MessagesSupportedVMP) vm.getVendorMessagePayload();
            assertTrue(vmp.equals(vmpRead));
            assertTrue(vmpRead.supportsTCPConnectBack() > -1);
            assertTrue(vmpRead.supportsUDPConnectBack() > -1);
            assertTrue(vmpRead.supportsHopsFlow() > -1);
        }
        catch (Exception noway) {
            assertTrue(false);
        }
    }

    public void testNetworkConstructor() {
        MessagesSupportedVMP.SupportedMessageBlock smp1 = 
            new MessagesSupportedVMP.SupportedMessageBlock("SUSH".getBytes(),
                                                            10, 10);
        MessagesSupportedVMP.SupportedMessageBlock smp2 = 
            new MessagesSupportedVMP.SupportedMessageBlock("NEIL".getBytes(), 
                                                           5, 5);
        MessagesSupportedVMP.SupportedMessageBlock smp3 = 
            new MessagesSupportedVMP.SupportedMessageBlock("DAWG".getBytes(), 
                                                           3, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteOrder.short2leb((short)4, baos);
            baos.write(smp1.encode());
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp3.encode());
            VendorMessage vm = new VendorMessage(new byte[4], 0, 0,
                                                 baos.toByteArray());
            baos = new ByteArrayOutputStream();
            vm.write(baos);
            ByteArrayInputStream bais = 
                new ByteArrayInputStream(baos.toByteArray());
            vm = (VendorMessage) Message.read(bais);
            MessagesSupportedVMP vmp = 
                (MessagesSupportedVMP) vm.getVendorMessagePayload();
            // make sure it supports everything we expect....
            assertTrue(vmp.supportsMessage("SUSH".getBytes(), 10) == 10);
            assertTrue(vmp.supportsMessage("NEIL".getBytes(), 5) == 5);
            assertTrue(vmp.supportsMessage("DAWG".getBytes(), 3) == 3);
            assertTrue(vmp.supportsTCPConnectBack() == -1);
            assertTrue(vmp.supportsUDPConnectBack() == -1);
            assertTrue(vmp.supportsHopsFlow() == -1);

            // now creat another one, mix up the blocks that are supported, and
            // make sure they are equal....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)3, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            
            MessagesSupportedVMP vmpOther = 
                new MessagesSupportedVMP(0, baos.toByteArray());

            assertTrue(vmp.equals(vmpOther));
        }
        catch (IOException noway) {
            noway.printStackTrace();
            assertTrue(false);
        }
        catch (BadPacketException noway) {
            noway.printStackTrace();
            assertTrue(false);
        }

    }


    public void testBadCases() {
        MessagesSupportedVMP.SupportedMessageBlock smp1 = 
            new MessagesSupportedVMP.SupportedMessageBlock("SUSH".getBytes(),
                                                            10, 10);
        MessagesSupportedVMP.SupportedMessageBlock smp2 = 
            new MessagesSupportedVMP.SupportedMessageBlock("NEIL".getBytes(), 
                                                           5, 5);
        MessagesSupportedVMP.SupportedMessageBlock smp3 = 
            new MessagesSupportedVMP.SupportedMessageBlock("DAWG".getBytes(), 
                                                           3, 3);
        ByteArrayOutputStream baos = null;
        try {
            // test missing info....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)4, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            MessagesSupportedVMP vmpOther = 
                new MessagesSupportedVMP(0, baos.toByteArray());
            assertTrue(false);
        }
        catch (IOException noway) {
            noway.printStackTrace();
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
        try {
            // test corrupt info....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)4, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            baos.write("crap".getBytes());
            MessagesSupportedVMP vmpOther = 
                new MessagesSupportedVMP(0, baos.toByteArray());
            assertTrue(false);
        }
        catch (IOException noway) {
            noway.printStackTrace();
            assertTrue(false);
        }
        catch (BadPacketException expected) {
        }
        try {
            // test semantics....
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)0, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            MessagesSupportedVMP vmpOther = 
                new MessagesSupportedVMP(0, baos.toByteArray());
            baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)3, baos);
            baos.write(smp2.encode());
            baos.write(smp3.encode());
            baos.write(smp1.encode());
            MessagesSupportedVMP vmpOneOther = 
                new MessagesSupportedVMP(0, baos.toByteArray());
            assertTrue(!vmpOther.equals(vmpOneOther));
        }
        catch (IOException noway) {
            noway.printStackTrace();
            assertTrue(false);
        }
        catch (BadPacketException noway) {
            noway.printStackTrace();
            assertTrue(false);
        }

    }


}
