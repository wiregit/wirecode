/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.Signature;
import java.security.SignatureException;

import org.limewire.io.ByteBufferOutputStream;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;


/**
 * An abstract implementation of DHTMessage
 */
abstract class AbstractDHTMessage implements DHTMessage {
    
    protected final Context context;
    
    private final OpCode opcode;
    
    private final Contact contact;
    
    private final MessageID messageId;
    
    private final Version messageVersion;
    
    private byte[] payload;
    
    public AbstractDHTMessage(Context context, 
            OpCode opcode, Contact contact, MessageID messageId) {
        this(context, opcode, contact, messageId, Version.valueOf(1));
    }
    
    public AbstractDHTMessage(Context context, 
            OpCode opcode, Contact contact, MessageID messageId, Version messageVersion) {

        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        if (contact == null) {
            throw new NullPointerException("Contact is null");
        }

        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        if (messageVersion == null) {
            throw new NullPointerException("Version is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        this.contact = contact;
        this.messageId = messageId;
        this.messageVersion = messageVersion;
    }

    public AbstractDHTMessage(Context context, OpCode opcode, SocketAddress src, 
            MessageID messageId, Version version, MessageInputStream in) throws IOException {
        
        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        this.messageId = messageId;
        
        Vendor vendor = in.readVendor();
        KUID nodeId = in.readKUID();
        SocketAddress contactAddress = in.readSocketAddress();
        
        if (contactAddress == null) {
            throw new UnknownHostException("Contact Address is null");
        }
        
        int instanceId = in.readUnsignedByte();
        int flags = in.readUnsignedByte();
        
        this.contact = ContactFactory.createLiveContact(src, vendor, version, 
                nodeId, contactAddress, instanceId, flags);
        
        Version messageVersion = Version.ZERO;
        int extensionsLength = in.readUnsignedShort();
        if (extensionsLength > 0) {
            messageVersion = in.readVersion();
            extensionsLength -= Version.LENGTH;
        }
        this.messageVersion = messageVersion;
        
        in.skip(extensionsLength);
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
    
    public Version getMessageVersion() {
        return messageVersion;
    }
    
    public void write(OutputStream os) throws IOException {
        serialize();
        
        MessageOutputStream out = new MessageOutputStream(os);
        
        // --- GNUTELLA HEADER ---
        
        messageId.write(out); // 0-15
        out.writeByte(DHTMessage.F_DHT_MESSAGE); // 16
        out.writeShort(getContact().getVersion().getVersion()); //17-18
        
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
        out.writeInt(getContact().getVendor().getVendor()); // 1-3
        out.writeKUID(getContact().getNodeID()); // 4-23
        out.writeSocketAddress(getContact().getContactAddress()); // 24-31
        out.writeByte(getContact().getInstanceID()); // 32
        out.writeByte(getContact().getFlags()); // 33
        
        // Write the extended header
        writeExtendedHeader(out); // 34-
    }
    
    private void writeExtendedHeader(MessageOutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2);
        MessageOutputStream mos = new MessageOutputStream(baos);
        mos.writeVersion(getMessageVersion());
        mos.close();
        
        byte[] extendedHeader = baos.toByteArray();
        out.writeShort(extendedHeader.length); // 34-35
        out.write(extendedHeader); // 36-
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
