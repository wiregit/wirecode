package org.limewire.core.impl.search;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.endpoint.RemoteHostAction;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.IpPort;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

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

        // TODO is this correct?
        property.put(PropertyKey.NAME, rfd.getFileName());
        Date date = new Date(rfd.getCreationTime());
        Calendar c = new GregorianCalendar();
        c.setTime(date);
        property.put(PropertyKey.DATE_CREATED, c);
        
//        if(doc == null)
//            return Collections.emptyMap();
        
        if(doc != null) {
            //TODO: clean this implementation up, fix missing fields 
            if(LimeXMLNames.AUDIO_SCHEMA.equals(doc.getSchemaURI())) {
                property.put(PropertyKey.ALBUM_TITLE, doc.getValue(LimeXMLNames.AUDIO_ALBUM));
                property.put(PropertyKey.ARTIST_NAME, doc.getValue(LimeXMLNames.AUDIO_ARTIST));
                property.put(PropertyKey.BITRATE, doc.getValue(LimeXMLNames.AUDIO_BITRATE));
                property.put(PropertyKey.COMMENTS, doc.getValue(LimeXMLNames.AUDIO_COMMENTS));
                property.put(PropertyKey.GENRE, doc.getValue(LimeXMLNames.AUDIO_GENRE));
                property.put(PropertyKey.LENGTH, doc.getValue(LimeXMLNames.AUDIO_SECONDS));
    //            property.put(PropertyKey.SAMPLE_RATE, doc.getValue(LimeXMLNames.AUDIO_));
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

//        property.put(PropertyKey.QUALITY, doc.getValue(LimeXMLNames.AUDIO_TITLE));
//        property.put(PropertyKey.RATING, doc.getValue(LimeXMLNames.AUDIO_TITLE));        
        return property;
    }

    @Override
    public Object getProperty(PropertyKey key) {
        return getProperties().get(key);
    }

    @Override
    public ResultType getResultType() {
        String extension = getFileExtension();
        if (extension != null) {
            MediaType type = MediaType.getMediaTypeForExtension(extension);
            if (type == MediaType.getAudioMediaType()) {
                return ResultType.AUDIO;
            } else if (type == MediaType.getVideoMediaType()) {
                return ResultType.VIDEO;
            } else if (type == MediaType.getImageMediaType()) {
                return ResultType.IMAGE;
            } else if (type == MediaType.getDocumentMediaType()) {
                return ResultType.DOCUMENT;
            }
        }
        
        return ResultType.OTHER;
    }

    public RemoteFileDesc getRfd() {
        return rfd;
    }

    // TODO: RMV Need to implement this still!
    public List<SearchResult> getSimiliarResults() {
        throw new UnsupportedOperationException("Not supported yet.");
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
                        public List<RemoteHostAction> getHostActions() {
                            return Collections.emptyList();
                        }

                        @Override
                        public String getHostDescription() {
                            return rfd.getInetSocketAddress().toString();
                        }
                    };
                } else {
                    return new RemoteHost() {
                        @Override
                        public List<RemoteHostAction> getHostActions() {
                            return Collections.emptyList();
                        }

                        @Override
                        public String getHostDescription() {
                            return locs.get(index - 1).getInetSocketAddress().toString();
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

    public void addSimilarResult(SearchResult result) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}