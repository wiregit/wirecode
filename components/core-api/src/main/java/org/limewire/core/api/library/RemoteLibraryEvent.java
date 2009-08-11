package org.limewire.core.api.library;

import java.util.Collection;
import java.util.Collections;

import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.DefaultSourceTypeEvent;

public class RemoteLibraryEvent extends DefaultSourceTypeEvent<RemoteLibrary, RemoteLibraryEvent.Type> {

    private final RemoteLibraryState state;
    private final Collection<SearchResult> addedFiles;

    public static enum Type { FILES_CLEARED, FILES_ADDED, STATE_CHANGED }
    
    private RemoteLibraryEvent(RemoteLibrary source, Type type, Collection<SearchResult> addedFiles) { 
        super(source, type);
        this.state = source.getState();
        this.addedFiles = addedFiles;
    }
    
    public static RemoteLibraryEvent createStateChangedEvent(RemoteLibrary remoteLibrary) {
        return new RemoteLibraryEvent(remoteLibrary, Type.STATE_CHANGED, Collections.<SearchResult>emptyList());
    }
    
    public static RemoteLibraryEvent createFilesClearedEvent(RemoteLibrary remoteLibrary) {
        return new RemoteLibraryEvent(remoteLibrary, Type.FILES_CLEARED, Collections.<SearchResult>emptyList());
    }
    
    public static RemoteLibraryEvent createFilesAddedEvent(RemoteLibrary remoteLibrary, Collection<SearchResult> addedFiles) {
        return new RemoteLibraryEvent(remoteLibrary, Type.FILES_ADDED, addedFiles);
    }
    
    public RemoteLibraryState getState() {
        return state;
    }
    
    public Collection<SearchResult> getAddedFiles() {
        return addedFiles;
    }
}
