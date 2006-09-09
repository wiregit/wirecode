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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private static final Log LOG = LogFactory.getLog(DHTSizeEstimator.class);
    
    private static final BigInteger MAXIMUM = KUID.MAXIMUM.toBigInteger();
    
    /** History of local estimations */
    private List<BigInteger> localSizeHistory = new LinkedList<BigInteger>();

    /** History of remote estimations (sizes we received with pongs) */
    private List<BigInteger> remoteSizeHistory = new LinkedList<BigInteger>();

    /** Current estimated size */
    private volatile BigInteger estimatedSize = BigInteger.ZERO;

    /** The time when we made the last estimation */
    private volatile long lastEstimateTime = 0L;

    private RouteTable routeTable;
    
    public DHTSizeEstimator(RouteTable routeTable) {
        this.routeTable = routeTable;
    }

    /**
     * Clears the history and sets everyting to
     * its initial state
     */
    public void clear() {
        estimatedSize = BigInteger.ZERO;
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
    public BigInteger getEstimatedSize() {

        if ((System.currentTimeMillis() - lastEstimateTime) 
                >= ContextSettings.ESTIMATE_NETWORK_SIZE_EVERY.getValue()) {

            estimatedSize = computeSize();
            lastEstimateTime = System.currentTimeMillis();
        }

        return estimatedSize;
    }

    /**
     * Adds the approximate DHT size as returned by a remote Node.
     * The average of the remote DHT sizes is incorporated into into
     * our local computation.
     */
    public void addEstimatedRemoteSize(BigInteger remoteSize) {
        if (!ContextSettings.COUNT_REMOTE_SIZE.getValue()) {
            return;
        }
        
        if (remoteSize.compareTo(BigInteger.ZERO) == 0) {
            return;
        }
        
        if (remoteSize.compareTo(BigInteger.ZERO) < 0
                || remoteSize.compareTo(MAXIMUM) > 0) {
            if (LOG.isErrorEnabled()) {
                LOG.error(remoteSize + " is an illegal argument");
            }
            return;
        }
        
        synchronized (remoteSizeHistory) {
            remoteSizeHistory.add(remoteSize);
            if (remoteSizeHistory.size() 
                    > ContextSettings.MAX_REMOTE_HISTORY_SIZE.getValue()) {
                remoteSizeHistory.remove(0);
            }
        }
    }

    /**
     * Computes and returns the approximate DHT size
     */
    public BigInteger computeSize() {
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();

        // TODO only live nodes?
        KUID localNodeId = routeTable.getLocalNode().getNodeID();
        List<Contact> nodes = routeTable.select(localNodeId, k, false);

        // TODO accoriding to Az code it works only with more than
        // two Nodes
        if (nodes.size() <= 2) {
            // There's always we!
            return BigInteger.ONE.max(BigInteger.valueOf(nodes.size()));
        }

        // See Azureus DHTControlImpl.estimateDHTSize()
        // Di = localNodeID xor NodeIDi
        // Dc = sum(i * Di) / sum(i * i)
        // Size = 2**160 / Dc

        BigInteger sum1 = BigInteger.ZERO;
        BigInteger sum2 = BigInteger.ZERO;
        
        // We start 1 because the local Node is the 0th item!
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
        }

        // And there is always us!
        estimatedSize = BigInteger.ONE.max(estimatedSize);
        
        // Get the average of the local estimations
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
        
        // Get the combined average
        // S = (localEstimation + sum(remoteEstimation[i]))/count
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
                    
                    // Make sure we didn't exceed the MAXIMUM number as
                    // we made an addition with the local estimation which
                    // might be already 2**160 bit!
                    combinedSize = combinedSize.min(MAXIMUM);
                }
            }
        }

        // There is always us!
        return BigInteger.ONE.max(combinedSize);
    }
}
