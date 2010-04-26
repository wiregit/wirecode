package org.limewire.mojito.message2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.SecurityToken;
import org.limewire.util.StringUtils;

public class MessageOutputStream extends DataOutputStream {

    private static final byte[] EMPTY = new byte[0];
    
    public MessageOutputStream(OutputStream out) {
        super(out);
    }
    
    public void writeMessage(Message message) throws IOException {
        
        writeMessageId(message.getMessageId());
        writeByte(Message.F_DHT_MESSAGE);
        writeVersion(Version.ZERO);
        writeInt(0); /* SIZE! */
        
        OpCode opcode = OpCode.valueOf(message);
        Contact src = message.getContact();
        
        writeOpCode(opcode); // 0
        writeVendor(src.getVendor()); // 1-3
        writeVersion(src.getVersion()); // 4-5
        writeKUID(src.getNodeID()); // 6-25
        writeSocketAddress(src.getContactAddress()); // 26-33
        writeByte(src.getInstanceID()); // 34
        writeByte(src.getFlags()); // 35
        
        // The length of the extended header.
        writeShort(0);  // 36-37
        
        switch (opcode) {
            case PING_REQUEST:
                writePingRequest((PingRequest)message);
                break;
            case PING_RESPONSE:
                writePingResponse((PingResponse)message);
                break;
            case FIND_NODE_REQUEST:
                writeNodeRequest((NodeRequest)message);
                break;
            case FIND_NODE_RESPONSE:
                writeNodeResponse((NodeResponse)message);
                break;
            case FIND_VALUE_REQUEST:
                writeValueRequest((ValueRequest)message);
                break;
            case FIND_VALUE_RESPONSE:
                writeValueResponse((ValueResponse)message);
                break;
            case STORE_REQUEST:
                writeStoreRequest((StoreRequest)message);
                break;
            case STORE_RESPONSE:
                writeStoreResponse((StoreResponse)message);
                break;
            default:
                throw new IllegalArgumentException("message=" + message);
        }
    }
    
    private void writePingRequest(PingRequest message) throws IOException {
        // Do nothing
    }
    
    private void writePingResponse(PingResponse message) throws IOException {
        writeSocketAddress(message.getExternalAddress());
        writeBigInteger(message.getEstimatedSize());
    }
    
    private void writeNodeRequest(NodeRequest message) throws IOException {
        writeKUID(message.getLookupId());
    }
    
    private void writeNodeResponse(NodeResponse message) throws IOException {
        writeSecurityToken(message.getSecurityToken());
        writeContacts(message.getContacts());
    }
    
    private void writeValueRequest(ValueRequest message) throws IOException {
        writeKUID(message.getLookupId());
        
        writeKUIDs(message.getSecondaryKeys());
        writeValueType(message.getValueType());
    }
    
    private void writeValueResponse(ValueResponse message) throws IOException {
        writeFloat(message.getRequestLoad());
        writeValueEntities(message.getValueEntities());
        writeKUIDs(message.getSecondaryKeys());
    }
    
    private void writeStoreRequest(StoreRequest message) throws IOException {
        writeSecurityToken(message.getSecurityToken());
        writeValueEntities(message.getValueEntities());
    }
    
    private void writeStoreResponse(StoreResponse message) throws IOException {
        writeStoreStatusCodes(message.getStoreStatusCodes());
    }
    
    public void writeValueEntities(DHTValueEntity[] values) throws IOException {
        writeByte(values.length);
        for (DHTValueEntity value : values) {
            writeValueEntity(value);
        }
    }
    
    public void writeValueEntity(DHTValueEntity value) throws IOException {
        writeContact(value.getCreator());
        writeKUID(value.getPrimaryKey());
        writeValue(value.getValue());
    }
    
    public void writeValue(DHTValue value) throws IOException {
        writeValueType(value.getValueType());
        writeVersion(value.getVersion());
        
        byte[] data = value.getValue();
        writeShort(data.length);
        write(data);
    }
    
    public void writeSocketAddress(SocketAddress address) throws IOException {
        if (address instanceof InetSocketAddress
                && !((InetSocketAddress)address).isUnresolved()) {
            InetSocketAddress isa = (InetSocketAddress)address;
            writeInetAddress(isa.getAddress());
            writePort(isa.getPort());
        } else {
            writeByte(0);
        }
    }
    
    public void writeInetAddress(InetAddress address) throws IOException {
        if (address != null) {
            byte[] addr = address.getAddress();
            writeByte(addr.length);
            write(addr);
        } else {
            writeByte(0);
        }
    }
    
    public void writePort(int port) throws IOException {
        writeShort(port);
    }
    
    public void writeVendor(Vendor vendor) throws IOException {
        writeInt(vendor.intValue());
    }
    
    public void writeVersion(Version version) throws IOException {
        writeShort(version.shortValue());
    }
    
    public void writeStatusCode(StatusCode value) throws IOException {
        writeShort(value.shortValue());
        
        byte[] description = getBytes(value.getDescription());
        writeShort(description.length);
        write(description);
    }
    
    public void writeMessageId(MessageID messageId) throws IOException {
        messageId.write(this);
    }
    
    public void writeKUIDs(KUID[] kuids) throws IOException {
        writeByte(kuids.length);
        for (KUID kuid : kuids) {
            writeKUID(kuid);
        }
    }
    
    public void writeKUID(KUID kuid) throws IOException {
        kuid.write(this);
    }
    
    public void writeStoreStatusCodes(StoreStatusCode[] codes) throws IOException {
        writeByte(codes.length);
        
        for (StoreStatusCode code : codes) {
            writeKUID(code.getPrimaryKey());
            writeKUID(code.getSecondaryKey());
            writeStatusCode(code.getStatusCode());
        }
    }
    
    
    public void writeSecurityToken(SecurityToken securityToken) throws IOException {
        if (securityToken != null) {
            assert (securityToken instanceof AddressSecurityToken);
            byte[] qk = securityToken.getBytes();
            writeByte(qk.length);
            write(qk, 0, qk.length);
        } else {
            writeByte(0);
        }
    }
    
    public void writeOpCode(OpCode opcode) throws IOException {
        writeByte(opcode.byteValue());
    }
    
    public void writeBigInteger(BigInteger value) throws IOException {
        byte[] data = value.toByteArray();
        if (data.length > KUID.LENGTH) {
            throw new IllegalArgumentException();
        }
        
        writeByte(data.length);
        write(data);
    }
    
    public void writeContacts(Contact[] contacts) throws IOException {
        writeByte(contacts.length);
        for (Contact contact : contacts) {
            writeContact(contact);
        }
    }
    
    public void writeContact(Contact contact) throws IOException {
        writeVendor(contact.getVendor());
        writeVersion(contact.getVersion());
        writeKUID(contact.getNodeID());
        writeSocketAddress(contact.getContactAddress());
    }
    
    public void writeValueType(DHTValueType valueType) throws IOException {
        writeInt(valueType.intValue());
    }
    
    private static byte[] getBytes(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }
        
        return StringUtils.toUTF8Bytes(value);
    }
}
