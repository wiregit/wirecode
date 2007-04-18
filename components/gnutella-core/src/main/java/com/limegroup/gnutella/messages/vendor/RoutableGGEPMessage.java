package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Signature;
import java.security.SignatureException;

import org.limewire.io.IPPortCombo;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.security.SecureMessage;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.GGEPParser;
import com.limegroup.gnutella.messages.SecureGGEPData;


/**
 * A ggep-based message that may have a specific return address.  It also contains 
 * an optional version number and can be secure.
 */
public class RoutableGGEPMessage extends VendorMessage implements SecureMessage {
    
    static final String RETURN_ADDRESS_KEY = "RA";
    static final String VERSION_KEY = "V";

    /** Whether or not this message has been verified as secure. */
    private int _secureStatus = SecureMessage.INSECURE;
    
    /**
     * The ggep field that this message is.
     */
    protected final GGEP ggep;
    
    /**
     * Secure ggep data.
     */
    private final SecureGGEPData secureData;
    
    /**
     * The return address of this message.
     */
    private final IpPort returnAddress;
    
    /**
     * The routing version of this message
     */
    private final int routableVersion;
    
    protected RoutableGGEPMessage(byte[] guid, byte ttl, byte hops, 
            byte [] vendor, int selector, int version, byte[] payload, int network)
    throws BadPacketException {
        super(guid, ttl, hops, vendor, selector, version, payload, network);

        // parse ggep
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(payload, 0);
        GGEP ggep = parser.getSecureGGEP();
        if (ggep == null) {
            ggep = parser.getNormalGGEP();
            if (ggep == null) // no ggep at all?
                throw new BadPacketException("no ggep at all");
            this.secureData = null;
        } else {
            this.secureData = new SecureGGEPData(parser);
        }
        this.ggep = ggep;
        
        // get routable version if any
        int routableVersion;
        try {
            routableVersion = ggep.getInt(VERSION_KEY);
        } catch (BadGGEPPropertyException bad){
            routableVersion = -1;
        }
        this.routableVersion = routableVersion;
        
        // get return address if any
        IpPort retAddr = null;
        try {
            byte [] returnAddress = ggep.get(RETURN_ADDRESS_KEY);
            IPPortCombo.getCombo(returnAddress);
        } catch (InvalidDataException bleh) {}
        this.returnAddress = retAddr;
    }
    
    protected RoutableGGEPMessage(byte [] vendor, int selector, int version, GGEP ggep) {
        super(vendor, selector, version, derivePayload(ggep));
        this.ggep = ggep;
        // nodes cannot create messages with custom return address or version.
        this.returnAddress = null;
        this.routableVersion = -1;
        this.secureData = null;
    }
    
    private static byte [] derivePayload(GGEP ggep) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ggep.write(baos);
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        }
        return baos.toByteArray();
    }
    
    /**
     * @return the address responses to this message should be sent to.
     * null if none was present.
     */
    public IpPort getReturnAddress() {
        return returnAddress;
    }
    
    /**
     * @return the routable version of this message. Negative if none was present.
     */
    public int getRoutableVersion() {
        return routableVersion;
    }

    public byte[] getSecureSignature() {
        SecureGGEPData sg = secureData;
        if(sg != null) {
            try {
                return sg.getGGEP().getBytes(GGEP.GGEP_HEADER_SIGNATURE);
            } catch(BadGGEPPropertyException bgpe) {
                return null;
            }
        } else {
            return null;
        }
    }

    public int getSecureStatus() {
        return _secureStatus;
    }

    public void setSecureStatus(int secureStatus) {
        _secureStatus = secureStatus;
    }

    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
        SecureGGEPData sg = secureData;       
        if(sg != null) {
            signature.update(getPayload(), 0, sg.getStartIndex());
            int end = sg.getEndIndex();
            int length = getPayload().length - 16 - end;
            signature.update(getPayload(), end, length);
        }
        
    }
    
    
}
