/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.limegroup.mojito.util;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.KademliaSettings;

/**
 * An utility class to compute the approximate DHT size.
 * 
 * http://azureus.cvs.sourceforge.net/azureus/azureus2/com/aelitis/azureus/core/dht/control/impl/DHTControlImpl.java
 */
public class DHTSizeEstimator {

    private static final BigInteger MAXIMUM = BigInteger.valueOf(Integer.MAX_VALUE);
    
    /** History of local estimations */
    private List<BigInteger> localSizeHistory = new LinkedList<BigInteger>();

    /** History of remote estimations (sizes we received with pongs) */
    private List<BigInteger> remoteSizeHistory = new LinkedList<BigInteger>();

    /** Current estimated size */
    private volatile int estimatedSize = 0;

    /** The time when we made the last estimation */
    private volatile long lastEstimateTime = 0L;

    public DHTSizeEstimator() {
    }

    /**
     * Clears the history and sets everyting to
     * its initial state
     */
    public void clear() {
        estimatedSize = 0;
        lastEstimateTime = 0L;

        synchronized (localSizeHistory) {
            localSizeHistory.clear();
        }

        synchronized (remoteSizeHistory) {
            remoteSizeHistory.clear();
        }
    }

    /**
     * Returns the approximate DHT size
     */
    public int getEstimatedSize(RouteTable routeTable) {

        if ((System.currentTimeMillis() - lastEstimateTime) 
                >= ContextSettings.ESTIMATE_NETWORK_SIZE_EVERY.getValue()) {

            estimatedSize = computeSize(routeTable);
            lastEstimateTime = System.currentTimeMillis();
        }

        return estimatedSize;
    }

    /**
     * Adds the approximate DHT size as returned by a remote Node.
     * The average of the remote DHT sizes is incorporated into into
     * our local computation.
     */
    public void addEstimatedRemoteSize(int remoteSize) {
        if (remoteSize <= 0 || !ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            return;
        }

        synchronized (remoteSizeHistory) {
            remoteSizeHistory.add(BigInteger.valueOf(remoteSize));
            if (remoteSizeHistory.size() 
                    > ContextSettings.MAX_REMOTE_HISTORY_SIZE.getValue()) {
                remoteSizeHistory.remove(0);
            }
        }
    }

    /**
     * Computes and returns the approximate DHT size
     */
    public int computeSize(RouteTable routeTable) {
        
        assert (Integer.MAX_VALUE == MAXIMUM.intValue());
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();

        // TODO only live nodes?
        KUID localNodeId = routeTable.getLocalNode().getNodeID();
        List<Contact> nodes = routeTable.select(localNodeId, k, false);

        // TODO accoriding to Az code it works only with more than
        // two Nodes
        if (nodes.size() <= 2) {
            // There's always we!
            return Math.max(1, nodes.size());
        }

        // See Azureus DHTControlImpl.estimateDHTSize()
        // Di = localNodeID xor NodeIDi
        // Dc = sum(i * Di) / sum(i * i)
        // Size = 2**160 / Dc

        BigInteger sum1 = BigInteger.ZERO;
        BigInteger sum2 = BigInteger.ZERO;

        for (int i = 1; i < nodes.size(); i++) {
            Contact node = nodes.get(i);

            BigInteger distance = localNodeId.xor(node.getNodeID()).toBigInteger();
            BigInteger j = BigInteger.valueOf(i);

            sum1 = sum1.add(j.multiply(distance));
            sum2 = sum2.add(j.pow(2));
        }

        BigInteger estimatedSize = BigInteger.ZERO;
        if (!sum1.equals(BigInteger.ZERO)) {
            estimatedSize = KUID.MAXIMUM.toBigInteger().multiply(sum2).divide(sum1);
            estimatedSize = limit(estimatedSize);
        }

        // And there is always us!
        estimatedSize = BigInteger.ONE.max(estimatedSize);
        
        BigInteger localSize = BigInteger.ZERO;
        synchronized (localSizeHistory) {
            localSizeHistory.add(estimatedSize);
            if (localSizeHistory.size() 
                    > ContextSettings.MAX_LOCAL_HISTORY_SIZE.getValue()) {
                localSizeHistory.remove(0);
            }

            if (!localSizeHistory.isEmpty()) {
                BigInteger localSizeSum = BigInteger.ZERO;
                for (BigInteger size : localSizeHistory) {
                    localSizeSum = localSizeSum.add(size);
                }

                localSize = localSizeSum.divide(BigInteger.valueOf(localSizeHistory.size()));
            }
        }

        BigInteger combinedSize = localSize;
        if (ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            synchronized (remoteSizeHistory) {
                if (remoteSizeHistory.size() >= 3) {
                    BigInteger[] remote = remoteSizeHistory.toArray(new BigInteger[0]);
                    Arrays.sort(remote);
                    
                    // Skip the smallest and largest value
                    int count = 1;
                    while (count < (remote.length - 1)) {
                        combinedSize = combinedSize.add(remote[count++]);
                    }
                    combinedSize = combinedSize.divide(BigInteger.valueOf(count));
                    combinedSize = limit(combinedSize);
                }
            }
        }

        // There is always us!
        return BigInteger.ONE.max(combinedSize).intValue();
    }
    
    private static BigInteger limit(BigInteger value) {
        // Wow! We estimate there are more than MAXIMUM number
        // of Nodes in the DHT which is a lot. The technical max
        // is 2**160 but as we're not using 160bit values for
        // the estimated size we have to cap the estimated size
        // at the MAXIMUM number.
        return value.min(MAXIMUM);
    }
}
