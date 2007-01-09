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
 
package org.limewire.mojito.messages.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.Signature;
import java.security.SignatureException;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;

import com.limegroup.gnutella.util.ByteBufferOutputStream;

/**
 * An abstract implementation of DHTMessage
 */
abstract class AbstractDHTMessage implements DHTMessage {
    
    protected final Context context;
    
    private OpCode opcode;
    
    private Contact contact;
    
    private MessageID messageId;
    
    private byte[] payload;
    
    public AbstractDHTMessage(Context context, 
            OpCode opcode, Contact contact, MessageID messageId) {

        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        if (contact == null) {
            throw new NullPointerException("Contact is null");
        }

        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        this.contact = contact;
        this.messageId = messageId;
    }

    public AbstractDHTMessage(Context context, OpCode opcode, SocketAddress src, 
            MessageID messageId, int version, MessageInputStream in) throws IOException {
        
        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        this.messageId = messageId;
        
        int vendor = in.readInt();
        KUID nodeId = in.readKUID();
        SocketAddress contactAddress = in.readSocketAddress();
        
        if (contactAddress == null) {
            throw new UnknownHostException("Contact Address is null");
        }
        
        int instanceId = in.readUnsignedByte();
        int flags = in.readUnsignedByte();
        
        this.contact = ContactFactory.createLiveContact(src, vendor, version, 
                nodeId, contactAddress, instanceId, flags);
        
        int extensions = in.readUnsignedShort();
        in.skip(extensions);
    }
    
    public Context getContext() {
        return context;
    }
    
    public OpCode getOpCode() {
        return opcode;
    }

    public Contact getContact() {
        return contact;
    }
    
    public MessageID getMessageID() {
        return messageId;
    }
    
    public void write(OutputStream os) throws IOException {
        serialize();
        
        MessageOutputStream out = new MessageOutputStream(os);
        
        // --- GNUTELLA HEADER ---
        
        messageId.write(out); // 0-15
        out.writeByte(DHTMessage.F_DHT_MESSAGE); // 16
        out.writeShort(getContact().getVersion()); //17-18
        
        // Length is in Little-Endian!
        out.write((payload.length      ) & 0xFF); // 19-22
        out.write((payload.length >>  8) & 0xFF);
        out.write((payload.length >> 16) & 0xFF);
        out.write((payload.length >> 24) & 0xFF);
        
        // --- GNUTELLA PAYLOAD ---
        out.write(payload, 0, payload.length); // 23-n
    }
    
    private synchronized void serialize() throws IOException {
        if (payload != null) {
            return;
        }
        
        ByteBufferOutputStream baos = new ByteBufferOutputStream(640);
        MessageOutputStream out = new MessageOutputStream(baos);
        
        // --- MOJITO HEADER CONINUED ---
        writeHeader(out);
        
        // --- MOJITO BODY ---
        writeBody(out);
        
        out.close();
        payload = baos.toByteArray();
    }
    
    protected void writeHeader(MessageOutputStream out) throws IOException {
        out.writeOpCode(getOpCode()); // 0
        out.writeInt(getContact().getVendor()); // 1-3
        out.writeKUID(getContact().getNodeID()); // 4-23
        out.writeSocketAddress(getContact().getContactAddress()); // 24-31
        out.writeByte(getContact().getInstanceID()); // 32
        out.writeByte(getContact().getFlags()); // 33
        
        // We don't support any header extensions so write none
        out.writeShort(0); // 34-35
    }
    
    protected abstract void writeBody(MessageOutputStream out) throws IOException;
    
    protected void initSignature(Signature signature) 
            throws SignatureException {
        try {
            // Destination
            SocketAddress myExternalAddress = context.getContactAddress();
            signature.update(NetworkUtils.getBytes(myExternalAddress));

            // Source
            SocketAddress contactAddress = getContact().getContactAddress();
            signature.update(NetworkUtils.getBytes(contactAddress));
        } catch (UnknownHostException err) {
            throw new SignatureException(err);
        }
    }
}
