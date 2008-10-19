package com.limegroup.gnutella.library;

import org.limewire.listener.DefaultEvent;

public class FileDescChangeEvent extends DefaultEvent<FileDesc, FileDescChangeEvent.Type> {
    
    public static enum Type { URNS_CHANGED }
    
    public FileDescChangeEvent(FileDesc fileDesc, Type type) {
        super(fileDesc, type);
    }

}
