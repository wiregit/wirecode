package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.GGEP;

public abstract class AbstractPrivateGroupsValue implements PrivateGroupsValue{

    /**
     * DHTValueType for PrivateGroups
     */
    public static final DHTValueType PRIVATE_GROUPS = DHTValueType.valueOf("Gnutella Private Group", "PGRP");
    
    protected final Version version;
    
    public static final Version VERSION = Version.valueOf(0);
    
    static final String CLIENT_ID = "client-id";
    
    static final String VALUE = "value";
    
    static final String PORT = "port";
    
    static final String TYPE = "type";
    
    

    
    public AbstractPrivateGroupsValue(Version version){
        this.version = version;
    }
    
    public int size() {
        return getValue().length;
    }
    
    public DHTValueType getValueType() {
        return PRIVATE_GROUPS;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public abstract byte[] getGUID();

    public abstract int getPort(); 
    
    public abstract int getPublicKey();
    
    public abstract byte[] getIPAddress();
   
    
    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append("GUID=").append(new GUID(getGUID())).append("\n");
        buffer.append("Type=").append(getValueType().getName()).append("\n");
        buffer.append("Port=").append(getPort()).append("\n");
        buffer.append("PublicKey=").append(getPublicKey()).append("\n");
        buffer.append("IPAddress=").append(getIPAddress()).append("\n");
        return buffer.toString();
    }
    
    public static byte[] serialize(PrivateGroupsValue value){
        
        GGEP ggep = new GGEP();
        ggep.put(CLIENT_ID, value.getGUID());
        ggep.put(TYPE, "ANTHONY's TYPE");
        
        byte[] port = new byte[2];
        ByteOrder.short2beb((short)value.getPort(), port, 0);
        ggep.put(PORT, port);
        
        return ggep.toByteArray();

    }
    
    public static int setGroupID(int id){
        
        return id;
    }
    
    /**
     * RSA Algorithm
     */

}
