package com.limegroup.gnutella.tests.requery;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.tests.*;
import java.util.*;

public class RequeryMessageRouter extends MessageRouterStub {

    private long lastQRReceivedTime = 0;
    private final long BETWEEN_TIME = DownloadManager.TIME_BETWEEN_REQUERIES;
    
    private List queryStrings = new ArrayList();

    public void broadcastQueryRequest( QueryRequest qr) {
        // 1. make sure you didn't get the qr too fast....
        if (lastQRReceivedTime == 0)
            ;
        else if ((System.currentTimeMillis() - lastQRReceivedTime) <
                 BETWEEN_TIME) 
            Assert.that(false, "GOT QR TOO FAST");

        lastQRReceivedTime = System.currentTimeMillis();

        // 2. make sure you get 1 of each....
        if (queryStrings.contains(qr.getQuery()))
            Assert.that(false, "GOT DUPLICATE QR");
        else
            queryStrings.add(qr.getQuery());
        if (queryStrings.size() == 3) {
            System.out.println("passed...");
            System.exit(0);
        }
    }
    
}
