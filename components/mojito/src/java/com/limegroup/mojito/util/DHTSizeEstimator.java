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

    /** */
    private List<Number> localSizeHistory = new LinkedList<Number>();

    /** */
    private List<Number> remoteSizeHistory = new LinkedList<Number>();

    /** */
    private volatile int estimatedSize = 0;

    /** */
    private volatile long lastEstimateTime = 0L;

    public DHTSizeEstimator() {
    }

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

        if ((System.currentTimeMillis() - lastEstimateTime) >= ContextSettings.ESTIMATE_NETWORK_SIZE_EVERY
                .getValue()) {

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
            remoteSizeHistory.add(new Integer(remoteSize));
            if (remoteSizeHistory.size() >= ContextSettings.MAX_REMOTE_HISTORY_SIZE
                    .getValue()) {
                remoteSizeHistory.remove(0);
            }
        }
    }

    /**
     * Computes and returns the approximate DHT size
     */
    public int computeSize(RouteTable routeTable) {

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

            BigInteger distance = localNodeId.xor(node.getNodeID())
                    .toBigInteger();
            BigInteger j = BigInteger.valueOf(i);

            sum1 = sum1.add(j.multiply(distance));
            sum2 = sum2.add(j.pow(2));
        }

        int estimatedSize = 0;
        if (!sum1.equals(BigInteger.ZERO)) {
            estimatedSize = KUID.MAXIMUM.toBigInteger().multiply(sum2).divide(
                    sum1).intValue();
        }
        estimatedSize = Math.max(1, estimatedSize);

        int localSize = 0;
        synchronized (localSizeHistory) {
            localSizeHistory.add(new Integer(estimatedSize));
            if (localSizeHistory.size() >= ContextSettings.MAX_LOCAL_HISTORY_SIZE
                    .getValue()) {
                localSizeHistory.remove(0);
            }

            int localSizeSum = 0;
            if (!localSizeHistory.isEmpty()) {
                for (Number size : localSizeHistory) {
                    localSizeSum += size.intValue();
                }

                localSize = localSizeSum / localSizeHistory.size();
            }
        }

        int combinedSize = localSize;
        if (ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            synchronized (remoteSizeHistory) {
                if (remoteSizeHistory.size() >= 3) {
                    Number[] remote = remoteSizeHistory.toArray(new Number[0]);
                    Arrays.sort(remote);

                    // Skip the smallest and largest value
                    int count = 1;
                    while (count < remote.length - 1) {
                        combinedSize += remote[count++].intValue();
                    }
                    combinedSize /= count;
                }
            }
        }

        // There's always us!
        return Math.max(1, combinedSize);
    }
}
