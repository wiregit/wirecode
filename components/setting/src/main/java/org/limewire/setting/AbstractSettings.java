package org.limewire.setting;

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.setting.evt.SettingsEvent;
import org.limewire.setting.evt.SettingsListener;
import org.limewire.setting.evt.SettingsEvent.EventType;

public abstract class AbstractSettings implements Settings {
    
    /**
     * List of SettingsListeners
     */
    private Collection<SettingsListener> listeners;
    
    /**
     * Value for whether or not settings should be saved to file.
     */
    private volatile boolean shouldSave = true;
    
    /*
     * (non-Javadoc)
     * @see org.limewire.setting.Settings#addSettingsListener(org.limewire.setting.evt.SettingsListener)
     */
    public void addSettingsListener(SettingsListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsListener is null");
        }
        
        synchronized (this) {
            if (listeners == null) {
                listeners = new ArrayList<SettingsListener>();
            }
            listeners.add(l);
        }        
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.setting.Settings#removeSettingsListener(org.limewire.setting.evt.SettingsListener)
     */
    public void removeSettingsListener(SettingsListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsListener is null");
        }
        
        synchronized (this) {
            if (listeners != null) {
                listeners.remove(l);
                if (listeners.isEmpty()) {
                    listeners = null;
                }
            }
        }
    }

    public SettingsListener[] getSettingsListeners() {
        synchronized (this) {
            if (listeners == null) {
                return null;
            }
            
            return listeners.toArray(new SettingsListener[0]);
        }
    }
    
    /** Mutator for shouldSave     */
    public void setShouldSave(boolean shouldSave) {
        if (this.shouldSave != shouldSave) {
            this.shouldSave = shouldSave;
            fireSettingsEvent(EventType.SHOULD_SAVE);
        }
    }
    
    /** Access for shouldSave     */
    public boolean getShouldSave() {
        return shouldSave;
    }
    
    /**
     * Fires a SettingsEvent
     */
    protected void fireSettingsEvent(EventType type) {
        fireSettingsEvent(new SettingsEvent(type, this));
    }
    
    /**
     * Fires a SettingsEvent
     */
    protected void fireSettingsEvent(final SettingsEvent evt) {
        if (evt == null) {
            throw new NullPointerException("SettingsEvent is null");
        }
        
        final SettingsListener[] listeners = getSettingsListeners();
        if (listeners != null) {
            Runnable command = new Runnable() {
                public void run() {
                    for (SettingsListener l : listeners) {
                        l.settingsChanged(evt);
                    }
                }
            };
            
            SettingsHandler.instance().execute(command);
        }
    }
}
