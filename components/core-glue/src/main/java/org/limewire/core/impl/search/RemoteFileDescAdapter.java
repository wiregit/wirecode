package org.limewire.core.impl.search;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.URNImpl;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.MediaType;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class RemoteFileDescAdapter implements SearchResult {

    private final RemoteFileDesc rfd;
    private final List<IpPort> locs;
    private final Map<PropertyKey, Object> properties;    
    private final Category category;    
    private final String extension;
    private final String fileName;
    
    private volatile FriendPresence friendPresence;    

    public RemoteFileDescAdapter(RemoteFileDesc rfd, QueryReply queryReply,
            Set<? extends IpPort> locs) {
        this.rfd = rfd;
        this.locs = new ArrayList<IpPort>(locs);        
        this.properties = new HashMap<PropertyKey, Object>();
        fileName = rfd.getFileName();
        extension = FileUtils.getFileExtension(rfd.getFileName());
        category = MediaTypeConverter.toCategory(MediaType.getMediaTypeForExtension(extension));

        set(properties, PropertyKey.NAME, FileUtils.getFilenameNoExtension(rfd.getFileName()));
        set(properties, PropertyKey.DATE_CREATED, rfd.getCreationTime());

        LimeXMLDocument doc = rfd.getXMLDocument();
        if (doc != null) {
            if (LimeXMLNames.AUDIO_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, PropertyKey.ALBUM_TITLE, doc.getValue(LimeXMLNames.AUDIO_ALBUM));
                set(properties, PropertyKey.ARTIST_NAME, doc.getValue(LimeXMLNames.AUDIO_ARTIST));
                set(properties, PropertyKey.BITRATE, doc.getValue(LimeXMLNames.AUDIO_BITRATE));
                set(properties, PropertyKey.COMMENTS, doc.getValue(LimeXMLNames.AUDIO_COMMENTS));
                set(properties, PropertyKey.GENRE, doc.getValue(LimeXMLNames.AUDIO_GENRE));
                set(properties, PropertyKey.LENGTH, doc.getValue(LimeXMLNames.AUDIO_SECONDS));
                set(properties, PropertyKey.TRACK_NUMBER, doc.getValue(LimeXMLNames.AUDIO_TRACK));
                set(properties, PropertyKey.YEAR, doc.getValue(LimeXMLNames.AUDIO_YEAR));
                set(properties, PropertyKey.TRACK_NAME, doc.getValue(LimeXMLNames.AUDIO_TITLE));

                Long bitrate = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.AUDIO_BITRATE));
                
                Long length = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.AUDIO_SECONDS));

                int quality = toAudioQualityScore(getFileExtension(), getSize(), bitrate, length);
                if(quality > 0) {
                    set(properties, PropertyKey.QUALITY, quality);
                }

            } else if (LimeXMLNames.VIDEO_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.VIDEO_PRODUCER));
                set(properties, PropertyKey.BITRATE, doc.getValue(LimeXMLNames.VIDEO_BITRATE));
                set(properties, PropertyKey.COMMENTS, doc.getValue(LimeXMLNames.VIDEO_COMMENTS));
                set(properties, PropertyKey.LENGTH, doc.getValue(LimeXMLNames.VIDEO_LENGTH));
                set(properties, PropertyKey.HEIGHT, doc.getValue(LimeXMLNames.VIDEO_HEIGHT));
                set(properties, PropertyKey.WIDTH, doc.getValue(LimeXMLNames.VIDEO_WIDTH));
                set(properties, PropertyKey.YEAR, doc.getValue(LimeXMLNames.VIDEO_YEAR));

                Long bitrate = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_BITRATE));
                Long length = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_LENGTH));
                Long height = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_HEIGHT));
                Long width = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_WIDTH));

                
                int quality = toVideoQualityScore(getFileExtension(),getSize(), bitrate, length, height,
                        width);
                if(quality > 0) {
                    set(properties, PropertyKey.QUALITY, quality);
                }
            } else if (LimeXMLNames.APPLICATION_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, PropertyKey.NAME, doc.getValue(LimeXMLNames.APPLICATION_NAME));
                set(properties, PropertyKey.AUTHOR, doc
                        .getValue(LimeXMLNames.APPLICATION_PUBLISHER));
            } else if (LimeXMLNames.DOCUMENT_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, PropertyKey.NAME, doc.getValue(LimeXMLNames.DOCUMENT_TITLE));
                set(properties, PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.DOCUMENT_AUTHOR));
            } else if (LimeXMLNames.IMAGE_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, PropertyKey.NAME, doc.getValue(LimeXMLNames.IMAGE_TITLE));
                set(properties, PropertyKey.AUTHOR, doc.getValue(LimeXMLNames.IMAGE_ARTIST));
            }
        }
    }

    public List<IpPort> getAlts() {
        return locs;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }
    
    private void set(Map<PropertyKey, Object> map, PropertyKey property, Object value) {
        // Insert nothing if value is null|empty.
        if(value != null && !value.toString().isEmpty()) {
            if(value instanceof String) {
                value = I18NConvert.instance().compose((String)value);
            }
            map.put(property, value);
        }
    }

    @Override
    public Map<PropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(PropertyKey key) {
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
                                return rfd.getInetSocketAddress().toString();
                            }

                            @Override
                            public String getRenderName() {
                                return getName();
                            }

                            @Override
                            public void setName(String name) {

                            }

                            public Network getNetwork() {
                                return null;
                            }
                        };
                    }

                    @Override
                    public Address getPresenceAddress() {
                        return new ConnectableImpl(rfd);
                    }

                    @Override
                    public String getPresenceId() {
                        return getFriend().getId();
                    }

                    public byte[] getAuthToken() {
                        return null;
                    }
                };
            }
        }

        @Override
        public String getRenderName() {
            Friend friend = friendPresence != null ? friendPresence.getFriend()
                    : null;
            if (friend != null) {
                return friend.getRenderName();
            }
            return rfd.getInetSocketAddress().toString();
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

                        public Network getNetwork() {
                            return null;
                        }
                    };
                }

                @Override
                public Address getPresenceAddress() {
                    IpPort ipPort = locs.get(index - 1);
                    if(ipPort instanceof Connectable) {
                        return ((Connectable)ipPort);
                    } else {
                        return new ConnectableImpl(ipPort, false);
                    }
                }

                @Override
                public String getPresenceId() {
                    return getFriend().getId();
                }

                public byte[] getAuthToken() {
                    return null;
                }
            };
        }

        @Override
        public String getRenderName() {
            return getFriendPresence().getFriend().getRenderName();
        }
    }

    @Override
    public String getFileName() {
       return fileName;
    }

    @Override
    public String getMagnetURL() {
        MagnetOptions magnet = MagnetOptions.createMagnet(rfd, rfd.getInetSocketAddress(), rfd.getClientGUID());
        return magnet.toExternalForm();
    }

    /**
     * TODO use a better analysis to map bit rates and file types to quality,
     * for now using the following articles as a guide for now.
     * 
     * http://www.extremetech.com/article2/0,2845,1560793,00.asp
     * 
     * http://www.cdburner.ca/digital-audio-formats-article/digital-audio-
     * comparison.htm
     * 
     * http://ipod.about.com/od/introductiontoitunes/a/sound_qual_test.htm
     * 
     * Returns 1 of 4 quality scores.
     * 
     * 0 - unscored
     * 1 - poor
     * 2 - good
     * 3 - excellent
     */
    private static int toAudioQualityScore(String fileExtension, Long fileSize, Long bitrate, Long length) {
        int quality = 0;
        if ("wav".equalsIgnoreCase(fileExtension) || "flac".equalsIgnoreCase(fileExtension)) {
            quality = 3;
        } else if (bitrate != null) {
            if ("mp3".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 96) {
                    quality = 1;
                } else if (bitrate < 192) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("wma".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("aac".equalsIgnoreCase(fileExtension)
                    || "m4a".equalsIgnoreCase(fileExtension)
                    || "m4b".equalsIgnoreCase(fileExtension)
                    || "m4p".equalsIgnoreCase(fileExtension)
                    || "m4v".equalsIgnoreCase(fileExtension)
                    || "mp4".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("ogg".equalsIgnoreCase(fileExtension)
                    || "ogv".equalsIgnoreCase(fileExtension)
                    || "oga".equalsIgnoreCase(fileExtension)
                    || "ogx".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 48) {
                    quality = 1;
                } else if (bitrate < 96) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if(length != null && length < 30) {
                quality = 1;
            } else if(fileSize != null) {
                if(fileSize < (1 * 1024 * 1024)) {
                    quality = 1;
                } else if(fileSize < (3 * 1024 * 1024)) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            }
        }
        return quality;
    }
   
   /**
    * TODO use a better analysis to map video attributes to quality for now using the following articles as a guide for now.
    * 
    * Right now the scoring is somewhat arbitrary.
    * 
    * Returns 1 of 4 quality scores.
    * 
    * 0 - unscored
    * 1 - poor
    * 2 - good
    * 3 - excellent
    */
    private int toVideoQualityScore(String fileExtension, Long fileSize, Long bitrate, Long length, Long height,
            Long width) {
        int quality = 0;
        
        if( "mpg".equalsIgnoreCase(fileExtension) && height != null && width != null) {
            if((height * width) < (352 * 240)) {
                quality = 1;
            } else if((height * width) < (352 * 480) ) {
                quality = 2;
            } else { 
                quality = 3; 
            }
        } else if( length != null && length < 60) {
            quality = 1;
        } else if (fileSize != null) {
            if(fileSize < (5 * 1024 * 1024)) {
                quality = 1;
            } else if (fileSize < (100 * 1024 * 1024)) {
                quality = 2;
            } else {
                quality = 3;
            }
        } 
        return quality;
    }
}