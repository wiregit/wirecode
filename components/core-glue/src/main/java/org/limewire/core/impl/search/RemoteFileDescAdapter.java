package org.limewire.core.impl.search;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class RemoteFileDescAdapter implements SearchResult {

    private final RemoteFileDesc rfd;
    private final List<IpPort> locs;

    public RemoteFileDescAdapter(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        this.rfd = rfd;
        this.locs = new ArrayList<IpPort>(locs);
    }
    
    public List<IpPort> getAlts() {
        return locs;
    }

    @Override
    public String getDescription() {
        return rfd.toString();
    }

    @Override
    public String getFileExtension() {
        return FileUtils.getFileExtension(rfd.getFileName());
    }

    @Override
    public Map<PropertyKey, Object> getProperties() {
        Map<PropertyKey, Object> property = new HashMap<PropertyKey, Object>();
        LimeXMLDocument doc = rfd.getXMLDocument();

        property.put(PropertyKey.NAME, rfd.getFileName());
        property.put(PropertyKey.DATE_CREATED, rfd.getCreationTime());
        
        if(doc != null) { 
            if(LimeXMLNames.AUDIO_SCHEMA.equals(doc.getSchemaURI())) {
                property.put(PropertyKey.ALBUM_TITLE, doc.getValue(LimeXMLNames.AUDIO_ALBUM));
                property.put(PropertyKey.ARTIST_NAME, doc.getValue(LimeXMLNames.AUDIO_ARTIST));
                property.put(PropertyKey.BITRATE, doc.getValue(LimeXMLNames.AUDIO_BITRATE));
                property.put(PropertyKey.COMMENTS, doc.getValue(LimeXMLNames.AUDIO_COMMENTS));
                property.put(PropertyKey.GENRE, doc.getValue(LimeXMLNames.AUDIO_GENRE));
                property.put(PropertyKey.LENGTH, doc.getValue(LimeXMLNames.AUDIO_SECONDS));
                property.put(PropertyKey.TRACK_NUMBER, doc.getValue(LimeXMLNames.AUDIO_TRACK));
                property.put(PropertyKey.YEAR, doc.getValue(LimeXMLNames.AUDIO_YEAR));
            } else if(LimeXMLNames.VIDEO_SCHEMA.equals(doc.getSchemaURI())) {
                property.put(PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.AUDIO_ARTIST));
                property.put(PropertyKey.BITRATE, doc.getValue(LimeXMLNames.VIDEO_BITRATE));
                property.put(PropertyKey.COMMENTS, doc.getValue(LimeXMLNames.VIDEO_COMMENTS));
                property.put(PropertyKey.LENGTH, doc.getValue(LimeXMLNames.VIDEO_LENGTH));
                property.put(PropertyKey.HEIGHT, doc.getValue(LimeXMLNames.VIDEO_HEIGHT));
                property.put(PropertyKey.WIDTH, doc.getValue(LimeXMLNames.VIDEO_WIDTH));
                property.put(PropertyKey.YEAR, doc.getValue(LimeXMLNames.VIDEO_YEAR));            
            } else if(LimeXMLNames.APPLICATION_SCHEMA.equals(doc.getSchemaURI())) {
                property.put(PropertyKey.NAME, doc.getValue(LimeXMLNames.APPLICATION));
                property.put(PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.APPLICATION_PUBLISHER));
            } else if(LimeXMLNames.DOCUMENT_SCHEMA.equals(doc.getSchemaURI())) {
                property.put(PropertyKey.NAME, doc.getValue(LimeXMLNames.DOCUMENT));
                property.put(PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.DOCUMENT_AUTHOR));
            } else if(LimeXMLNames.IMAGE_SCHEMA.equals(doc.getSchemaURI())) {
                property.put(PropertyKey.NAME, doc.getValue(LimeXMLNames.IMAGE));
                property.put(PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.IMAGE_ARTIST));
            }
        }
  
        return property;
    }

    @Override
    public Object getProperty(PropertyKey key) {
        return getProperties().get(key);
    }

    @Override
    public Category getCategory() {
        String extension = getFileExtension();
        MediaType type = MediaType.getMediaTypeForExtension(extension);
        if (type == MediaType.getAudioMediaType()) {
            return Category.AUDIO;
        } else if (type == MediaType.getVideoMediaType()) {
            return Category.VIDEO;
        } else if (type == MediaType.getImageMediaType()) {
            return Category.IMAGE;
        } else if (type == MediaType.getDocumentMediaType()) {
            return Category.DOCUMENT;
        } else if (type == MediaType.getProgramMediaType()) {
            return Category.PROGRAM;
        } else {
            return Category.OTHER;
        }
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
        return new AbstractList<RemoteHost>() {
            @Override
            public RemoteHost get(final int index) {
                if (index == 0) {
                    return new RemoteHost() {
                        @Override
                        public String getId() {
                            return GUID.toHexString(rfd.getClientGUID());
                        }
                        
                        @Override
                        public String getHostDescription() {
                            return rfd.getInetSocketAddress().toString();
                        }

                        @Override
                        public Address getAddress() {
                            return rfd.toAddress();
                        }

                        @Override
                        public String getName() {
                            // TODO tie in with jabber code, if buddy return
                            // buddies name
                            return getHostDescription();
                        }

                        @Override
                        public boolean isBrowseHostEnabled() {
                            return rfd.isBrowseHostEnabled();
                        }

                        @Override
                        public boolean isChatEnabled() {
                            // TODO tie in with jabber code
                            // if buddy enable
                            return false;
                        }

                        @Override
                        public boolean isSharedFiles() {
                            // TODO tie in with jabber code
                            // if buddy enable
                            return false;
                        }
                    };
                } else {
                    return new RemoteHost() {
                        @Override
                        public String getId() {
                            return getHostDescription();
                        }
                        
                        @Override
                        public String getHostDescription() {
                            return locs.get(index - 1).getInetSocketAddress().toString();
                        }

                        @Override
                        public Address getAddress() {
                            Connectable connectable = null;
                            IpPort ipPort = locs.get(index - 1);
                            if (ipPort instanceof Connectable) {
                                connectable = (Connectable) ipPort;
                            } else {
                                connectable = new ConnectableImpl(ipPort, false);
                            }
                            return connectable;
                        }

                        @Override
                        public String getName() {
                            // TODO tie in with jabber code, if buddy return
                            // buddies name
                            return getHostDescription();
                        }

                        @Override
                        public boolean isBrowseHostEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isChatEnabled() {
                            // TODO tie in with jabber code
                            // if buddy enable
                            return false;
                        }

                        @Override
                        public boolean isSharedFiles() {
                            // TODO tie in with jabber code
                            // if buddy enable
                            return false;
                        }
                    };
                }
            }

            @Override
            public int size() {
                return 1 + locs.size();
            }
        };
    }

    @Override
    public String getUrn() {
        return rfd.getSHA1Urn().toString();
    }

    @Override
    public boolean isSpam() {
        return rfd.isSpam();
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    
}