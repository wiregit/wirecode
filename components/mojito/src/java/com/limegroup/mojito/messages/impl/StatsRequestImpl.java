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
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.util.ByteBufferUtils;

/**
 *
 */
public class StatsRequestImpl extends AbstractRequestMessage
        implements StatsRequest {

    private int request;
    
    private ByteBuffer data;
    private byte[] signature;
    
    private int secureStatus = INSECURE;
    
    public StatsRequestImpl(int vendor, int version,
            ContactNode node, KUID messageId, int request) {
        super(STATS_REQUEST, vendor, version, node, messageId);

        this.request = request;
    }

    public StatsRequestImpl(SocketAddress src, ByteBuffer data) throws IOException {
        super(STATS_REQUEST, src, data);
        
        this.data = data;
        
        this.request = data.get() & 0xFF;
        
        if (data.remaining() >= 20) {
            signature = ByteBufferUtils.getSignature(data, 20);
        }
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
        return getSecureSignature() != null;
    }
    
    public boolean isSecure() {
        return secureStatus == SECURE;
    }

    public byte[] getSecureSignature() {
        return signature;
    }

    public void updateSignatureWithSecuredBytes(Signature signature) 
            throws SignatureException {
        
    }
    
    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeByte(request);
    }
    
    public String toString() {
        return "StatsRequest: " + request;
    }
}
