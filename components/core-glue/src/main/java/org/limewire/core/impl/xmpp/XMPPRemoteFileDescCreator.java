package org.limewire.core.impl.xmpp;

import java.util.Set;

import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.core.api.friend.address.FriendAddress;
import org.limewire.core.api.friend.address.FriendAddressResolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescCreator;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Creates {@link XMPPRemoteFileDesc} for {@link org.limewire.core.api.friend.address.FriendAddress}.
 */
@Singleton
public class XMPPRemoteFileDescCreator implements RemoteFileDescCreator {

    private final AddressFactory addressFactory;
    private final FriendAddressResolver addressResolver;

    @Inject
    public XMPPRemoteFileDescCreator(AddressFactory addressFactory, FriendAddressResolver addressResolver) {
        this.addressFactory = addressFactory;
        this.addressResolver = addressResolver;
    }
    
    @Inject
    void register(RemoteFileDescFactory remoteFileDescFactory) {
        remoteFileDescFactory.register(this);
    }
    
    @Override
    public boolean canCreateFor(Address address) {
        return address instanceof FriendAddress;
    }

    /**
     * Note browseHost and replyToMulticast will be ignored.
     */
    @Override
    public RemoteFileDesc create(Address address, long index, String filename, long size,
            byte[] clientGUID, int speed, int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime, boolean http1) {
        return new XMPPRemoteFileDesc((FriendAddress)address, index, filename, size, clientGUID, speed, quality, xmlDoc, urns, vendor, createTime, true, addressFactory, addressResolver);
    }

}
