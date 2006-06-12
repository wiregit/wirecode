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

import java.security.Signature;
import java.security.SignatureException;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;

/**
 * An abstract base class for request Messages
 */
public abstract class AbstractRequestMessage extends AbstractDHTMessage
        implements RequestMessage {

    private int secureStatus = INSECURE;

    private byte[] signature;

    public AbstractRequestMessage(int opcode, int vendor, int version,
            ContactNode node, KUID messageId) {
        this(opcode, vendor, version, node, messageId, null);
    }

    public AbstractRequestMessage(int opcode, int vendor, int version,
            ContactNode node, KUID messageId, byte[] signature) {
        super(opcode, vendor, version, node, messageId);

        this.signature = signature;
    }

    public void setSecureStatus(int secureStatus) {
        this.secureStatus = secureStatus;
    }

    public int getSecureStatus() {
        return secureStatus;
    }

    public boolean isSigned() {
        return signature != null;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
    
    public boolean isSecure() {
        return secureStatus == SECURE;
    }

    public byte[] getSecureSignature() {
        return signature;
    }

    public void updateSignatureWithSecuredBytes(Signature signature) 
            throws SignatureException {
        // TODO Auto-generated method stub
    }
}
