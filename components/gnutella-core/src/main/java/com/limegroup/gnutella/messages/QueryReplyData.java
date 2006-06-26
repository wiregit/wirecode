package com.limegroup.gnutella.messages;

import java.util.Set;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.IpPort;

class QueryReplyData {

    /** If parsed, the responses vendor string, if defined, or null otherwise. */
    private volatile String vendor = null;
    
    /** If parsed, one of TRUE (push needed), FALSE, or UNDEFINED. */
    private volatile int pushFlag = QueryReply.UNDEFINED;
    
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int busyFlag = QueryReply.UNDEFINED;
    
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int uploadedFlag = QueryReply.UNDEFINED;
    
    /** If parsed, one of TRUE (server busy), FALSE, or UNDEFINTED. */
    private volatile int measuredSpeedFlag = QueryReply.UNDEFINED;

    /** Determines if the remote host supports chat */
    private volatile boolean supportsChat = false;
    
    /** Determines if the remote host supports browse host */
    private volatile boolean supportsBrowseHost = false;
    
    /** Determines if this is a reply to a multicast query */
    private volatile boolean replyToMulticast = false;
    
    /** Determines if the remote host supports FW transfers */
    private volatile boolean supportsFWTransfer = false;
    
    /** Version number of FW Transfer the host supports. */
    private volatile byte fwTransferVersion = (byte)0;
    
    /** If parsed, the response records for this, or null if they could not be parsed. */
    private volatile Response[] responses = null;
    
    /** The number of unique results (by SHA1) this message carries */
    private volatile short uniqueResultURNs;

    /** the PushProxy info for this hit. */ 
    private volatile Set<? extends IpPort> proxies; 
    
    /** Whether or not this is a result from a browse-host reply. */  
    private volatile boolean browseHostReply;  

    /**  The HostData containing information about this QueryReply. */
    private volatile HostData hostData;
    
    /** The data with info about the secure result. */  
    private volatile SecureGGEPData secureGGEP;
    
    /** The xml chunk that contains metadata about xml responses*/  
    private volatile byte[] xmlBytes = DataUtils.EMPTY_BYTE_ARRAY;  
    

    public int getBusyFlag() {
        return busyFlag;
    }

    public void setBusyFlag(int busyFlag) {
        this.busyFlag = busyFlag;
    }

    public byte getFwTransferVersion() {
        return fwTransferVersion;
    }

    public void setFwTransferVersion(byte fwTransferVersion) {
        this.fwTransferVersion = fwTransferVersion;
    }

    public int getMeasuredSpeedFlag() {
        return measuredSpeedFlag;
    }

    public void setMeasuredSpeedFlag(int measuredSpeedFlag) {
        this.measuredSpeedFlag = measuredSpeedFlag;
    }

    public int getPushFlag() {
        return pushFlag;
    }

    public void setPushFlag(int pushFlag) {
        this.pushFlag = pushFlag;
    }

    public boolean isReplyToMulticast() {
        return replyToMulticast;
    }

    public void setReplyToMulticast(boolean replyToMulticast) {
        this.replyToMulticast = replyToMulticast;
    }

    public Response[] getResponses() {
        return responses;
    }

    public void setResponses(Response[] responses) {
        this.responses = responses;
    }

    public boolean isSupportsBrowseHost() {
        return supportsBrowseHost;
    }

    public void setSupportsBrowseHost(boolean supportsBrowseHost) {
        this.supportsBrowseHost = supportsBrowseHost;
    }

    public boolean isSupportsChat() {
        return supportsChat;
    }

    public void setSupportsChat(boolean supportsChat) {
        this.supportsChat = supportsChat;
    }

    public boolean isSupportsFWTransfer() {
        return supportsFWTransfer;
    }

    public void setSupportsFWTransfer(boolean supportsFWTransfer) {
        this.supportsFWTransfer = supportsFWTransfer;
    }

    public short getUniqueResultURNs() {
        return uniqueResultURNs;
    }

    public void setUniqueResultURNs(short uniqueResultURNs) {
        this.uniqueResultURNs = uniqueResultURNs;
    }

    public int getUploadedFlag() {
        return uploadedFlag;
    }

    public void setUploadedFlag(int uploadedFlag) {
        this.uploadedFlag = uploadedFlag;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public boolean isBrowseHostReply() {
        return browseHostReply;
    }

    public void setBrowseHostReply(boolean browseHostReply) {
        this.browseHostReply = browseHostReply;
    }

    public HostData getHostData() {
        return hostData;
    }

    public void setHostData(HostData hostData) {
        this.hostData = hostData;
    }

    public Set<? extends IpPort> getProxies() {
        return proxies;
    }

    public void setProxies(Set<? extends IpPort> proxies) {
        this.proxies = proxies;
    }

    public SecureGGEPData getSecureGGEP() {
        return secureGGEP;
    }

    public void setSecureGGEP(SecureGGEPData secureGGEP) {
        this.secureGGEP = secureGGEP;
    }

    public byte[] getXmlBytes() {
        return xmlBytes;
    }

    public void setXmlBytes(byte[] xmlBytes) {
        this.xmlBytes = xmlBytes;
    }
    
}
