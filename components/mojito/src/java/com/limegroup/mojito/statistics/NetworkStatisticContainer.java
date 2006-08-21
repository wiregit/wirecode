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
 
package com.limegroup.mojito.statistics;

import java.io.IOException;
import java.io.Writer;

import com.limegroup.mojito.KUID;

public class NetworkStatisticContainer extends StatisticContainer {

    public NetworkStatisticContainer(KUID nodeId) {
        super(nodeId);
    }
    
    public void writeStats(Writer writer) throws IOException {
        writer.write("Network Stats:\n");
        super.writeStats(writer);
    }
    
    /**
     * <tt>Statistic</tt> for all outgoing messages
     */
    public Statistic SENT_MESSAGES_COUNT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the size of all outgoing messages
     */
    public Statistic SENT_MESSAGES_SIZE = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for all incoming messages
     */
    public Statistic RECEIVED_MESSAGES_COUNT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the size of all incoming messages
     */
    public Statistic RECEIVED_MESSAGES_SIZE = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for late coming responses
     */
    public Statistic LATE_MESSAGES_COUNT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for sent PING messages
     */
    public Statistic PINGS_SENT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for incoming PONG messages
     */
    public Statistic PINGS_OK = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for failed PING messages
     */
    public Statistic PINGS_FAILED = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for received PING messages
     */
    public Statistic PING_REQUESTS = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for received StatsRequest messages
     */
    public Statistic STATS_REQUEST = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for sent PONG messages
     */
    public Statistic PONGS_SENT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for sent signed PONG messages
     */
    public Statistic SIGNED_PONGS_SENT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for filtered messages
     */
    public Statistic FILTERED_MESSAGES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for receipts timeouts
     */
    public Statistic RECEIPTS_TIMEOUT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for evicted receipts
     */
    public Statistic RECEIPTS_EVICTED = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for bootstrap time
     */
    public Statistic BOOTSTRAP_TIME = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for bootstrap ping failures
     */
    public Statistic BOOTSTRAP_PING_FAILURES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the estimated network stats
     */
    public Statistic ESTIMATE_SIZE = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for STORE_REQUESTS
     */
    public Statistic STORE_REQUESTS = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for STORE_REQUESTS without a query key
     */
    public Statistic STORE_REQUESTS_NO_QK = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for STORE_REQUESTS without a bad query key
     */
    public Statistic STORE_REQUESTS_BAD_QK = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for STORE_REQUESTS without a bad query key
     */
    public Statistic STORE_REQUESTS_OK = new SizeStatistic();
    
    /**
     * <tt>Statistic</tt> for STORE_REQUESTS without a bad query key
     */
    public Statistic STORE_REQUESTS_FAILURE = new SizeStatistic();
    

    public Statistic FIND_VALUE_REQUESTS_OK = new SimpleStatistic();
    
    public Statistic FIND_VALUE_REQUESTS_FAILURE = new SimpleStatistic();
    
    public Statistic LOOKUP_REQUESTS = new SimpleStatistic();
    
        
    protected static class SizeStatistic extends AbstractStatistic{
        public void addData(int data) {
            super.addData(data);
            super.storeCurrentStat();
        }
    }
}
