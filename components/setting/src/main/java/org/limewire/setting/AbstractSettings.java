package org.limewire.setting;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.setting.evt.SettingsEvent;
import org.limewire.setting.evt.SettingsListener;
import org.limewire.setting.evt.SettingsEvent.Type;

public abstract class AbstractSettings implements Settings {
    
    /**
     * 
     */
    private final Collection<SettingsListener> listeners = new CopyOnWriteArrayList<SettingsListener>();
    
    /**
     * Value for whether or not settings should be saved to file.
     */
    private volatile boolean shouldSave = true;
    
    public void addSettingsListener(SettingsListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsListener is null");
        }
        
        listeners.add(l);
    }
    
    /** Mutator for shouldSave     */
    public void setShouldSave(boolean shouldSave) {
        if (this.shouldSave != shouldSave) {
            this.shouldSave = shouldSave;
            fireSettingsEvent(Type.SHOULD_SAVE);
        }
    }
    
    /** Access for shouldSave     */
    public boolean getShouldSave() {
        return shouldSave;
    }
    
    public void removeSettingsListener(SettingsListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsListener is null");
        }
        
        listeners.remove(l);
    }
    
    protected void fireSettingsEvent(Type type) {
        fireSettingsEvent(new SettingsEvent(type, this));
    }
    
    protected void fireSettingsEvent(final SettingsEvent evt) {
        if (evt == null) {
            throw new NullPointerException("SettingsEvent is null");
        }
        
        Runnable command = new Runnable() {
            public void run() {
                for (SettingsListener l : listeners) {
                    l.handleSettingsEvent(evt);
                }
            }
        };
        
        SettingsHandler.instance().fireEvent(command);
    }
}
