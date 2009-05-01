package com.limegroup.gnutella.library;

import java.io.File;

import org.limewire.listener.SourcedEvent;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;


public class FileViewChangeEvent implements SourcedEvent<FileView> {
    
    public static enum Type {
        FILE_ADDED, FILE_REMOVED, FILE_CHANGED, FILE_ADD_FAILED, FILE_CHANGE_FAILED, FILES_CLEARED, AUDIO_COLLECTION, VIDEO_COLLECTION, IMAGE_COLLECTION;
    }
    
    private final Type type;
    private final FileView list;
    private final FileDesc newValue;
    private final FileDesc oldValue;
    private final File oldFile;
    private final File newFile;
    private boolean isShared = false;
    
    public FileViewChangeEvent(FileCollection list, Type type) {
        assert type == Type.FILES_CLEARED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newFile = null;
        this.oldFile = null;
        this.newValue = null;
    }
    
    public FileViewChangeEvent(FileCollection list, Type type, File file) {
        assert type == Type.FILE_ADD_FAILED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newFile = Objects.nonNull(file, "file");
        this.oldFile = null;
        this.newValue = null;
    }
    
    public FileViewChangeEvent(FileCollection list, Type type, File oldFile, FileDesc oldValue, File newValue) {
        assert type == Type.FILE_CHANGE_FAILED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = oldValue; // May be null!
        this.newValue = null;
        this.oldFile = Objects.nonNull(oldFile, "oldFile");
        this.newFile = Objects.nonNull(newValue, "file");
    }
    
    public FileViewChangeEvent(FileCollection list, Type type, FileDesc value) {
        assert type == Type.FILE_ADDED || type == Type.FILE_REMOVED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = null;
        this.newValue = Objects.nonNull(value, "value");
        this.oldFile = null;
        this.newFile = Objects.nonNull(newValue.getFile(), "value.getFile()");
    }
    
    public FileViewChangeEvent(FileCollection list, Type type, FileDesc oldValue, FileDesc newValue) {
        assert type == Type.FILE_CHANGED;
        this.type = Objects.nonNull(type, "type");
        this.list = Objects.nonNull(list, "list");
        this.oldValue = Objects.nonNull(oldValue, "oldValue");
        this.newValue = Objects.nonNull(newValue, "newValue");
        this.oldFile = Objects.nonNull(oldValue.getFile(), "oldValue.getFile()");
        this.newFile = Objects.nonNull(newValue.getFile(), "newValue.getFile()");
    }
    
    public FileViewChangeEvent(Type type, boolean state) {
        assert type == Type.AUDIO_COLLECTION || type == Type.VIDEO_COLLECTION || type == Type.IMAGE_COLLECTION;
        this.type = Objects.nonNull(type, "type");
        this.list = null;
        this.oldValue = null;
        this.newFile = null;
        this.oldFile = null;
        this.newValue = null;
        this.isShared = state;
    }
    
    @Override
    public FileView getSource() {
        return list;
    }
    
    public File getOldFile() {
        return oldFile;
    }
    
    public File getFile() {
        return newFile;
    }
    
    public Type getType() {
        return type;
    }
    
    public FileView getFileView() {
        return list;
    }
    
    public FileDesc getFileDesc() {
        return newValue;
    }
    
    public FileDesc getOldValue() {
        return oldValue;
    }
    
    public boolean isShared() {
        return isShared;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
