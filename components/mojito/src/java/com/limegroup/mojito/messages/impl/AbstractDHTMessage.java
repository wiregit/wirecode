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
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.Signature;
import java.security.SignatureException;

import com.limegroup.gnutella.util.ByteBufferInputStream;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * An abstract implementation of DHTMessage
 */
public abstract class AbstractDHTMessage extends AbstractMessage implements DHTMessage {

    /*
     *  To remove the (Gnutella) Message dependence don't
     *  extend from AbstractMessage and scroll down to the
     *  bottom of this class.
     */
    
    protected final Context context;
    
    private ByteBuffer data;
    
    private int opcode;
    
    private int vendor;
    private int version;

    private ContactNode contactNode;
    
    private KUID messageId;
    
    private MessageInputStream in;
    
    public AbstractDHTMessage(Context context, 
            int opcode, int vendor, int version,
            ContactNode contactNode, KUID messageId) {

        if (!checkOpCode(opcode)) {
            throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
        
        if (contactNode == null) {
            throw new NullPointerException("ContactNode is null");
        }

        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }

        if (!messageId.isMessageID()) {
            throw new IllegalArgumentException("MessageID is of wrong type: " + messageId);
        }
        
        if ((version & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("Version must be between 0x00 and 0xFFFF: " + version);
        }
        
        this.context = context;
        this.opcode = opcode;
        this.vendor = vendor;
        this.version = version;
        this.contactNode = contactNode;
        this.messageId = messageId;
    }

    public AbstractDHTMessage(Context context, 
            int opcode, SocketAddress src, ByteBuffer data) throws IOException {
        
        if (!checkOpCode(opcode)) {
            throw new IOException("Unknown opcode: " + opcode);
        }
        
        this.context = context;
        this.opcode = opcode;
        
        in = new MessageInputStream(new ByteBufferInputStream(data));
        
        this.vendor = in.readInt();
        this.version = in.readUnsignedShort();
        
        KUID nodeId = in.readNodeID();
        int instanceId = in.readUnsignedByte();
        int nodeFlags = in.readUnsignedByte();
        this.contactNode = new ContactNode(nodeId, src, instanceId, nodeFlags);
        
        this.messageId = in.readMessageID();
        
        //int messageFlags = in.readUnsignedByte();
        //int checksum = in.readInt();
        //in.skipBytes(5); // see above
        in.skip(5);
        
        this.data = data;
    }
    
    private static boolean checkOpCode(int opcode) {
        switch(opcode) {
            case PING_REQUEST:
            case PING_RESPONSE:
            case STORE_REQUEST:
            case STORE_RESPONSE:
            case FIND_NODE_REQUEST:
            case FIND_NODE_RESPONSE:
            case FIND_VALUE_REQUEST:
            case FIND_VALUE_RESPONSE:
            case STATS_REQUEST:
            case STATS_RESPONSE:
                return true;
            default:
                return false;
        }
    }
    
    protected MessageInputStream getMessageInputStream() throws IOException {
        if (in == null) {
            throw new IOException("Illegal State");
        }
        return in;
    }
    
    public Context getContext() {
        return context;
    }
    
    public ByteBuffer getData() {
        return data;
    }
    
    public int getOpCode() {
        return opcode;
    }
    
    public int getVendor() {
        return vendor;
    }

    public int getVersion() {
        return version;
    }

    public ContactNode getContactNode() {
        return contactNode;
    }
    
    public KUID getMessageID() {
        return messageId;
    }
    
    /*
     *  To remove the (Gnutella) Message dependence rename
     *  writeMessage(OutputStream) to write(OutputStream) 
     */
    
    // public void write(OutputStream out) throws IOException {
    protected void writeMessage(OutputStream out) throws IOException {
        MessageOutputStream msgOut = new MessageOutputStream(out);
        writeHeader(msgOut);
        writeBody(msgOut);
    }
    
    protected void writeHeader(MessageOutputStream out) throws IOException {
        out.writeByte(getOpCode()); // 0
        out.writeInt(getVendor()); // 1-3
        out.writeShort(getVersion()); // 4-5
        out.writeKUID(getContactNode().getNodeID()); // 6-26
        out.writeByte(getContactNode().getInstanceID()); // 27
        out.writeByte(getContactNode().getFlags()); // 28
        out.writeKUID(getMessageID()); // 29-48
        out.writeByte(0); // 49
        out.writeInt(0); // 50-53
    }
    
    protected abstract void writeBody(MessageOutputStream out) throws IOException;
    
    protected void initSignature(Signature signature) 
            throws SignatureException {
        try {
            // Destination
            SocketAddress myExternalAddress = context.getSocketAddress();
            signature.update(NetworkUtils.getBytes(myExternalAddress));

            // Source
            SocketAddress contactAddress = getContactNode().getSocketAddress();
            signature.update(NetworkUtils.getBytes(contactAddress));
        } catch (UnknownHostException err) {
            throw new SignatureException(err);
        }
    }
}
