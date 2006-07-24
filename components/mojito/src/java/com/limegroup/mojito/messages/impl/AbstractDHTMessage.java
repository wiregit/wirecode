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

    public AbstractDHTMessage(Context context, 
            OpCode opcode, SocketAddress src, MessageInputStream in) throws IOException {
        
        if (opcode == null) {
            throw new NullPointerException("OpCode is null");
        }
        
        this.context = context;
        this.opcode = opcode;
        
        int vendor = in.readInt();
        int version = in.readUnsignedShort();
        KUID nodeId = in.readNodeID();
        SocketAddress contactAddress = in.readSocketAddress();
        int instanceId = in.readUnsignedByte();
        int flags = in.readUnsignedByte();
        
        boolean firewalled = (flags & FIREWALLED) != 0;
        
        this.contact = ContactNode.createLiveContact(src, vendor, version, 
                nodeId, contactAddress, instanceId, firewalled);
        
        this.messageId = in.readMessageID();
        
        //int messageFlags = in.readUnsignedByte();
        //int checksum = in.readInt();
        in.skip(5); // see above
    }
    
    public Context getContext() {
        return context;
    }
    
    public ByteBuffer[] getData() {
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
    
    /*
     *  To remove the (Gnutella) Message dependence rename
     *  writePayload(OutputStream) to write(OutputStream) 
     */
    
    // public void write(OutputStream out) throws IOException {
    protected void writePayload(OutputStream out) throws IOException {
        MessageOutputStream msgOut = new MessageOutputStream(out);
        writeHeader(msgOut);
        writeBody(msgOut);
    }
    
    protected void writeHeader(MessageOutputStream out) throws IOException {
        out.writeOpCode(getOpCode()); // 0
        out.writeInt(getContact().getVendor()); // 1-3
        out.writeShort(getContact().getVersion()); // 4-5
        out.writeKUID(getContact().getNodeID()); // 6-26
        out.writeSocketAddress(getContact().getContactAddress());
        out.writeByte(getContact().getInstanceID()); // 27
        
        int flags = 0;
        if (getContact().isFirewalled()) {
            flags |= FIREWALLED;
        }
        out.writeByte(flags);
        
        out.writeMessageID(getMessageID()); // 29-48
        out.writeByte(0); // 49
        out.writeInt(0); // 50-53
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
