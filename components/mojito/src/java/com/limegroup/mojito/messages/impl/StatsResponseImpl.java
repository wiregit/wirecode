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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.StatsResponse;


public class StatsResponseImpl extends AbstractResponseMessage
        implements StatsResponse {

    private String statistics;

    public StatsResponseImpl(Context context, 
            Contact contact, KUID messageId, String statistics) {
        super(context, OpCode.STATS_RESPONSE, contact, messageId);

        this.statistics = statistics;
    }

    public StatsResponseImpl(Context context, 
            SocketAddress src, ByteBuffer... data) throws IOException {
        super(context, OpCode.STATS_RESPONSE, src, data);
        
        MessageInputStream in = getMessageInputStream();
        
        byte[] s = in.readStatistics();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(s);
        GZIPInputStream gz = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(s.length * 2);
        
        byte[] b = new byte[2048];
        int len = -1;
        while((len = gz.read(b)) != -1) {
            baos.write(b, 0, len);
        }
        gz.close();
        baos.close();
        
        this.statistics = new String(baos.toByteArray());
    }
    
    public String getStatistics() {
        return statistics;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(baos);
        gz.write(statistics.getBytes());
        gz.close();
        byte[] s = baos.toByteArray();
        
        out.writeStatistics(s);
    }
    
    public String toString() {
        String s = statistics;
        if (s != null) {
            if (s.length() > 128) {
                s = s.substring(0, 128);
            }
        }
        
        return "StatsResponse: " + s;
    }
}
