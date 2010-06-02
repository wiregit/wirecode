package org.limewire.mojito.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.storage.DefaultValue;
import org.limewire.mojito.storage.ValueType;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.StringUtils;

public class MessageInputStream extends DataInputStream {

    private static enum Type {
        SOLICITED,
        UNSOLICITED;
    }
    
    private final MACCalculatorRepositoryManager calculator;
    
    public MessageInputStream(InputStream in, 
            MACCalculatorRepositoryManager calculator) {
        super(in);
        
        this.calculator = calculator;
    }
    
    public Message readMessage(SocketAddress src) throws IOException {
        MessageID messageId = readMessageId();
        
        int func = readUnsignedByte();
        if (func != Message.F_DHT_MESSAGE) {
            throw new IOException("func=" + func);
        }
        
        // The version of the Message
        Version version = readVersion();
        if (!version.equals(Version.ZERO)) {
            throw new IOException("version=" + version);
        }
        
        // Skip the payload length! Keep in mind it's
        // in Little-Endian!
        skip(4);
        
        return readPayload(src, messageId, version);
    }
    
    private Message readPayload(SocketAddress src, 
            MessageID messageId, Version version) throws IOException {
        
        OpCode opcode = readOpCode();
        Contact contact = readContact(opcode.isRequest() 
                ? Type.UNSOLICITED : Type.SOLICITED, src);
        
        int extendedHeader = readUnsignedShort();
        skip(extendedHeader);
        
        switch (opcode) {
            case PING_REQUEST:
                return readPingRequest(messageId, contact);
            case PING_RESPONSE:
                return readPingResponse(messageId, contact);
            case FIND_NODE_REQUEST:
                return readNodeRequest(messageId, contact);
            case FIND_NODE_RESPONSE:
                return readNodeResponse(messageId, contact);
            case FIND_VALUE_REQUEST:
                return readValueRequest(messageId, contact);
            case FIND_VALUE_RESPONSE:
                return readValueResponse(messageId, contact);
            case STORE_REQUEST:
                return readStoreRequest(messageId, contact);
            case STORE_RESPONSE:
                return readStoreResponse(messageId, contact);
            default:
                throw new IOException("opcode=" + opcode);
        }
    }
    
    private PingRequest readPingRequest(MessageID messageId, 
            Contact contact) throws IOException {
        return new DefaultPingRequest(messageId, contact);
    }
    
    private PingResponse readPingResponse(MessageID messageId, 
            Contact contact) throws IOException {
        SocketAddress externalAddress = readSocketAddress();
        BigInteger estimatedSize = readBigInteger();
        
        return new DefaultPingResponse(messageId, contact, 
                externalAddress, estimatedSize);
    }
    
    private NodeRequest readNodeRequest(MessageID messageId, 
            Contact contact) throws IOException {
        KUID lookupId = readKUID();
        return new DefaultNodeRequest(messageId, contact, lookupId);
    }
    
    private NodeResponse readNodeResponse(MessageID messageId, 
            Contact contact) throws IOException {
        
        SecurityToken securityToken = readSecurityToken();
        Contact[] contacts = readContacts();
        
        return new DefaultNodeResponse(messageId, contact, 
                        securityToken, contacts);
    }
    
    private ValueRequest readValueRequest(MessageID messageId, 
            Contact contact) throws IOException {
        
        KUID lookupId = readKUID();
        KUID[] secondaryKeys = readKUIDs();
        ValueType valueType = readValueType();
        
        return new DefaultValueRequest(messageId, contact, 
                lookupId, secondaryKeys, valueType);
    }
    
    private ValueResponse readValueResponse(MessageID messageId, 
            Contact contact) throws IOException {
        
        float requestLoad = readFloat();
        ValueTuple[] entities = readValueTuples(contact);
        KUID[] secondaryKeys = readKUIDs();
        
        return new DefaultValueResponse(messageId, contact, 
                requestLoad, secondaryKeys, entities);
    }
    
    private StoreRequest readStoreRequest(MessageID messageId, 
            Contact contact) throws IOException {
        
        SecurityToken securityToken = readSecurityToken();
        ValueTuple[] entities = readValueTuples(contact);
        
        return new DefaultStoreRequest(messageId, contact, 
                securityToken, entities);
    }
    
    private StoreResponse readStoreResponse(MessageID messageId, 
            Contact contact) throws IOException {
        StoreStatusCode[] codes = readStoreStatusCodes();
        return new DefaultStoreResponse(messageId, contact, codes);
    }
    
    private StoreStatusCode[] readStoreStatusCodes() throws IOException {
        int length = readUnsignedByte();
        StoreStatusCode[] codes = new StoreStatusCode[length];
        
        for (int i = 0; i < codes.length; i++) {
            KUID primaryKey = readKUID();
            KUID secondaryKey = readKUID();
            StatusCode code = readStatusCode();
            
            codes[i] = new StoreStatusCode(primaryKey, secondaryKey, code);
        }
        
        return codes;
    }
    
    private StatusCode readStatusCode() throws IOException {
        int code = readUnsignedShort();
        
        byte[] decription = new byte[readUnsignedShort()];
        readFully(decription);
        
        return StatusCode.valueOf(code, StringUtils.toUTF8String(decription));
    }
    
    private ValueTuple[] readValueTuples(Contact sender) throws IOException {
        int length = readUnsignedByte();
        ValueTuple[] entities = new ValueTuple[length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = readValueTuple(sender);
        }
        return entities;
    }
    
    private ValueTuple readValueTuple(Contact sender) throws IOException {
        Contact creator = readContact();
        KUID primaryKey = readKUID();
        Value value = readValue();
        
        return ValueTuple.createValueTuple(
                creator, sender, primaryKey, value);
    }
    
    private Value readValue() throws IOException {
        ValueType type = readValueType();
        Version version = readVersion();
        
        int length = readUnsignedShort();
        byte[] data = new byte[length];
        readFully(data);
        
        return new DefaultValue(type, version, data);
    }
    
    private OpCode readOpCode() throws IOException {
        return OpCode.valueOf(read());
    }
    
    private Vendor readVendor() throws IOException {
        return Vendor.valueOf(readInt());
    }
    
    private Version readVersion() throws IOException {
        return Version.valueOf(readUnsignedShort());
    }
    
    private KUID readKUID() throws IOException {
        return KUID.createWithInputStream(this);
    }
    
    private KUID[] readKUIDs() throws IOException {
        int length = readUnsignedByte();
        KUID[] kuids = new KUID[length];
        for (int i = 0; i < kuids.length; i++) {
            kuids[i] = readKUID();
        }
        return kuids;
    }
    
    private ValueType readValueType() throws IOException {
        return ValueType.valueOf(readInt());
    }
    
    private SecurityToken readSecurityToken() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] securityToken = new byte[length];
        readFully(securityToken);
        return new AddressSecurityToken(securityToken, calculator);
    }
    
    private SocketAddress readSocketAddress() throws IOException {
        InetAddress address = readInetAddress();
        if (address == null || !NetworkUtils.isValidAddress(address)) {
            return null;
        }
        
        int port = readPort();
        return new InetSocketAddress(address, port);
    }
    
    private InetAddress readInetAddress() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] address = new byte[length];
        readFully(address);
        
        return InetAddress.getByAddress(address);
    }
    
    private int readPort() throws IOException {
        return readUnsignedShort();
    }
    
    private Contact readContact(Type type, SocketAddress src) throws IOException {
        Vendor vendor = readVendor();
        Version version = readVersion();
        KUID contactId = readKUID();
        SocketAddress address = readSocketAddress();
        int instanceId = readUnsignedByte();
        int flags = readUnsignedByte();
        
        if (address == null) {
            address = src;
        }
        
        return ContactFactory.createLiveContact(src, vendor, version, 
                contactId, address, instanceId, flags);
    }
    
    public Contact[] readContacts() throws IOException {
        int size = readUnsignedByte();
        
        Contact[] contacts = new Contact[size];
        for(int i = 0; i < contacts.length; i++) {
            contacts[i] = readContact();
        }
        
        return contacts;
    }
    
    private Contact readContact() throws IOException {
        Vendor vendor = readVendor();
        Version version = readVersion();
        KUID contactId = readKUID();
        SocketAddress address = readSocketAddress();
        
        return ContactFactory.createUnknownContact(
                vendor, version, contactId, address);
    }
    
    private BigInteger readBigInteger() throws IOException {
        int length = readUnsignedByte();
        if (length > KUID.LENGTH) {
            throw new IOException("length=" + length);
        }
        
        byte[] data = new byte[length];
        readFully(data);
        
        return new BigInteger(1 /* unsigned */, data);
    }
    
    private MessageID readMessageId() throws IOException {
        return DefaultMessageID.createWithInputStream(this, calculator);
    }
}
