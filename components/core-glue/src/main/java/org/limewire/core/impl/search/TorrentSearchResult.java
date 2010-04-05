package org.limewire.core.impl.search;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.XMLTorrent;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;

public class TorrentSearchResult implements SearchResult {
    
    private final URI uri;
    private final URI referrer;
    private final URN urn;
    private final BTData torrentData;
    private final File torrentFile;
    private final Torrent torrent;

    public TorrentSearchResult(BTData torrentData,
            URI uri, URI referrer, File torrentFile, Torrent torrent) {
        this.uri = uri;
        this.referrer = referrer;
        this.torrentData = torrentData;
        this.torrentFile = torrentFile;
        this.torrent = torrent;
        try {
            urn = URN.createSHA1UrnFromBytes(torrentData.getInfoHash());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Category getCategory() {
        return Category.TORRENT;
    }

    @Override
    public String getFileExtension() {
        return "torrent";
    }

    @Override
    public String getFileName() {
        return torrentData.getName() + ".torrent";
    }

    @Override
    public String getFileNameWithoutExtension() {
        return torrentData.getName();
    }

    @Override
    public String getMagnetURL() {
        return null;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        switch (key) {
        case FILE_SIZE:
            return getSize();
        case NAME:
            return torrentData.getName();
        case TORRENT:
            return torrent;
        }
        return null;
    }

    @Override
    public float getRelevance(String query) {
        return 0;
    }

    @Override
    public long getSize() {
        if (torrent instanceof XMLTorrent) {
            return ((XMLTorrent)torrent).getTotalSize();
        }
        return 0;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    @Override
    public boolean isLicensed() {
        return false;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    public File getTorrentFile() {
        return torrentFile;
    }

    @Override
    public RemoteHost getSource() {
        return new RemoteHost() {
            
            @Override
            public boolean isSharingEnabled() {
                return false;
            }
            
            @Override
            public boolean isChatEnabled() {
                return false;
            }
            
            @Override
            public boolean isBrowseHostEnabled() {
                return false;
            }
            
            @Override
            public FriendPresence getFriendPresence() {
                return new FriendPresence() {

                    @Override
                    public void addFeature(Feature feature) {
                    }

                    @Override
                    public <D, F extends Feature<D>> void addTransport(Class<F> clazz,
                            FeatureTransport<D> transport) {
                    }

                    @Override
                    public Feature getFeature(URI id) {
                        return null;
                    }

                    @Override
                    public Collection<Feature> getFeatures() {
                        return null;
                    }

                    @Override
                    public Friend getFriend() {
                        return new Friend() {

                            @Override
                            public void addPresenceListener(
                                    EventListener<PresenceEvent> presenceListener) {
                            }

                            @Override
                            public MessageWriter createChat(MessageReader reader) {
                                return null;
                            }

                            @Override
                            public FriendPresence getActivePresence() {
                                return null;
                            }

                            @Override
                            public String getFirstName() {
                                return null;
                            }

                            @Override
                            public String getId() {
                                return null;
                            }

                            @Override
                            public String getName() {
                                return null;
                            }

                            @Override
                            public Network getNetwork() {
                                return null;
                            }

                            @Override
                            public Map<String, FriendPresence> getPresences() {
                                return null;
                            }

                            @Override
                            public String getRenderName() {
                                return null;
                            }

                            @Override
                            public boolean hasActivePresence() {
                                return false;
                            }

                            @Override
                            public boolean isAnonymous() {
                                return true;
                            }

                            @Override
                            public boolean isSignedIn() {
                                return false;
                            }

                            @Override
                            public boolean isSubscribed() {
                                return false;
                            }

                            @Override
                            public void removeChatListener() {
                            }

                            @Override
                            public void setChatListenerIfNecessary(IncomingChatListener listener) {
                            }

                            @Override
                            public void setName(String name) {
                            }
                            
                        };
                    }

                    @Override
                    public Mode getMode() {
                        return null;
                    }

                    @Override
                    public String getPresenceId() {
                        return null;
                    }

                    @Override
                    public int getPriority() {
                        return 0;
                    }

                    @Override
                    public String getStatus() {
                        return null;
                    }

                    @Override
                    public <F extends Feature<D>, D> FeatureTransport<D> getTransport(
                            Class<F> feature) {
                        return null;
                    }

                    @Override
                    public Type getType() {
                        return null;
                    }

                    @Override
                    public boolean hasFeatures(URI... id) {
                        return false;
                    }

                    @Override
                    public void removeFeature(URI id) {
                    }
                    
                };
            }
        };
    }
}
