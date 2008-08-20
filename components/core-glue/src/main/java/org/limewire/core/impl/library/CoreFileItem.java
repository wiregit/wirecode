package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.library.FileItem;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.FileDesc;

public class CoreFileItem implements FileItem {

    private final File file;
    private final String name;
    private final long creationTime;
    private final long modifiedTime;
    private final long size;
    private final int numHits;
    private final int numUploads;
    private final Category category;
    private final Map<Keys,Object> map;
    
    public CoreFileItem(FileDesc fileDesc) { 
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

}
