package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

class PrivateGroupsValueImpl extends AbstractPrivateGroupsValue{

    private final byte[] data;
    private byte[] guid;
    private int port;
    private long fileSize;
    private final byte[] ipaddress;
    
    /**
     *  Constructor for testing purposes only 
     *
     **/
    
    public PrivateGroupsValueImpl(Version version, byte[] guid, int port, long fileSize){
        
        super(version);
        
        if (guid == null || guid.length != 16) {
            throw new IllegalArgumentException("Illegal GUID");
        }
        
        if (!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("Illegal port: " + port);
        }
        
        this.guid = guid;
        this.port = port;
        this.fileSize = fileSize;
        this.data = AbstractPrivateGroupsValue.serialize(this);
        this.ipaddress = GuiCoreMediator.getNetworkManager().getAddress();
    }
    
    public PrivateGroupsValueImpl(Version version, byte[] data) throws DHTValueException{
        super(version);
        
        if (version == null) {
            throw new DHTValueException("Version is null");
        }
        
        if (data == null) {
            throw new DHTValueException("Data is null");
        }
        
        this.data = data;
        this.ipaddress = GuiCoreMediator.getNetworkManager().getAddress();
        
        
        try {
            GGEP ggep = new GGEP(data, 0);
            
            this.guid = ggep.getBytes(AbstractPrivateGroupsValue.CLIENT_ID);
            if (guid.length != 16) {
                throw new DHTValueException("Illegal GUID length: " + guid.length);
            }
            
            byte[] portBytes = ggep.getBytes(AbstractPrivateGroupsValue.PORT);
            this.port = ByteOrder.beb2short(portBytes, 0) & 0xFFFF;
            if (!NetworkUtils.isValidPort(port)) {
                throw new DHTValueException("Illegal port: " + port);
            }
                       
            
        } catch (BadGGEPPropertyException err) {
            throw new DHTValueException(err);  
        } catch (BadGGEPBlockException err) {
            throw new DHTValueException(err);
        }
       
    }
    @Override
    public byte[] getGUID() {
        return guid;
    }

    @Override
    public int getPort() {
        return port;
    }

    public byte[] getValue() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public void write(OutputStream out) throws IOException {
        out.write(data);
        
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public int getPublicKey() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte[] getIPAddress() {
        return ipaddress;
    }


    
    

}
