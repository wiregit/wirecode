/*
 * Mojito Distributed Hash Tabe (DHT)
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

package com.limegroup.mojito.messages.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.util.ByteBufferUtils;


public class StatsResponseImpl extends AbstractResponseMessage
        implements StatsResponse {

    private String statistics;

    public StatsResponseImpl(int vendor, int version, ContactNode node,
            KUID messageId, String statistics) {
        super(STATS_RESPONSE, vendor, version, node, messageId);

        this.statistics = statistics;
    }

    public StatsResponseImpl(SocketAddress src, ByteBuffer data) throws IOException {
        super(STATS_RESPONSE, src, data);
        
        this.statistics = ByteBufferUtils.getUTFString(data);
    }
    
    public String getStatistics() {
        return statistics;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        byte[] str = statistics.getBytes("UTF-8");
        out.write(str, 0, str.length);
    }
    
    public String toString() {
        return "StatsResponse: " + statistics;
    }
}
