package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.impl.InvalidURN;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.LocalFileDetailsFactory;

class CoreLocalFileItem implements LocalFileItem , Comparable {

    private final Category category;

    private Map<FilePropertyKey, Object> propertiesMap;  // TODO this is a shallow copy of LimeXMLDocument.fieldToValue

    private final FileDesc fileDesc;

    private final LocalFileDetailsFactory detailsFactory;

    private final CreationTimeCache creationTimeCache;

    @Inject
    public CoreLocalFileItem(@Assisted FileDesc fileDesc, LocalFileDetailsFactory detailsFactory,
            CreationTimeCache creationTimeCache) {
        this.fileDesc = fileDesc;
        this.detailsFactory = detailsFactory;
        this.creationTimeCache = creationTimeCache;
        this.category = CategoryConverter.categoryForFile(fileDesc.getFile());
    }

    /**
     * Lazily builds the properties map for this local file item.
     */
    private Map<FilePropertyKey, Object> getPropertiesMap() {
        synchronized (this) {
            if(propertiesMap == null) {
                reloadProperties();
            }
            return propertiesMap;
        }
    }

    @Override
    public long getCreationTime() {
        URN sha1 = fileDesc.getSHA1Urn();
        if(sha1 != null) {
            return creationTimeCache.getCreationTimeAsLong(sha1);
        } else {
            return -1;
        }
    }

    @Override
    public File getFile() {
        return fileDesc.getFile();
    }

    @Override
    public long getLastModifiedTime() {
        return fileDesc.lastModified();
    }

    @Override
    public String getName() {
        return FileUtils.getFilenameNoExtension(fileDesc.getFileName());
    }

    @Override
    public long getSize() {
        return fileDesc.getFileSize();
    }

    @Override
    public int getNumHits() {
        return fileDesc.getHitCount();
    }

    @Override
    public int getNumUploads() {
        return fileDesc.getCompletedUploads();
    }
    
    @Override
    public int getNumUploadAttempts() {
        return getFileDesc().getAttemptedUploads();
    }  

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getPropertiesMap().get(key);
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }

    @Override
    public FileMetaData toMetadata() {
        FileMetaDataImpl fileMetaData = new FileMetaDataImpl();
        fileMetaData.setCreateTime(new Date(getCreationTime()));
        fileMetaData.setDescription(""); // TODO
        fileMetaData.setId(fileDesc.getSHA1Urn().toString());
        fileMetaData.setIndex(fileDesc.getIndex());
        fileMetaData.setName(fileDesc.getFileName());
        fileMetaData.setSize(fileDesc.getFileSize());
        Set<String> urns = new HashSet<String>();
        for(URN urn : fileDesc.getUrns()) {
            urns.add(urn.toString());
        }
        fileMetaData.setURNs(urns);
        return fileMetaData;
    }

    public FileDetails getFileDetails() {
        return detailsFactory.create(fileDesc);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFile() == null) ? 0 : getFile().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return getFile().equals(((CoreLocalFileItem) obj).getFile());
    }

    @Override
    public String toString() {
        return "CoreLocalFileItem for: " + fileDesc;
    }

    @Override
    public String getFileName() {
        return fileDesc.getFileName();
    }

    @Override
    public boolean isShareable() {
        return !InvalidURN.instance.equals(getUrn()) && !fileDesc.isStoreFile() && !isIncomplete();
    }
    
    @Override
    public org.limewire.core.api.URN getUrn() {
        URN urn = fileDesc.getSHA1Urn();
        if(urn != null) {
            return new URNImpl(urn);
        } else {
            return InvalidURN.instance;
        }
    }

    @Override
    public boolean isIncomplete() {
        return fileDesc instanceof IncompleteFileDesc;
    }

    @Override
    public void setProperty(FilePropertyKey key, Object value) {
        getPropertiesMap().put(key, value);
    }

    public FileDesc getFileDesc() {
        return fileDesc;
    }

    @Override
    public int compareTo(Object obj) {
        if (getClass() != obj.getClass()) {
            return -1;
        }
        return Objects.compareToNullIgnoreCase(getFileName(), ((CoreLocalFileItem) obj).getFileName(), true);
    }
    
    /**
     * Reloads the properties map to whatever values are stored in the
     * LimeXmlDocs for this file.
     */
    public void reloadProperties() {
        synchronized (this) {
            Map<FilePropertyKey, Object> reloadedMap = Collections.synchronizedMap(new HashMap<FilePropertyKey, Object>());
            FilePropertyKeyPopulator.populateProperties(fileDesc.getFileName(), fileDesc.getFileSize(), 
                    getCreationTime(), reloadedMap, fileDesc.getXMLDocument());
            reloadedMap.put(FilePropertyKey.LOCATION, getFile().getParent());
            propertiesMap = reloadedMap;
        }
    }

    @Override
    public boolean isLoaded() {
        return !InvalidURN.instance.equals(getUrn());
    }
    
}
