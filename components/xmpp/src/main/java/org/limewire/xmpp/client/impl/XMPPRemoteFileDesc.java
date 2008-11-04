package org.limewire.xmpp.client.impl;

import java.util.Set;

import org.apache.http.auth.Credentials;
import org.limewire.collection.IntervalSet;
import org.limewire.io.Address;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.net.address.FirewalledAddress;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class XMPPRemoteFileDesc implements RemoteFileDesc {

    private Address resolvedAddress = null;
    private boolean http11;
    private IntervalSet availableRanges = null;
    
    public XMPPRemoteFileDesc(XMPPAddress xmmAddress) {
    }
    
    @Override
    public int getSecureStatus() {
        return 0;
    }

    @Override
    public float getSpamRating() {
        return 0;
    }

    @Override
    public String getUrlPath() {
        return null;
    }

    @Override
    public boolean isAltLocCapable() {
        return false;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public boolean isFromAlternateLocation() {
        // important to not end up in the set of invalid alts
        return false;
    }

    @Override
    public boolean isMe(byte[] myClientGUID) {
        Address address = this.resolvedAddress;
        if (address instanceof FirewalledAddress) {
            return ((FirewalledAddress)address).getClass().equals(new GUID(myClientGUID));
        }
        return false;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public void setDownloading(boolean dl) {
    }

    @Override
    public void setSecureStatus(int secureStatus) {
    }

    @Override
    public void setSerializeProxies() {
    }

    @Override
    public void setSpamRating(float rating) {
    }

    @Override
    public RemoteHostMemento toMemento() {
        return null;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public long getIndex() {
        return 0;
    }

    @Override
    public URN getSHA1Urn() {
        return null;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public Set<URN> getUrns() {
        return null;
    }

    @Override
    public LimeXMLDocument getXMLDocument() {
        return null;
    }

    @Override
    public byte[] getClientGUID() {
        return null;
    }

    @Override
    public int getQuality() {
        return 0;
    }

    @Override
    public int getSpeed() {
        return 0;
    }

    @Override
    public String getVendor() {
        return null;
    }

    @Override
    public boolean isBrowseHostEnabled() {
        return false;
    }

    @Override
    public boolean isChatEnabled() {
        return false;
    }

    
    @Override
    public boolean isHTTP11() {
        return http11;
    }

    @Override
    public boolean isReplyToMulticast() {
        return false;
    }

    @Override
    public void setHTTP11(boolean http11) {
        this.http11 = http11;
    }

    @Override
    public void setTLSCapable(boolean tlsCapable) {
        // no-op
    }

    @Override
    public Address getAddress() {
        return null;
    }

    @Override
    public Credentials getCredentials() {
        return null;
    }


}
