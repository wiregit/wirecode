package org.limewire.ui.swing.properties;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.listener.SwingSafePropertyChangeSupport;

/**
 * We need this because we don't want to update the real entries
 * if the user hits the cancel button
 */
public class TorrentFileEntryWrapper {
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);

    private TorrentFileEntry entry;
    private int priority;
    
    public TorrentFileEntryWrapper(TorrentFileEntry entry) {
        this.entry = entry;
        this.priority = entry.getPriority();
    }
    
    public String getPath() {
        return entry.getPath();
    }
    
    public long getSize() {
        return entry.getSize();
    }
    
    public float getProgress() {
        return entry.getProgress();
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public TorrentFileEntry getTorrentFileEntry() {
        return entry;
    }
    
    public void setTorrentFileEntry(TorrentFileEntry newEntry) {
        TorrentFileEntry oldEntry = this.entry;
        this.entry = newEntry;
        support.firePropertyChange("entry", oldEntry, newEntry);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }

    public boolean hasChanged() {
        return getPriority() != getTorrentFileEntry().getPriority();
    }
}