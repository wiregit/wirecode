package com.limegroup.gnutella.tests.requery;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tests.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.gui.search.*;
import com.limegroup.gnutella.tests.downloader.*;



public class RequeryTester {
    
//      static TestUploader uploader1=new TestUploader("6346", 6346);
//      static TestUploader uploader2=new TestUploader("6347", 6347);
    //    static TestUploader uploader3=new TestUploader("6348", 6348);
    static final DownloadManager dm = new DownloadManager();
    

    public static void main (String[] args) {
        SettingsManager.instance().setConnectionSpeed(1000);

        RouterService rs=new RouterService(null, null, null, null);
        ActivityCallback acs = new ActivityCallbackStub();
        dm.initialize(acs, new RequeryMessageRouter(), 
                      new Acceptor(1627,acs), new FileManagerStub());

        SimpleTimer timer = new SimpleTimer(true);
        Runnable click = new Runnable() {
                public void run() {
                    dm.measureBandwidth();
                }
            };
        timer.schedule(click,0,SupernodeAssigner.TIMER_DELAY);
        
        testRequery();
    }

    private static void testRequery() {
        //uploader1.stopAfter(1);//uploader1 will stop after sending 1 byte
        //uploader2.stopAfter(1);
        //uploader3.stopAfter(1);

        RemoteFileDesc rfd1 = newRFD(6346,100, "b.txt");
        RemoteFileDesc rfd2 = newRFD(6347,100, "c.txt");
        RemoteFileDesc rfd3 = newRFD(6348,100, "d.txt");
        
        RemoteFileDesc[] rfds1 = {rfd1};
        RemoteFileDesc[] rfds2 = {rfd2};
        RemoteFileDesc[] rfds3 = {rfd3};

        boolean oneFailed = tryDownload(rfds1);        
        boolean twoFailed = tryDownload(rfds2);
        boolean threeFailed = tryDownload(rfds3);
        
        Assert.that(!oneFailed, "download 1FAILED");
        Assert.that(!twoFailed, "download 2 FAILED ");
        Assert.that(!threeFailed, "download 3 FAILED");
        
        while(true){
            try {
                Thread.sleep(10000);
            }catch(InterruptedException e){}
        }
        
    }


    private static RemoteFileDesc newRFD(int port, int speed, String name) {
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, name,
                                  TestFile.length(), new byte[16],
                                  speed, false, 4);
    }


    /**
     * returns true if the download FAILS
     */
    private static boolean tryDownload(RemoteFileDesc[] rfds) {
        Downloader download=null;
        try {
            download=dm.getFiles(rfds, false);
        } catch (FileExistsException e) {
            return true;
        } catch (AlreadyDownloadingException e) {
            return true;
        } catch (java.io.FileNotFoundException e) {
            return true;
        }
        return false;
    }

}
