package org.limewire.core.impl.xmpp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.xmpp.FileMetaDataConverter;
import org.limewire.core.impl.friend.FriendRemoteFileDescDeserializer;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.util.DataUtils;

@Singleton
public class FileMetaDataConverterImpl implements FileMetaDataConverter {
    private final FriendRemoteFileDescDeserializer remoteFileDescDeserializer;
    private final RemoteFileDescFactory remoteFileDescFactory;

    @Inject
    public FileMetaDataConverterImpl(FriendRemoteFileDescDeserializer remoteFileDescDeserializer, RemoteFileDescFactory remoteFileDescFactory) {
        this.remoteFileDescDeserializer = remoteFileDescDeserializer;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    public SearchResult create(FriendPresence presence, FileMetaData fileMetaData) throws InvalidDataException, SaveLocationException {
        FriendAddress presenceAddress = getAddressFromPresence(presence);

        RemoteFileDesc remoteFileDesc = createRfdFromChatResult(presenceAddress, fileMetaData);
        RemoteFileDescAdapter remoteFileDescAdapter = new RemoteFileDescAdapter(remoteFileDescDeserializer.promoteRemoteFileDescAndExchangeAddress(remoteFileDesc,
                presenceAddress), IpPort.EMPTY_SET, presence);
        return remoteFileDescAdapter;
    }
    
    private RemoteFileDesc createRfdFromChatResult(FriendAddress address, FileMetaData fileMeta)
            throws SaveLocationException, InvalidDataException {
        byte[] clientGuid = DataUtils.EMPTY_GUID;
        
        Set<String> urnsAsString = fileMeta.getUrns();
        Set<URN> urns = new HashSet<URN>();
        for (String urnStr : urnsAsString) {
            try {
                urns.add(URN.createUrnFromString(urnStr));
            } catch(IOException iox) {
                throw new InvalidDataException(iox);
            }
        }

        return remoteFileDescFactory.createRemoteFileDesc(address,
                fileMeta.getIndex(), fileMeta.getName(), fileMeta.getSize(), clientGuid,
                0, 0, true, null, urns, false, null,
                fileMeta.getCreateTime().getTime());
    }


    /**
     * Get the address if the presence has an address feature at the present moment.
     * Else construct an XMPPAddress with the friendPresence id.
     */
    private FriendAddress getAddressFromPresence(FriendPresence presence) {
        if (presence.hasFeatures(AddressFeature.ID)) {
            return (FriendAddress)((AddressFeature)presence.getFeature(AddressFeature.ID)).getFeature();
        }
        return new FriendAddress(presence.getPresenceId());
    }
    
}
