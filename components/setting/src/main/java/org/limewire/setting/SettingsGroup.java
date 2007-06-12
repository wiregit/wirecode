package org.limewire.setting;

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.setting.evt.SettingsGroupEvent;
import org.limewire.setting.evt.SettingsGroupListener;
import org.limewire.setting.evt.SettingsGroupEvent.EventType;

/**
 * Defines an abstract class to reload and save a value, revert to a 
 * default value and mark a value as always saving. 
 * <p>
 * If saving is turned off, then underlying settings will not be saved. If 
 * saving is turned on, then underlying settings still have the option not
 * to save settings to disk.
 */
public abstract class SettingsGroup {
    
    /**
     * List of {@link SettingsGroupListener}s
     */
    private Collection<SettingsGroupListener> listeners;
    
    /**
     * Value for whether or not settings should be saved to file.
     */
    private volatile boolean shouldSave = true;
    
    /**
     * Loads Settings from disk
     */
    public abstract void reload();
    
    /**
     * Saves the current Settings to disk
     */
    public abstract void save();
    
    /**
     * Reverts all Settings to their default values
     */
    public abstract void revertToDefault();
    
    /**
     * Adds the given {@link SettingsGroupListener}
     */
    public void addSettingsGroupListener(SettingsGroupListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsGroupListener is null");
        }
        
        synchronized (this) {
            if (listeners == null) {
                listeners = new ArrayList<SettingsGroupListener>();
            }
            listeners.add(l);
        }        
    }
    
    /**
     * Removes the given {@link SettingsGroupListener}
     */
    public void removeSettingsGroupListener(SettingsGroupListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsGroupListener is null");
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

    /**
     * Returns all {@link SettingsGroupListener}s or null if there are none
     */
    public SettingsGroupListener[] getSettingsGroupListeners() {
        synchronized (this) {
            if (listeners == null) {
                return null;
            }
            
            return listeners.toArray(new SettingsGroupListener[0]);
        }
    }
    
    /**
     * Sets whether or not all Settings should be saved
     */
    public void setShouldSave(boolean shouldSave) {
        if (this.shouldSave != shouldSave) {
            this.shouldSave = shouldSave;
            fireSettingsEvent(EventType.SHOULD_SAVE);
        }
    }
    
    /** 
     * Access for shouldSave
     */
    public boolean getShouldSave() {
        return shouldSave;
    }
    
    /**
     * Fires a SettingsEvent
     */
    protected void fireSettingsEvent(EventType type) {
        fireSettingsEvent(new SettingsGroupEvent(type, this));
    }
    
    /**
     * Fires a SettingsEvent
     */
    protected void fireSettingsEvent(final SettingsGroupEvent evt) {
        if (evt == null) {
            throw new NullPointerException("SettingsEvent is null");
        }
        
        final SettingsGroupListener[] listeners = getSettingsGroupListeners();
        if (listeners != null) {
            Runnable command = new Runnable() {
                public void run() {
                    for (SettingsGroupListener l : listeners) {
                        l.settingsGroupChanged(evt);
                    }
                }
            };
            
            SettingsGroupManager.instance().execute(command);
        }
    }
}
