/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.kapsi.net.kademlia.ContactNode;

public final class BucketUtils {
    
    private BucketUtils() {}
    
    public static ContactNode getLastSeen(List bucketList) {
        return (ContactNode)bucketList.get(0);
    }
    
    public static ContactNode getLeastRecentlySeen(List bucketList) {
        return (ContactNode)bucketList.get(bucketList.size()-1);
    }
    
    public static List sort(List bucketList) {
        Collections.sort(bucketList, new Comparator() {
            public int compare(Object a, Object b) {
                long t1 = ((ContactNode)a).getTimeStamp();
                long t2 = ((ContactNode)b).getTimeStamp();
                if (t1 == t2) {
                    return 0;
                } else if (t1 < t2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return bucketList;
    }
}
