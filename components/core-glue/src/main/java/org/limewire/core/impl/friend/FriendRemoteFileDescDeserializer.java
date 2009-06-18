package org.limewire.core.impl.friend;

import java.util.Set;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescDeserializer;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
public class FriendRemoteFileDescDeserializer implements RemoteFileDescDeserializer {

    private final AddressFactory addressFactory;
    private final FriendAddressResolver addressResolver;

    @Inject
    public FriendRemoteFileDescDeserializer(AddressFactory addressFactory, FriendAddressResolver addressResolver) {
        this.addressFactory = addressFactory;
        this.addressResolver = addressResolver;
    }
    
    @Override
    @Inject
    public void register(RemoteFileDescFactory remoteFileDescFactory) {
        remoteFileDescFactory.register(FriendRemoteFileDesc.TYPE, this);
    }
    
    @Override
    public RemoteFileDesc createRemoteFileDesc(Address address, long index, String filename,
            long size, byte[] clientGUID, int speed, int quality, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, String vendor, long createTime) {
        return new FriendRemoteFileDesc((FriendAddress)address, index, filename, size, clientGUID, speed, quality, xmlDoc, urns, vendor, createTime, true, addressFactory, addressResolver);
    }

    /**
     * Creates an XMPPRemoteFileDesc clone of a RemoteFileDesc replacing the address, XMPP vitals, and 
     *  linking as necessary.
     *  
     * Not all attributes will be preserved in the promotion process.
     */
    public RemoteFileDesc promoteRemoteFileDescAndExchangeAddress(RemoteFileDesc rfd, FriendAddress address) {
       return new FriendRemoteFileDesc(address, rfd.getIndex(), rfd.getFileName(), rfd.getSize(), rfd.getClientGUID(), rfd.getSpeed(), rfd.getQuality(), rfd.getXMLDocument(), rfd.getUrns(), 
               rfd.getVendor(), rfd.getCreationTime(), rfd.isHTTP11(), addressFactory, addressResolver); 
    }
}
