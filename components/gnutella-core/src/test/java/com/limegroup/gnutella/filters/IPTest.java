package com.limegroup.gnutella.filters;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * Unit tests for IP, IPFilter, IPList
 */
public class IPTest extends com.limegroup.gnutella.util.LimeTestCase {
    SpamFilter filter;
    byte[] guid;

    public IPTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IPTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private byte[] bytes(int one, int two, int three, int four) {
        return new byte[] { (byte)one, (byte)two, (byte)three, (byte)four };
    }

    public void testIPListLegacy() {
        IPList iplist = new IPList();

        iplist.add ("255.255.255.255");
        iplist.add ("0.0.0.0");
        iplist.add ("255.255.255.255");
        iplist.add ("255.255.0.255");
        iplist.add ("255.255.255.0");
        iplist.add ("0.255.255.255");
        iplist.add ("255.255.0.0");
        iplist.add ("0.255.0.255");
        iplist.add ("192.168.0.1/255.255.255.0");
        iplist.add ("10.0.*.*");

        assertTrue(iplist.contains (new IP("0.0.0.0")));
        assertTrue(iplist.contains (new IP(bytes(0, 0, 0, 0))));
        assertTrue(iplist.contains (new IP("255.255.255.255")));
        assertTrue(iplist.contains (new IP(bytes(255,255,255,255))));
        assertTrue(iplist.contains (new IP("255.255.255.0")));
        assertTrue(iplist.contains (new IP(bytes(255,255,255,0))));
        assertTrue(!iplist.contains (new IP("255.0.255.255")));
        assertTrue(!iplist.contains (new IP(bytes(255,0,255,255))));
        assertTrue(!iplist.contains (new IP("192.168.1.2/255.255.255.0")));
        assertTrue(iplist.contains (new IP("192.168.0.2")));
        assertTrue(iplist.contains (new IP(bytes(192,168,0,2))));
        assertTrue(iplist.contains (new IP("192.168.0.1")));
        assertTrue(iplist.contains (new IP(bytes(192,168,0,1))));
        assertTrue(!iplist.contains (new IP("192.168.1.1")));
        assertTrue(!iplist.contains (new IP(bytes(192,168,1,1))));
        assertTrue(iplist.contains (new IP("10.0.1.1")));
        assertTrue(iplist.contains (new IP(bytes(10,0,1,1))));
        assertTrue(!iplist.contains (new IP("10.1.0.1")));        
        assertTrue(!iplist.contains (new IP(bytes(10,1,0,1))));
    }

    public void testIPLegacy() {
        IP a=new IP("1.1.1.*");
        IP b=new IP("1.1.1.2");
        IP c=new IP("1.1.2.1");
        IP b1 = new IP(bytes(1,1,1,2));
        IP c1 = new IP(bytes(1,1,2,1));
        assertTrue(a.contains(b));
        assertTrue(! b.contains(a));
        assertTrue(! a.contains(c));
        assertTrue(! c.contains(a));
        assertTrue(b.contains(b1));
        assertTrue(c.contains(c1));
        assertTrue(b1.contains(b));
        assertTrue(c1.contains(c));

        assertNotEquals(a,c);
        assertNotEquals(a,b);
        assertNotEquals(b,a);
        assertNotEquals(b,c);
        assertEquals(a, a);
        assertEquals(b,b);
        assertEquals(b, b1);
        assertEquals(c, c1);
        assertEquals(b.hashCode(), b1.hashCode());
        assertEquals(c.hashCode(), c1.hashCode());
        assertNotEquals("asdf", a);
        IP d=new IP("1.1.1.0/255.255.255.0");
        IP e=new IP("1.1.1.0/24");
        IP f=new IP("1.1.1/24");
        assertEquals(a,d);
        assertEquals(d,a);
        assertEquals(a.hashCode(),d.hashCode());
        assertEquals(a,e);
        assertEquals(e,a);
        assertEquals(a.hashCode(),e.hashCode());
        assertEquals(a,f);
        assertEquals(f,a);
        assertEquals(a.hashCode(),f.hashCode());
        assertEquals(d,e);
        assertEquals(f,e);
        
        try {
            new IP("this should fail.");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("821.1.1.0");
            fail("illegal argument excepted.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1*.1.0");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.256.0.0");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/33");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/-1");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}    
        
        try {
            new IP("1.1..1");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/255.255.256.255");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1234.1.1");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP("1.1.1.1/255.1234.255.255");
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP(new byte[] {0,0,0});
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            new IP(new byte[] {0,0,0,0,0});
            fail("illegal argument expected.");
        } catch(IllegalArgumentException ignored) {}
    }


    public void testIPFilterLegacy() {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"18.239.0.*", "13.0.0.0"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"18.239.0.144"});
        IPFilter filter = RouterService.getIpFilter();
        filter.refreshHosts();
        assertTrue(filter.allow("18.240.0.0"));
        assertTrue(! filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertTrue(! filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
        byte[] address={(byte)18, (byte)239, (byte)0, (byte)144};
        assertTrue(filter.allow(
            (Message)PingReply.createExternal(new byte[16], (byte)3, 6346, address, false)));
        byte[] address2=new byte[] {(byte)18, (byte)239, (byte)0, (byte)143};
        assertTrue(! filter.allow(
            new QueryReply(new byte[16], (byte)3, 6346, address2, 0,
                           new Response[0], new byte[16], false)));
        assertTrue(filter.allow(QueryRequest.createQuery("test", (byte)3)));
        PushRequest push1=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address, 6346);
        assertTrue(filter.allow(push1));
        PushRequest push2=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address2, 6346);
        assertTrue(! filter.allow(push2));
    }

    public void testGetDistanceTo() {
        IP ipRange = new IP("18.194.0.1/16");
        IP ip = new IP("18.194.0.37");
        assertEquals("Distance between an IP and its subnet should be zero.", 0, ipRange.getDistanceTo(ip));
        ip = new IP("128.0.0.1");
        assertLessThan("Expect the high-order bit of the xor distance to be set", 0 , ipRange.getDistanceTo(ip));
        
        ipRange = new IP("0.0.0.0/16");
        ip      = new IP("127.255.255.255"); // = Integer.MAX_VALUE
        assertEquals(0x7FFF0000, ipRange.getDistanceTo(ip));
        
        IP ipRange2 = new IP("10.1.2.3/8");
        assertEquals("Incorrect distance between two ranges", 0x0A000000,ipRange.getDistanceTo(ipRange2));
    }
}

