package com.limegroup.gnutella.filters;

import junit.framework.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.*;

/**
 * Unit tests for IP, IPFilter, IPList
 */
public class IPTest extends com.limegroup.gnutella.util.BaseTestCase {
    SpamFilter filter;
    byte[] guid;

    public IPTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IPTest.class);
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

        assertTrue(iplist.contains ("0.0.0.0"));
        assertTrue(iplist.contains ("255.255.255.255"));
        assertTrue(iplist.contains ("255.255.255.0"));
        assertTrue(!iplist.contains ("255.0.255.255"));
        assertTrue(!iplist.contains ("192.168.1.2/255.255.255.0"));
        assertTrue(iplist.contains ("192.168.0.2"));
        assertTrue(iplist.contains ("192.168.0.1"));
        assertTrue(!iplist.contains ("192.168.1.1"));
        assertTrue(iplist.contains ("10.0.1.1"));
        assertTrue(!iplist.contains ("10.1.0.1"));
    }

    public void testIPLegacy() {
        IP a=new IP("1.1.1.*");
        IP b=new IP("1.1.1.2");
        IP c=new IP("1.1.2.1");
        assertTrue(a.contains(b));
        assertTrue(! b.contains(a));
        assertTrue(! a.contains(c));
        assertTrue(! c.contains(a));

        assertTrue(! a.equals(c));
        assertTrue(! a.equals(b));
        assertTrue(! b.equals(a));
        assertTrue(! b.equals(c));
        assertTrue(a.equals(a));
        assertTrue(b.equals(b));
        assertTrue(! a.equals("asdf"));
        IP d=new IP("1.1.1.0/255.255.255.0");
        IP e=new IP("1.1.1.0/24");
        IP f=new IP("1.1.1/24");
        assertTrue(a.equals(d));
        assertTrue(d.equals(a));
        assertTrue(a.hashCode()==d.hashCode());
        assertTrue(a.equals(e));
        assertTrue(e.equals(a));
        assertTrue(a.hashCode()==e.hashCode());
        assertTrue(a.equals(f));
        assertTrue(f.equals(a));
        assertTrue(a.hashCode()==f.hashCode());
        assertTrue(d.equals(e));
        assertTrue(f.equals(e));
    }


    public void testIPFilterLegacy() {
        SettingsManager.instance().setBannedIps(
            new String[] {"18.239.0.*", "13.0.0.0"});
        SettingsManager.instance().setAllowedIps(new String[] {"18.239.0.144"});
        IPFilter filter = new IPFilter();
        assertTrue(filter.allow("18.240.0.0"));
        assertTrue(! filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertTrue(! filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
        byte[] address={(byte)18, (byte)239, (byte)0, (byte)144};
        assertTrue(filter.allow(
            new PingReply(new byte[16], (byte)3, 6346, address, 0l, 0l)));
        byte[] address2=new byte[] {(byte)18, (byte)239, (byte)0, (byte)143};
        assertTrue(! filter.allow(
            new QueryReply(new byte[16], (byte)3, 6346, address2, 0,
                           new Response[0], new byte[16], false)));
        assertTrue(filter.allow(new QueryRequest((byte)3, 0, "test", false)));
        PushRequest push1=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address, 6346);
        assertTrue(filter.allow(push1));
        PushRequest push2=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address2, 6346);
        assertTrue(! filter.allow(push2));
    }

}

