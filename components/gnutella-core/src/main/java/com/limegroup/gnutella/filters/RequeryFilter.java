package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.util.Locale;

/** 
 * Blocks over-zealous automated requeries.
 */
public class RequeryFilter extends SpamFilter {
    public boolean allow(Message m) {
        if (m instanceof QueryRequest)
            return allow((QueryRequest)m);
        else
            return true;        
    }

    private boolean allow(QueryRequest q) {
        //Kill automated requeries from LW 2.3 and earlier.
        byte[] guid=q.getGUID();
        if (GUID.isLimeGUID(guid)) {
            if (GUID.isLimeRequeryGUID(guid, 0)             //LW 2.2.0-2.2.3
                    || GUID.isLimeRequeryGUID(guid, 1)) {   //LW 2.2.4-2.3.x
                return false;
            }
        }
        return true;
    }

    public static void main(String args[]) {
        SettingsManager.instance().setFilterDuplicates(false);
        SettingsManager.instance().setFilterGreedyQueries(false);
        SpamFilter filter=SpamFilter.newRouteFilter();
        Assert.that(filter.allow(new PingRequest((byte)3)));
        Assert.that(filter.allow(new QueryRequest((byte)3, 0, "Hello")));
        Assert.that(filter.allow(new QueryRequest((byte)3, 0, "Hello")));
        Assert.that(filter.allow(new QueryRequest((byte)3, 0, "Hel lo", true)));
        Assert.that(filter.allow(new QueryRequest((byte)3, 0, "asd", false)));
 
        byte[] guid=GUID.makeGuid();   //version 1
        guid[0]=(byte)0x02;
        guid[1]=(byte)0x01;
        guid[2]=(byte)0x17;
        guid[3]=(byte)0x05;
        guid[13]=(byte)0x2E;
        guid[14]=(byte)0x05;
        Assert.that(GUID.isLimeGUID(guid));
        Assert.that(GUID.isLimeRequeryGUID(guid, 1));
        Assert.that(! GUID.isLimeRequeryGUID(guid, 0));
        Assert.that(! filter.allow(new QueryRequest(guid, (byte)5, 0, "asdf")));
    }
}
