package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import com.sun.java.util.collections.List;

import junit.framework.Test;

import com.limegroup.gnutella.downloader.TestFile;
import com.limegroup.gnutella.downloader.TestUploader;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.Sockets;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Map;
import com.sun.java.util.collections.Set;

/**
 * Checks whether Meta-Queries are correctly answered, etc.
 */
public class ServerSideMetaQueryTest extends ClientSideTestCase {

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket[] UDP_ACCESS;

    public ServerSideMetaQueryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideMetaQueryTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }      
    
    private static void doSettings() {
        TIMEOUT = 3000;
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;mp3");
        // get the resource file for com/limegroup/gnutella
        File mp3 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/mp3/mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3");
        // now move them to the share dir
        CommonUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
    }   
    
    //////////////////////////////////////////////////////////////////

    public static Integer numUPs() {
        return new Integer(4);
    }
    
    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    public static ActivityCallback getActivityCallback() {
        return new MyCallback();
    }

    public static class MyCallback extends ActivityCallbackStub {
        public GUID aliveGUID = null;

        public void setGUID(GUID guid) { aliveGUID = guid; }
        public void clearGUID() { aliveGUID = null; }

        public boolean isQueryAlive(GUID guid) {
            if (aliveGUID != null)
                return (aliveGUID.equals(guid));
            return false;
        }
    }

    ///////////////////////// Actual Tests ////////////////////////////

    public void testMediaTypeAggregator() throws Exception {
        {
        QueryRequest query = 
            new QueryRequest(GUID.makeGuid(), (byte)3, "whatever", "", null,
                             null, null, true, Message.N_TCP, false, 0, 0);
       
        MediaType.Aggregator filter = MediaType.getAggregator(query);
        assertNull(filter);
        }

        int[] flags = new int[6];
        flags[0] = QueryRequest.AUDIO_MASK;
        flags[1] = QueryRequest.VIDEO_MASK;
        flags[2] = QueryRequest.DOC_MASK;
        flags[3] = QueryRequest.IMAGE_MASK;
        flags[4] = QueryRequest.WIN_PROG_MASK;
        flags[5] = QueryRequest.LIN_PROG_MASK;
        for (int i = 0; i < flags.length; i++) {
            testAggregator(0 | flags[i]);
            for (int j = 0; j < flags.length; j++) {
                if (j == i) continue;
                testAggregator(0 | flags[i] | flags[j]);
                for (int k = 0; k < flags.length; k++) {
                    if (k == j) continue;
                    testAggregator(0 | flags[i] | flags[j] | flags[k]);
                    for (int l = 0; l < flags.length; l++) {
                        if (l == k) continue;
                        testAggregator(0 | flags[i] | flags[j] | flags[k] |
                                       flags[l]);
                        for (int m = 0; m < flags.length; m++) {
                            if (m == l) continue;
                            testAggregator(0 | flags[i] | flags[j] | flags[k] |
                                           flags[l] | flags[m]);
                        }
                    }
                }
            }
        }

    }

    private void testAggregator(int flag) throws Exception {
        QueryRequest query = 
            new QueryRequest(GUID.makeGuid(), (byte)3, "whatever", "", null,
                             null, null, true, Message.N_TCP, false, 0, flag);
       
        MediaType.Aggregator filter = MediaType.getAggregator(query);
        assertNotNull(filter);
        List filterList = (List) PrivilegedAccessor.getValue(filter, 
                                                             "_filters");
        int numFilters = 0;
        if ((flag & QueryRequest.AUDIO_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.VIDEO_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.DOC_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.IMAGE_MASK) > 0) numFilters++;
        if (((flag & QueryRequest.WIN_PROG_MASK) > 0) ||
            ((flag & QueryRequest.LIN_PROG_MASK) > 0)) numFilters++;
        assertEquals(numFilters, filterList.size());
        assertEquals(((flag & QueryRequest.AUDIO_MASK) > 0),
                     filter.allow("susheel.mp3"));
        assertEquals(((flag & QueryRequest.VIDEO_MASK) > 0),
                     filter.allow("susheel.wmv"));
        assertEquals(((flag & QueryRequest.DOC_MASK) > 0),
                     filter.allow("susheel.txt"));
        assertEquals(((flag & QueryRequest.IMAGE_MASK) > 0),
                     filter.allow("susheel.png"));
        assertEquals((((flag & QueryRequest.WIN_PROG_MASK) > 0) ||
                      ((flag & QueryRequest.LIN_PROG_MASK) > 0)),
                     (filter.allow("susheel.exe") || 
                      filter.allow("susheel.bin")));
    }

}
