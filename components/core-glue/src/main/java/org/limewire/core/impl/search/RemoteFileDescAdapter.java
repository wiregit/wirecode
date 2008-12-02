package org.limewire.core.impl.search;

import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class RemoteFileDescAdapter implements SearchResult {

    private final RemoteFileDesc rfd;
    private final List<IpPort> locs;
    private final Map<FilePropertyKey, Object> properties;    
    private final Category category;    
    private final String extension;
    private final String fileName;
    
    private volatile FriendPresence friendPresence; 

    public RemoteFileDescAdapter(RemoteFileDesc rfd,
                                 Set<? extends IpPort> locs) {
        this.rfd = rfd;
        this.locs = new ArrayList<IpPort>(locs);        
        this.properties = new HashMap<FilePropertyKey, Object>();
        fileName = rfd.getFileName();
        extension = FileUtils.getFileExtension(rfd.getFileName());
        category = CategoryConverter.categoryForExtension(extension);

        LimeXMLDocument doc = rfd.getXMLDocument();
        long fileSize = rfd.getSize();
        FilePropertyKeyPopulator.populateProperties(fileName, fileSize, rfd.getCreationTime(), properties, doc);
    }

    /**
     * TODO come up with a better algorithm, right now just using number of sources and friends.
     * Friends are given a greater weight.
     */
    public int getRelevance() {
        int relevance = 0;
        for(RemoteHost remoteHost : getSources()) {
            if(remoteHost.isAnonymous()) {
                if(remoteHost.isBrowseHostEnabled()) {
                    relevance += 5;
                }
                
                if(remoteHost.isChatEnabled()) {
                    relevance += 2;
                }
                
                if(remoteHost.isSharingEnabled()) {
                    relevance += 10;
                }
                
                relevance++;//TODO might want to drop this line, spammers have lots of sources with nothing enabled.
            } else {
                relevance += 20;
            }
        }
        return relevance;
    }

    public List<IpPort> getAlts() {
        return locs;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }
    
    

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getProperties().get(key);
    }

    @Override
    public Category getCategory() {
        return category;
    }

    public RemoteFileDesc getRfd() {
        return rfd;
    }

    @Override
    public long getSize() {
        return rfd.getSize();
    }
    
    @Override
    public List<RemoteHost> getSources() {
        List<RemoteHost> remoteHosts = new ArrayList<RemoteHost>();
        int maxToAdd = 2;
        int numAdded = 0;
        
        for(RemoteHost remoteHost : buildSources()) {
            boolean anonymous = remoteHost.isAnonymous();
            if(!anonymous) {
                remoteHosts.add(remoteHost);
            } else if(numAdded < maxToAdd) {
                remoteHosts.add(remoteHost);
                numAdded++;
            }
        }
        return remoteHosts;
    }
    
    private List<RemoteHost> buildSources() {
        return new AbstractList<RemoteHost>() {
            @Override
            public RemoteHost get(final int index) {
                if (index == 0) {
                    return new RfdRemoteHost();
                } else {
                    return new AltLocRemoteHost(index);
                }
            }

            @Override
            public int size() {
                return 1 + locs.size();
            }
        };
    }

    @Override
    public URN getUrn() {
        return new URNImpl(rfd.getSHA1Urn());
    }

    @Override
    public boolean isSpam() {
        return rfd.isSpam();
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    public FriendPresence getFriendPresence() {
        return friendPresence;
    }

    public void setFriendPresence(FriendPresence friendPresence) {
        this.friendPresence = friendPresence;
    }    
    
    private final class RfdRemoteHost implements RemoteHost {
        @Override
        public boolean isBrowseHostEnabled() {
            return rfd.isBrowseHostEnabled();
        }

        @Override
        public boolean isChatEnabled() {
            if (friendPresence != null && !friendPresence.getFriend().isAnonymous()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            if (friendPresence != null && !friendPresence.getFriend().isAnonymous()) {
                return true;
            }

            return false;
        }

        public FriendPresence getFriendPresence() {
            if (friendPresence != null) {
                return friendPresence;
            } else {
                final Map<URI, Feature> features = new HashMap<URI, Feature>();
                features.put(AddressFeature.ID, new AddressFeature(rfd.getAddress()));
                // create dummy friend presence
                return new FriendPresence() {

                    @Override
                    public Friend getFriend() {
                        return new Friend() {
                            @Override
                            public boolean isAnonymous() {
                                return true;
                            }
                            
                            @Override
                            public String getId() {
                                return GUID.toHexString(rfd.getClientGUID()); 
                            }

                            @Override
                            public String getName() {
                                return rfd.getAddress().getAddressDescription();
                            }

                            @Override
                            public String getRenderName() {
                                return getName();
                            }

                            @Override
                            public void setName(String name) {

                            }

                            @Override
                            public Network getNetwork() {
                                return null; 
                            }

                            public Map<String, FriendPresence> getFriendPresences() {
                                return Collections.emptyMap();
                            }
                        };
                    }

                    @Override
                    public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
                        return new EventListenerList<FeatureEvent>();
                    }

                    public Collection<Feature> getFeatures() {
                        return features.values();
                    }

                    public Feature getFeature(URI id) {
                        return features.get(id);
                    }

                    public void addFeature(Feature feature) {
                        features.put(feature.getID(), feature);
                    }

                    public void removeFeature(URI id) {
                        features.remove(id);
                    }

                    public boolean hasFeatures(URI... id) {
                        for(URI uri : id) {
                            if(getFeature(uri) == null) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public String getPresenceId() {
                        return getFriend().getId();
                    }

                };
            }
        }

        @Override
        public String getRenderName() {
            Friend friend = friendPresence != null ?
                    friendPresence.getFriend() : null;
            if (friend != null) {
                return friend.getRenderName();
            }
            return rfd.getAddress().getAddressDescription();
        }

        @Override
        public boolean isAnonymous() {
            return getFriendPresence().getFriend().isAnonymous();
        }
    }
    
    
    private final class AltLocRemoteHost implements RemoteHost {
        private final int index;

        private AltLocRemoteHost(int index) {
            this.index = index;
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
        public boolean isSharingEnabled() {
            return false;
        }

        @Override
        public FriendPresence getFriendPresence() {
            final Map<URI, Feature> features = new HashMap<URI, Feature>();
            IpPort ipPort = locs.get(index - 1);
            Address address;
            if(ipPort instanceof Connectable) {
                address = ((Connectable)ipPort);
            } else {
                address = new ConnectableImpl(ipPort, false);
            }
            features.put(AddressFeature.ID, new AddressFeature(address));
            // create dummy friend presence
            return new FriendPresence() {

                @Override
                public Friend getFriend() {
                    return new Friend() {
                        @Override
                        public boolean isAnonymous() {
                            return true;
                        }

                        @Override
                        public String getId() {
                            return locs.get(index - 1).getInetSocketAddress()
                                    .toString();
                        }

                        @Override
                        public String getName() {
                            return getRenderName();
                        }

                        @Override
                        public String getRenderName() {
                            return getId();
                        }

                        @Override
                        public void setName(String name) {

                        }

                        @Override
                        public Network getNetwork() {
                            return null;
                        }

                        public Map<String, FriendPresence> getFriendPresences() {
                            return Collections.emptyMap();
                        }
                    };
                }

                @Override
                public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
                    return new EventListenerList<FeatureEvent>();
                }

                @Override
                public Collection<Feature> getFeatures() {
                    return features.values();
                }

                @Override
                public Feature getFeature(URI id) {
                    return features.get(id);
                }

                @Override
                public void addFeature(Feature feature) {
                    features.put(feature.getID(), feature);
                }

                @Override
                public void removeFeature(URI id) {
                    features.remove(id);
                }

                @Override
                public boolean hasFeatures(URI... id) {
                    for(URI uri : id) {
                        if(getFeature(uri) == null) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public String getPresenceId() {
                    return getFriend().getId();
                }
            };
        }

        @Override
        public String getRenderName() {
            return getFriendPresence().getFriend().getRenderName();
        }

        @Override
        public boolean isAnonymous() {
            return getFriendPresence().getFriend().isAnonymous();
        }
    }

    @Override
    public String getFileName() {
       return fileName;
    }

    @Override
    public String getMagnetURL() {
        MagnetOptions magnet = MagnetOptions.createMagnet(rfd, null, rfd.getClientGUID());
        return magnet.toExternalForm();
    }

}