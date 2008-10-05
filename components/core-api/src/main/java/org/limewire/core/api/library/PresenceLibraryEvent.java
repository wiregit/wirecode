package org.limewire.core.api.library;

import org.limewire.util.StringUtils;

public class PresenceLibraryEvent {

    public enum Type {
        LIBRARY_ADDED, LIBRARY_REMOVED
    }

    private final Type type;
    private final PresenceLibrary library;

    public PresenceLibraryEvent(PresenceLibrary library, Type type) {
        this.library = library;
        this.type = type;
    }

    public PresenceLibrary getLibrary() {
        return library;
    }

    public Type getType() {
        return type;
    }

    public String toString() {
        return StringUtils.toString(this);
    }
}
