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
 
package com.limegroup.mojito.messages.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.Signature;
import java.security.SignatureException;

import com.limegroup.gnutella.util.ByteBufferOutputStream;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.routing.impl.ContactNode;

/**
 * An abstract implementation of DHTMessage
 */
abstract class AbstractDHTMessage extends AbstractMessage implements DHTMessage {

    private static final int FIREWALLED = 0x01;
    
    /*
     *  To remove the (Gnutella) Message dependence don't
     *  extend from AbstractMessage and scroll down to the
     *  bottom of this class.
     *  
     *  See also DefaultMessageFactory!
     */
    
    protected final Context context;
    
    private ByteBuffer[] data;
    
    private OpCode opcode;
    
    private Contact contact;
    
    private MessageID messageId;
    
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
        KUID nodeId = in.readNodeID();
        SocketAddress contactAddress = in.readSocketAddress();
        
        if (contactAddress == null) {
            throw new UnknownHostException("Contact Address is null");
        }
        
        int instanceId = in.readUnsignedByte();
        int flags = in.readUnsignedByte();
        
        boolean firewalled = (flags & FIREWALLED) != 0;
        
        this.contact = ContactNode.createLiveContact(src, vendor, version, 
                nodeId, contactAddress, instanceId, firewalled);
        
        //int messageFlags = in.readUnsignedByte();
        //int checksum = in.readInt();
        in.skip(5); // see above
    }
    
    public Context getContext() {
        return context;
    }
    
    protected ByteBuffer[] getData() {
        return data;
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
    
    @Override
    public void write(OutputStream os) throws IOException {
        serialize();
        
        MessageOutputStream out = new MessageOutputStream(os);
        
        // --- GNUTELLA HEADER ---
        
        messageId.write(out); // 0-15
        out.writeByte(DHTMessage.F_DHT_MESSAGE); // 16
        out.writeShort(getContact().getVendor()); //17-18
        
        ByteBuffer payload = data[0];
        int length = payload.remaining(); // 19-22
        out.write((length      ) & 0xFF);
        out.write((length >>  8) & 0xFF);
        out.write((length >> 16) & 0xFF);
        out.write((length >> 24) & 0xFF);
        
        out.write(payload.array(), payload.arrayOffset(), length); // 23-n
    }
    
    private synchronized void serialize() throws IOException {
        if (data != null && data.length == 1) {
            return;
        }
        
        ByteBufferOutputStream baos = new ByteBufferOutputStream(640);
        MessageOutputStream out = new MessageOutputStream(baos);
        
        // --- MOJITO HEADER CONINUED ---
        writeHeader(out);
        
        // --- MOJITO BODY ---
        writeBody(out);
        out.close();
        
        data = new ByteBuffer[]{ ByteBuffer.wrap(baos.toByteArray()) };
    }
    
    protected void writeHeader(MessageOutputStream out) throws IOException {
        out.writeOpCode(getOpCode()); // 0
        out.writeInt(getContact().getVendor()); // 1-3
        out.writeKUID(getContact().getNodeID()); // 4-23
        out.writeSocketAddress(getContact().getContactAddress()); // 24-31
        out.writeByte(getContact().getInstanceID()); // 32
        
        int flags = 0;
        if (getContact().isFirewalled()) {
            flags |= FIREWALLED;
        }
        out.writeByte(flags); // 33
        
        out.writeByte(0); // 34
        out.writeInt(0); // 35-38
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
