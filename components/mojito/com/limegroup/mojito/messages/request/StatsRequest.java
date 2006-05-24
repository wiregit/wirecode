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
 
package com.limegroup.mojito.messages.request;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.util.NetworkUtils;

/**
 * 
 */
public class StatsRequest extends RequestMessage {
    
    public static final int STATS = 0x00;
    public static final int DB = 0x01;
    public static final int RT = 0x02;
    
    private int request;

    public StatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, int request) {
        super(vendor, version, node, messageId);
        
        this.request = request;
    }
    
    public StatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, byte[] signature, int request) {
        super(vendor, version, node, messageId, signature);
        
        this.request = request;
    }
    
    public int getRequest() {
        return request;
    }

    public boolean isDBRequest() {
        return (request & DB) == DB;
    }
    
    public boolean isRTRequest() {
        return (request & RT) == RT;
    }
    
    public boolean verify(PublicKey pubKey, SocketAddress src, SocketAddress dst) 
            throws IOException, InvalidKeyException, SignatureException {
        byte[] signature = getSignature();
        if (pubKey != null && signature != null) {
            return CryptoHelper.verify(pubKey, getSignatureBlock(src, dst), signature);
        }
        return false;
    }

    private byte[] getSignatureBlock(SocketAddress src, SocketAddress dst) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(20 + 18 + 18 + 4);
        DataOutputStream out = new DataOutputStream(baos);
        out.write(getMessageID().getBytes());
        out.write(NetworkUtils.getBytes(src));
        out.write(NetworkUtils.getBytes(dst));
        out.writeInt(getRequest());
        out.close();
        return baos.toByteArray();
    }
}
