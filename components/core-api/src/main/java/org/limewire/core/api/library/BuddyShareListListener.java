package org.limewire.core.api.library;

public interface BuddyShareListListener {

    public static enum BuddyShareEvent {ADD, REMOVE}
    
    public void handleBuddyShareEvent(BuddyShareEvent event, String name);
}
