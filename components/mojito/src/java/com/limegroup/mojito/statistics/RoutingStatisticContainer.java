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

public class RoutingStatisticContainer extends StatisticContainer {

    /**
     * <tt>Statistic</tt> for the total number of contacts added to the main
     * routing table
     */
    public Statistic NODE_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the total number of LIVE contacts added to the
     * main routing table
     */
    public Statistic LIVE_NODE_COUNT = new NodeCountStatistic();

    /**
     * <tt>Statistic</tt> for the total number of UNKNOWN contacts added to
     * the main routing table
     */
    public Statistic UNKNOWN_NODE_COUNT = new NodeCountStatistic();

    /**
     * <tt>Statistic</tt> for the number of contacts added to the main routing
     * table
     */
    public Statistic BUCKET_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of bucket refreshes
     */
    public Statistic BUCKET_REFRESH_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of replacement contacts added
     */
    public Statistic REPLACEMENT_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of dead contacts
     */
    public Statistic DEAD_NODE_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for contacts trying to spoof node ids
     */
    public Statistic SPOOF_COUNT = new SimpleStatistic();
    
    public RoutingStatisticContainer(KUID nodeId) {
        super(nodeId);
    }
    
    public void writeStats(Writer writer) throws IOException {
        writer.write("Routing Stats:\n");
        super.writeStats(writer);
    }

    protected class NodeCountStatistic extends SimpleStatistic{

        public void incrementStat() {
            super.incrementStat();
            NODE_COUNT.incrementStat();
        }
    }
}
