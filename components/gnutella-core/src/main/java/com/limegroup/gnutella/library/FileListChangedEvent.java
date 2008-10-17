package com.limegroup.gnutella.library;


public class FileListChangedEvent {
    
    public static enum Type { ADDED, REMOVED, CHANGED }
    
    private final Type type;
    private final FileList list;
    private final FileDesc newValue;
    private final FileDesc oldValue;
    
    public FileListChangedEvent(FileList list, Type type, FileDesc value) {
        this(list, type, null, value);
    }
    
    public FileListChangedEvent(FileList list, Type type, FileDesc oldValue, FileDesc newValue) {
        this.type = type;
        this.list = list;
        this.oldValue = oldValue;
        this.newValue = newValue;
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

}
