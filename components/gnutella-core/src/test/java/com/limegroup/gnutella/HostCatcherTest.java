package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.tests.stubs.ActivityCallbackStub;

public class HostCatcherTest extends TestCase {  
    private HostCatcher hc;

    public HostCatcherTest(String name) {
        super(name);
    }

    /** Returns a new HostCatcher connected to stubs.  YOU MAY WANT TO CALL
     *  EXPIRE to force bootstrap pongs. */
    public void setUp() {
        //This creates an acceptor thread.  We should probably use an Acceptor
        //stub or write a tearDown() method.
        hc=new HostCatcher(new ActivityCallbackStub());
        hc.initialize(new Acceptor(6346, null),
                      new ConnectionManager(null, null));
    }

    public void testAdd() {
        //Requires private data
        //System.out.println("-Testing add method");

        //Endpoints.
        setUp();
        hc.add(new Endpoint("192.168.0.1"), false);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==1);

        setUp();
        hc.add(new Endpoint("18.239.0.1"), false);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==1);
        assertTrue(hc.getNumPrivateHosts()==0);

        setUp();
        hc.add(new Endpoint("18.239.0.1"), true);
        assertTrue(hc.getNumUltrapeerHosts()==1);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==0);

        //PingReply's.
        setUp();
        hc.add(new PingReply(new byte[16], (byte)3, 6346, 
                             new byte[] {(byte)192,(byte)168,(byte)0,(byte)1},
                             0, 0),
               null);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==1);

        setUp();
        hc.add(new PingReply(new byte[16], (byte)3, 6346, 
                             new byte[] {(byte)18,(byte)239,(byte)0,(byte)1},
                             0, 0),
               null);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==1);
        assertTrue(hc.getNumPrivateHosts()==0);


        setUp();
        hc.add(new PingReply(new byte[16], (byte)3, 6346, 
                             new byte[] {(byte)18,(byte)239,(byte)0,(byte)1},
                             0, 0, true),
               null);
        assertTrue(hc.getNumUltrapeerHosts()==1);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==0);
    }

    public void testBootstraps() {
        try {
            //TODO: factor the new HostCatcher code into a setUp() method.
            //System.out.println("-Testing bootstrap servers");
            SettingsManager.instance().setQuickConnectHosts(
                new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347"});
            hc.expire();

            hc.add(new Endpoint("128.103.60.3", 6346), false);
            hc.add(new Endpoint("128.103.60.2", 6346), false);
            hc.add(new Endpoint("128.103.60.1", 6346), false);

            Endpoint router1=hc.getAnEndpoint();
            assertTrue(router1.equals(new Endpoint("r1.b.c.d", 6346)));         
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.1", 6346)));
            hc.add(new Endpoint("18.239.0.144", 6346), true);
            hc.doneWithEndpoint(router1);    //got pong
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.144", 6346)));        

            Endpoint router2=hc.getAnEndpoint();
            assertTrue(router2.equals(new Endpoint("r2.b.c.d", 6347)));        
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.2", 6346)));        
            hc.doneWithEndpoint(router2);    //did't get any pongs

            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.3", 6346))); //no more bootstraps
        } catch (InterruptedException e) {
            assertTrue("Mysterious InterruptedException", false);
        }
    }

    public void testExpire() {
        try {
            //System.out.println("-Testing expire");
            SettingsManager.instance().setQuickConnectHosts(
                 new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347"});
            hc.expire();

            assertTrue(hc.getAnEndpoint().equals(new Endpoint("r1.b.c.d", 6346)));

            hc.add(new Endpoint("18.239.0.144", 6346), true);
            hc.add(new Endpoint("128.103.60.3", 6346), false);
            hc.add(new Endpoint("192.168.0.1", 6346), false);
            assertTrue(hc.getNumUltrapeerHosts()==1);

            hc.expire();
            assertTrue(hc.getNumUltrapeerHosts()==0);
            Endpoint e=hc.getAnEndpoint();
            assertTrue(e.equals(new Endpoint("r1.b.c.d", 6346)));
            hc.doneWithEndpoint(e);
            e=hc.getAnEndpoint();
            assertTrue(e.equals(new Endpoint("r2.b.c.d", 6347)));
            hc.doneWithEndpoint(e);
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.144", 6346)));
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.3", 6346)));
        } catch (InterruptedException e) { 
            assertTrue("Mysterious InterruptedException", false);
        }
    }

    public void testIterators() {
        //System.out.println("-Testing iterators");

        Iterator iter=hc.getNormalHosts(10);
        assertTrue(! iter.hasNext());
        iter=hc.getUltrapeerHosts(10);
        assertTrue(! iter.hasNext());

        assertTrue(hc.getNumUltrapeerHosts()==0);
        hc.add(new Endpoint("18.239.0.1", 6346), true);
        assertTrue(hc.getNumUltrapeerHosts()==1);
        hc.add(new Endpoint("18.239.0.2", 6346), true);
        hc.add(new Endpoint("128.103.60.1", 6346), false);
        hc.add(new Endpoint("128.103.60.2", 6346), false);
        assertTrue(hc.getNumUltrapeerHosts()==2);

        iter=hc.getUltrapeerHosts(100);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("18.239.0.2", 6346)));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("18.239.0.1", 6346)));
        assertTrue(! iter.hasNext());

        iter=hc.getUltrapeerHosts(1);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("18.239.0.2", 6346)));
        assertTrue(! iter.hasNext());

        iter=hc.getNormalHosts(100);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("128.103.60.2", 6346)));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("128.103.60.1", 6346)));
        assertTrue(! iter.hasNext());

        iter=hc.getNormalHosts(1);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("128.103.60.2", 6346)));
        assertTrue(! iter.hasNext());
    }

    public void testPermanent() {
        //System.out.println("-Testing write of permanent nodes to Gnutella.net");
        try {
            //1. Create HC, add entries, write to disk.
            hc.add(new Endpoint("18.239.0.141", 6341), false);//default time=486
            hc.addPermanent(new Endpoint("18.239.0.142", 6342), 1000);
            hc.addPermanent(new Endpoint("18.239.0.143", 6343), 30);
            File tmp=File.createTempFile("hc_test", ".net" );
            hc.write(tmp.getAbsolutePath());

            //2. read HC from file.
            SettingsManager.instance().setQuickConnectHosts(new String[0]);
            setUp();
            hc.read(tmp.getAbsolutePath());
            assertTrue(hc.getNumUltrapeerHosts()==0);
            assertTrue("Got: "+hc.getNumHosts(), hc.getNumHosts()==3);
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.142", 6342)));
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.141", 6341)));
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.143", 6343)));

            //Cleanup.
            tmp.delete();
        } catch (IOException e) {
            assertTrue("Unexpected IO problem: "+e, false);
        } catch (InterruptedException e) {
            assertTrue("Unexpected InterruptedException "+e, false);
        }
    }

    public void testBadGnutellaDotNet() {
        //System.out.println("-Testing bad Gnutella.net");
        try {
            //1. Write (mostly) corrupt file
            File tmp=File.createTempFile("hc_test", ".net" );
            FileWriter out=new FileWriter(tmp);
            out.write("18.239.0.141\n");                  //GOOD: port optional
            out.write("\n");                              //blank line
            out.write("18.239.0.144:A\n");                //bad port
            out.write("18.239.0.141:6347 A\n");           //bad uptime
            out.write("18.239.0.142:6342  1000 ignore\n");//GOOD: ignore extra
            out.flush();
            out.close();

            //2. Read and verify
            SettingsManager.instance().setQuickConnectHosts(new String[0]);
            setUp();
            hc.read(tmp.getAbsolutePath());
            assertTrue(hc.getAnEndpoint().equals( 
                new Endpoint("18.239.0.142", 6342)));
            assertTrue(hc.getAnEndpoint().equals( 
                new Endpoint("18.239.0.141", 6346)));
            assertTrue(hc.getNumHosts()==0);
            assertTrue(hc.getNumUltrapeerHosts()==0);

            //Clean up
            tmp.delete();
        } catch (IOException e) { 
            assertTrue("Unexpected IO problem", false);
        } catch (InterruptedException e) {
            assertTrue("Unexpected InterruptedException "+e, false);
        }
    }
}
