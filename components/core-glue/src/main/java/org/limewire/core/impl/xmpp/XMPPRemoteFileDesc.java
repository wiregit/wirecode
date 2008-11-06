package org.limewire.core.impl.xmpp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.client.impl.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class XMPPRemoteFileDesc implements RemoteFileDesc {

    private final XMPPAddress address;

    static final String TYPE = "XMPPRFD";

    private final long index;

    private final String filename;

    private final long size;

    private final byte[] clientGUID;

    private final int speed;

    private final int quality;

    private final LimeXMLDocument xmlDoc;

    private final Set<URN> urns;

    private final boolean replyToMulticast;

    private final String vendor;

    private final long createTime;

    private final AddressFactory addressFactory;
    
    private boolean http11;

    private final XMPPAddressResolver addressResolver;
    
    public XMPPRemoteFileDesc(XMPPAddress address, long index, String filename,
            long size, byte[] clientGUID, int speed, boolean chat, int quality, boolean browseHost,
            LimeXMLDocument xmlDoc, Set<? extends URN> urns, boolean replyToMulticast,
            String vendor, long createTime, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        this.address = address;
        this.index = index;
        this.filename = filename;
        this.size = size;
        this.clientGUID = clientGUID;
        this.speed = speed;
        this.quality = quality;
        this.xmlDoc = xmlDoc;
        this.addressResolver = addressResolver;
        this.urns = Collections.unmodifiableSet(urns);
        this.replyToMulticast = replyToMulticast;
        this.vendor = vendor;
        this.createTime = createTime;
        this.addressFactory = addressFactory;
    }
    
    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public Credentials getCredentials() {
        FriendPresence presence = addressResolver.getPresence(address);
        if (presence == null) {
            return null;
        }
        // TODO change auth token to type string
        byte[] authToken = presence.getAuthToken();
        if (authToken == null) {
            return null;
        }
        return new UsernamePasswordCredentials(presence.getFriend().getNetwork().getMyID(), StringUtils.getUTF8String(authToken));
    }

    @Override
    public Status getSecureStatus() {
        return Status.INSECURE;
    }

    @Override
    public float getSpamRating() {
        return 0.0f;
    }

    @Override
    public String getUrlPath() {
        URN sha1Urn = getSHA1Urn();
        try {
            return CoreGlueXMPPService.FRIEND_DOWNLOAD_PREFIX + URLEncoder.encode(address.getId(), "UTF-8") + HTTPConstants.URI_RES_N2R + sha1Urn.httpStringValue();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAltLocCapable() {
        return false;
    }

    @Override
    public boolean isFromAlternateLocation() {
        return false;
    }

    @Override
    public boolean isMe(byte[] myClientGUID) {
        return Arrays.equals(clientGUID, myClientGUID);
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public void setSecureStatus(Status secureStatus) {
    }

    @Override
    public void setSpamRating(float rating) {
    }

    @Override
    public RemoteHostMemento toMemento() {
        return new RemoteHostMemento(address, filename, index, clientGUID, speed, size, true, quality, replyToMulticast, getXml(), urns, true, vendor, http11, TYPE, addressFactory);
    }
    
    private String getXml() {
        return xmlDoc != null ? xmlDoc.getXMLString() : null;
    }

    @Override
    public long getCreationTime() {
        return createTime;
    }

    @Override
    public String getFileName() {
        return filename;
    }

    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public URN getSHA1Urn() {
        for (URN urn : urns) {
            if (urn.isSHA1()) {
                return urn;
            }
        }
        throw new IllegalArgumentException(urns + " should have sha1");
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public Set<URN> getUrns() {
        return urns;
    }

    @Override
    public LimeXMLDocument getXMLDocument() {
        return xmlDoc;
    }

    @Override
    public byte[] getClientGUID() {
        return clientGUID;
    }

    @Override
    public int getQuality() {
        return quality;
    }

    @Override
    public int getSpeed() {
        return speed;
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public boolean isBrowseHostEnabled() {
        return true;
    }

    @Override
    public boolean isChatEnabled() {
        return true;
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

}
