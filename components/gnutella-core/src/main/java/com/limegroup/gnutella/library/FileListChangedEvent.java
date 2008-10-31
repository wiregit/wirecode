package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.util.Objects;
import org.limewire.util.StringUtils;


public class FileListChangedEvent {
    
    public static enum Type {
        ADDED, REMOVED, CHANGED, ADD_FAILED, CHANGE_FAILED, CLEAR;
    }
    
    private final Type type;
    private final FileList list;
    private final FileDesc newValue;
    private final FileDesc oldValue;
    private final File file;
    
    public FileListChangedEvent(FileList list, Type type) {
        assert type == Type.CLEAR;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.file = null;
        this.oldValue = null;
        this.newValue = null;
    }
    
    public FileListChangedEvent(FileList list, Type type, File file) {
        assert type == Type.ADD_FAILED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.file = Objects.nonNull(file, "file");
        this.oldValue = null;
        this.newValue = null;
    }
    
    public FileListChangedEvent(FileList list, Type type, FileDesc oldValue, File newValue) {
        assert type == Type.CHANGE_FAILED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.file = Objects.nonNull(newValue, "file");
        this.oldValue = oldValue;
        this.newValue = null;
    }
    
    public FileListChangedEvent(FileList list, Type type, FileDesc value) {
        assert type == Type.ADDED || type == Type.REMOVED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newValue = Objects.nonNull(value, "value");
        this.file = Objects.nonNull(newValue.getFile(), "value.getFile()");
    }
    
    public FileListChangedEvent(FileList list, Type type, FileDesc oldValue, FileDesc newValue) {
        assert type == Type.CHANGED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = Objects.nonNull(oldValue, "oldValue");
        this.newValue = Objects.nonNull(newValue, "newValue");
        this.file = Objects.nonNull(newValue.getFile(), "newValue.getFile()");
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
