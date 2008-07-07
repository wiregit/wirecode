package org.limewire.xmpp.client.impl.messages.library;

import java.util.Date;

import org.limewire.xmpp.client.service.FileMetaData;

class FileMetaDataImpl implements FileMetaData {
    private String id;
    private String name;
    private long size;
    private Date date;
    private String description;
    
    FileMetaDataImpl(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

     void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    void setSize(long size) {
        this.size = size;
    }

    public Date getDate() {
        return date;
    }

    void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }
}
