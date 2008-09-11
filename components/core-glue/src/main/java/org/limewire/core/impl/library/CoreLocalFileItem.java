package org.limewire.core.impl.library;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.LocalFileDetailsFactory;
import com.limegroup.gnutella.URN;

public class CoreLocalFileItem implements LocalFileItem {

    private final File file;
    private final String name;
    private final long creationTime;
    private final long modifiedTime;
    private final long size;
    private final int numHits;
    private final int numUploads;
    private final Category category;
    private final Map<Keys,Object> map;
    private final FileDesc fileDesc;
    private final LocalFileDetailsFactory detailsFactory;

    public CoreLocalFileItem(FileDesc fileDesc, LocalFileDetailsFactory detailsFactory) {
        this.fileDesc = fileDesc;
        this.detailsFactory = detailsFactory;
        this.file = fileDesc.getFile();
        this.name = fileDesc.getFileName();
        this.creationTime = fileDesc.getCreationTime();
        this.modifiedTime = fileDesc.lastModified();
        this.size = fileDesc.getFileSize();
        this.numHits = fileDesc.getHitCount();
        this.numUploads = fileDesc.getCompletedUploads();
        this.category = getCategory(fileDesc.getFile());
        this.map = Collections.synchronizedMap(new HashMap<Keys,Object>());
        
//        setLimeXMLDocuments(fileDesc.getLimeXMLDocuments());
    }
    
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getLastModifiedTime() {
        return modifiedTime;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getNumHits() {
        return numHits;
    }

    @Override
    public int getNumUploads() {
        return numUploads;
    }

    @Override
    public Category getCategory() {
        return category;
    }
    
    private Category getCategory(File file) {
        String ext = FileUtils.getFileExtension(file);
        MediaType type = MediaType.getMediaTypeForExtension(ext);
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
        }
        return Category.OTHER;   
    }
    
//    private void setLimeXMLDocument(LimeXMLDocument document) {
//        if(document == null)
//            return;
//        document.get
//    }

    @Override
    public Object getProperty(Keys key) {
        return map.get(key);
    }
    
    @Override
    public void setProperty(Keys key, Object value) {
        map.put(key, value);
    }

    public void offer(LimePresence presence) {
        FileDetails details = getFileDetails();
        FileMetaDataImpl fileMetaData = new FileMetaDataImpl();
        fileMetaData.setCreateTime(new Date(details.getCreationTime()));
        fileMetaData.setDescription(""); // TODO
        fileMetaData.setId(details.getSHA1Urn().toString());
        fileMetaData.setIndex(details.getIndex());
        fileMetaData.setName(details.getFileName());
        fileMetaData.setSize(details.getSize());
        fileMetaData.setURIs(copy(details.getUrns()));

        presence.offerFile(fileMetaData);
    }

    private Set<URI> copy(Set<URN> urns) {
        Set<URI> uris = new HashSet<URI>();
        for(URN urn : urns) {
            try {
                uris.add(new URI(urn.toString()));
            } catch (URISyntaxException e) {
                //LOG.debugf(e.getMessage(), e);
            }
        }
        return uris;
    }

    private static  class FileMetaDataImpl implements FileMetaData {
        private enum Element {
            id, name, size, description, index, metadata, uris, createTime
        }

        private final Map<Element, String> data = new HashMap<Element, String>();

        public String getId() {
            return data.get(Element.id);
        }

        public void setId(String id) {
            data.put(Element.id, id);
        }

        public String getName() {
            return data.get(Element.name);
        }

        public void setName(String name) {
            data.put(Element.name, name);
        }

        public long getSize() {
            return Long.valueOf(data.get(Element.size));
        }

        public void setSize(long size) {
            data.put(Element.size, Long.toString(size));
        }

        public String getDescription() {
            return data.get(Element.description);
        }

        public void setDescription(String description) {
            data.put(Element.description, description);
        }

        public long getIndex() {
            return Long.valueOf(data.get(Element.index));
        }

        public void setIndex(long index) {
            data.put(Element.index, Long.toString(index));
        }

        public Map<String, String> getMetaData() {
            // TODO
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Set<URI> getURIs() throws URISyntaxException {
            StringTokenizer st = new StringTokenizer(data.get(Element.uris), " ");
            HashSet<URI> set = new HashSet<URI>();
            while(st.hasMoreElements()) {
                set.add(new URI(st.nextToken()));
            }
            return set;
        }

        public void setURIs(Set<URI> uris) {
            String urisString = "";
            for(URI uri : uris) {
                urisString += uri  + " ";
            }
            data.put(Element.uris, urisString);
        }

        public Date getCreateTime() {
            return new Date(Long.valueOf(data.get(Element.createTime)));
        }

        public void setCreateTime(Date date) {
            data.put(Element.createTime, Long.toString(date.getTime()));
        }

        public String toXML() {
            // TODO StringBuilder instead of concats
            String fileMetadata = "<file>";
            for(Element element : data.keySet()) {
                fileMetadata += "<" + element.toString() + ">";
                fileMetadata += data.get(element);
                fileMetadata += "</" + element.toString() + ">";
            }
            fileMetadata += "</file>";
            return fileMetadata;
        }

    }

    public FileDetails getFileDetails() {
        return detailsFactory.create(fileDesc);
    }
}
