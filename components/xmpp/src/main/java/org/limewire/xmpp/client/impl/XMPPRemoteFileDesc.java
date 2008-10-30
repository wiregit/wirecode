package org.limewire.xmpp.client.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.DownloadStatsTracker;
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
    public PushEndpoint getPushAddr() {
        return null;
    }

    @Override
    public int getQueueStatus() {
        return 0;
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
    public boolean isPartialSource() {
        return availableRanges != null;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public boolean needsPush() {
        return false;
    }

    @Override
    public boolean needsPush(DownloadStatsTracker statsTracker) {
        return false;
    }

    @Override
    public void setDownloading(boolean dl) {
    }

    @Override
    public void setQueueStatus(int status) {
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
    public Address getAddress() {
        return null;
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
    public Set<? extends IpPort> getPushProxies() {
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
    public boolean isFirewalled() {
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
    public boolean supportsFWTransfer() {
        return false;
    }

    @Override
    public boolean isTLSCapable() {
        Address address = this.resolvedAddress;
        if (address != null) {
            if (address instanceof Connectable) {
                return ((Connectable)address).isTLSCapable();
            } else if (address instanceof FirewalledAddress) {
                return ((FirewalledAddress)address).getPrivateAddress().isTLSCapable();
            }
        }
        return false;
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

}
