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
import java.security.Signature;
import java.security.SignatureException;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.StatsRequest;

/**
 *
 */
public class StatsRequestImpl extends AbstractRequestMessage
        implements StatsRequest {

    private int request;
    
    private ByteBuffer data;
    
    private int secureStatus = INSECURE;
    
    public StatsRequestImpl(Context context, 
            int vendor, int version,
            ContactNode node, KUID messageId, int request) {
        super(context, STATS_REQUEST, vendor, version, node, messageId);

        this.request = request;
    }

    public StatsRequestImpl(Context context, 
            SocketAddress src, ByteBuffer data) throws IOException {
        super(context, STATS_REQUEST, src, data);
        
        this.request = data.get() & 0xFF;
        
        /*if (isSigned()) {
            data.reset();
            this.data = ByteBuffer.allocate(data.remaining());
            this.data.put(data);
        }*/
    }
    
    public int getRequest() {
        return request;
    }

    public void setSecureStatus(int secureStatus) {
        this.secureStatus = secureStatus;
    }

    public int getSecureStatus() {
        return secureStatus;
    }
    
    public boolean isSigned() {
        return false;
    }
    
    public boolean isSecure() {
        return secureStatus == SECURE;
    }

    public byte[] getSecureSignature() {
        return null;
    }

    public void updateSignatureWithSecuredBytes(Signature signature) 
            throws SignatureException {
        /*data.rewind();
        
        // 0-49
        data.limit(CHECKSUM_START);
        signature.update(data);
        
        // 50-69
        signature.update(EMPTY_CHECKSUM_FIELD);
        
        // 70-n
        data.limit(data.capacity());
        data.position(CHECKSUM_START+EMPTY_CHECKSUM_FIELD.length);
        signature.update(data);*/
    }
    
    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeByte(request);
    }
    
    public String toString() {
        return "StatsRequest: " + request;
    }
}
