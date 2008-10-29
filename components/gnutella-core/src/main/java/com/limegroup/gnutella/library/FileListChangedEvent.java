package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.util.StringUtils;


public class FileListChangedEvent {
    
    public static enum Type {
        ADDED, REMOVED, CHANGED, ADD_FAILED, CHANGE_FAILED;
    }
    
    private final Type type;
    private final FileList list;
    private final FileDesc newValue;
    private final FileDesc oldValue;
    private final File file;
    
    public FileListChangedEvent(FileList list, Type type, File file) {
        this.type = type;
        this.list = list;
        this.file = file;
        this.oldValue = null;
        this.newValue = null;
    }
    
    public FileListChangedEvent(FileList list, Type type, FileDesc value) {
        this(list, type, null, value);
    }
    
    public FileListChangedEvent(FileList list, Type type, FileDesc oldValue, FileDesc newValue) {
        this.type = type;
        this.list = list;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.file = newValue.getFile();
    }
    
    public File getFile() {
        return file;
    }
    
    public Type getType() {
        return type;
    }
    
    public FileList getList() {
        return list;
    }
    
    public FileDesc getFileDesc() {
        return newValue;
    }
    
    public FileDesc getOldValue() {
        return oldValue;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
