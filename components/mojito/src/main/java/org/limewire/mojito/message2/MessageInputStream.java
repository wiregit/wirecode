package org.limewire.mojito.message2;

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
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
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
    
    public Message readMessage(MessageID messageId, SocketAddress src) throws IOException {
        OpCode opcode = readOpCode();
        /*Vendor vendor = readVendor();
        Version version = readVersion();
        KUID contactId = readKUID();
        SocketAddress contactAddress = readSocketAddress();
        int instanceId = readUnsignedByte();
        int flags = readUnsignedByte();*/
        
        Contact contact = readContact(
                opcode.isRequest() ? Type.UNSOLICITED : Type.SOLICITED, src);
        
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
        DHTValueType valueType = readValueType();
        
        return new DefaultValueRequest(messageId, contact, 
                lookupId, secondaryKeys, valueType);
    }
    
    private ValueResponse readValueResponse(MessageID messageId, 
            Contact contact) throws IOException {
        
        float requestLoad = readFloat();
        DHTValueEntity[] entities = readValueEntities(contact);
        KUID[] secondaryKeys = readKUIDs();
        
        return new DefaultValueResponse(messageId, contact, 
                requestLoad, secondaryKeys, entities);
    }
    
    private StoreRequest readStoreRequest(MessageID messageId, 
            Contact contact) throws IOException {
        
        SecurityToken securityToken = readSecurityToken();
        DHTValueEntity[] entities = readValueEntities(contact);
        
        return new DefaultStoreRequest(messageId, contact, 
                securityToken, entities);
    }
    
    private StoreResponse readStoreResponse(MessageID messageId, 
            Contact contact) throws IOException {
        StoreStatusCode[] codes = readStoreStatusCodes();
        return new DefaultStoreResponse(messageId, contact, codes);
    }
    
    public StoreStatusCode[] readStoreStatusCodes() throws IOException {
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
    
    public StatusCode readStatusCode() throws IOException {
        int code = readUnsignedShort();
        
        byte[] decription = new byte[readUnsignedShort()];
        readFully(decription);
        
        return StatusCode.valueOf(code, StringUtils.toUTF8String(decription));
    }
    
    public DHTValueEntity[] readValueEntities(Contact sender) throws IOException {
        int length = readUnsignedByte();
        DHTValueEntity[] entities = new DHTValueEntity[length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = readValueEntity(sender);
        }
        return entities;
    }
    
    public DHTValueEntity readValueEntity(Contact sender) throws IOException {
        Contact creator = readContact();
        KUID primaryKey = readKUID();
        DHTValue value = readValue();
        
        return DHTValueEntity.createFromRemote(creator, sender, primaryKey, value);
    }
    
    public DHTValue readValue() throws IOException {
        DHTValueType type = readValueType();
        Version version = readVersion();
        
        int length = readUnsignedShort();
        byte[] data = new byte[length];
        readFully(data);
        
        return new DHTValueImpl(type, version, data);
    }
    
    public OpCode readOpCode() throws IOException {
        return OpCode.valueOf(read());
    }
    
    public Vendor readVendor() throws IOException {
        return Vendor.valueOf(readInt());
    }
    
    public Version readVersion() throws IOException {
        return Version.valueOf(readUnsignedShort());
    }
    
    public KUID readKUID() throws IOException {
        return KUID.createWithInputStream(this);
    }
    
    public KUID[] readKUIDs() throws IOException {
        int length = readUnsignedByte();
        KUID[] kuids = new KUID[length];
        for (int i = 0; i < kuids.length; i++) {
            kuids[i] = readKUID();
        }
        return kuids;
    }
    
    public DHTValueType readValueType() throws IOException {
        return DHTValueType.valueOf(readInt());
    }
    
    public SecurityToken readSecurityToken() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] securityToken = new byte[length];
        readFully(securityToken);
        return new AddressSecurityToken(securityToken, calculator);
    }
    
    public SocketAddress readSocketAddress() throws IOException {
        InetAddress address = readInetAddress();
        if (address == null || !NetworkUtils.isValidAddress(address)) {
            return null;
        }
        
        int port = readPort();
        return new InetSocketAddress(address, port);
    }
    
    public InetAddress readInetAddress() throws IOException {
        int length = readUnsignedByte();
        if (length == 0) {
            return null;
        }
        
        byte[] address = new byte[length];
        readFully(address);
        
        return InetAddress.getByAddress(address);
    }
    
    public int readPort() throws IOException {
        return readUnsignedShort();
    }
    
    public Contact readContact(Type type, SocketAddress src) throws IOException {
        Vendor vendor = readVendor();
        Version version = readVersion();
        KUID contactId = readKUID();
        SocketAddress address = readSocketAddress();
        int instanceId = readUnsignedByte();
        int flags = readUnsignedByte();
        
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
    
    public Contact readContact() throws IOException {
        Vendor vendor = readVendor();
        Version version = readVersion();
        KUID contactId = readKUID();
        SocketAddress address = readSocketAddress();
        
        return ContactFactory.createUnknownContact(
                vendor, version, contactId, address);
    }
    
    public BigInteger readBigInteger() throws IOException {
        int length = readUnsignedByte();
        if (length > KUID.LENGTH) {
            throw new IOException("length=" + length);
        }
        
        byte[] data = new byte[length];
        readFully(data);
        
        return new BigInteger(1 /* unsigned */, data);
    }
}
