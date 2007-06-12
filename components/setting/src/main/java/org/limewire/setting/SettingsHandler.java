package org.limewire.setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.setting.evt.SettingsHandlerEvent;
import org.limewire.setting.evt.SettingsHandlerListener;
import org.limewire.setting.evt.SettingsHandlerEvent.EventType;


/**
 * Groups all {@link SettingsGroup} objects in one location to reload, revert to
 * a default value, save, or mark as save-able all <code>Settings</code> 
 * objects at once.
 */
public final class SettingsHandler {
    
    /**
     * The singleton instance of SettingsHandler
     */
    private static final SettingsHandler INSTANCE = new SettingsHandler();
    
    /**
     * Returns the singleton instance of the SettingsHandler
     */
    public static SettingsHandler instance() {
        return INSTANCE;
    }
    
    /**
     * A list of Settings this SettingsHandler is managing
     */
    private final Collection<SettingsGroup> PROPS = Collections.synchronizedList(new ArrayList<SettingsGroup>());

    /**
     * A list of {@link SettingsHandlerListener}s
     */
    private Collection<SettingsHandlerListener> listeners;
    
    /**
     * The Executor for the Events
     */
    private volatile Executor executor = ExecutorsHelper.newFixedSizeThreadPool(1, "SettingsHandlerEventDispatcher");
    
    // never instantiate this class.
    private SettingsHandler() {}
    
    /**
     * Registers a SettingsHandlerListener
     */
    public void addSettingsHandlerListener(SettingsHandlerListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsHandlerListener is null");
        }
        
        synchronized (this) {
            if (listeners == null) {
                listeners = new ArrayList<SettingsHandlerListener>();
            }
            listeners.add(l);
        }        
    }
    
    /**
     * Removes a SettingsHandlerListener
     */
    public void removeSettingsHandlerListener(SettingsHandlerListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsHandlerListener is null");
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
    
    public SettingsHandlerListener[] getSettingsHandlerListeners() {
        synchronized (this) {
            if (listeners == null) {
                return null;
            }
            
            return listeners.toArray(new SettingsHandlerListener[0]);
        }
    }
    
    /**
     * Adds a settings class to the list of factories that 
     * this handler will act upon.
     */
    public void addSettingsGroup(SettingsGroup group) {
        PROPS.add(group);
        fireSettingsHandlerEvent(EventType.SETTINGS_GROUP_ADDED, group);
    }
    
    /**
     * Removes a settings class from the list of factories that
     * this handler will act upon.
     */
    public void removeSettingsGroup(SettingsGroup group) {
        if (PROPS.remove(group)) {
            fireSettingsHandlerEvent(EventType.SETTINGS_GROUP_REMOVED, group);
        }
    }

    /**
     * Reload settings from both the property and configuration files.
     */
    public void reload() {
        synchronized (PROPS) {
            for (SettingsGroup group : PROPS) {
                group.reload();
            }
        }
        
        fireSettingsHandlerEvent(EventType.RELOAD, null);
    }
    
    /**
     * Save property settings to the property file.
     */
    public void save() {
        synchronized (PROPS) {
            for (SettingsGroup group : PROPS) {
                group.save();
            }
        }
        
        fireSettingsHandlerEvent(EventType.SAVE, null);
    }
    
    /**
     * Revert all settings to their default value.
     */
    public void revertToDefault() {
        synchronized (PROPS) {
            for (SettingsGroup group : PROPS) {
                group.revertToDefault();
            }
        }
        
        fireSettingsHandlerEvent(EventType.REVERT_TO_DEFAULT, null);
    }
    
    /**
     * Mutator for shouldSave.
     */
    public void setShouldSave(boolean shouldSave) {
        synchronized (PROPS) {
            for (SettingsGroup group : PROPS) {
                group.setShouldSave(shouldSave);
            }
        }
        
        fireSettingsHandlerEvent(EventType.SHOULD_SAVE, null);
    }
    
    /**
     * Fires a SettingsHandlerEvent
     */
    protected void fireSettingsHandlerEvent(EventType type, SettingsGroup group) {
        fireSettingsHandlerEvent(new SettingsHandlerEvent(type, this, group));
    }
    
    /**
     * Fires a SettingsHandlerEvent
     */
    protected void fireSettingsHandlerEvent(final SettingsHandlerEvent evt) {
        if (evt == null) {
            throw new NullPointerException("SettingsHandlerEvent is null");
        }
        
        final SettingsHandlerListener[] listeners = getSettingsHandlerListeners();
        if (listeners != null) {
            Runnable command = new Runnable() {
                public void run() {
                    for (SettingsHandlerListener l : listeners) {
                        l.settingsHandlerChanged(evt);
                    }
                }
            };
            
            execute(command);
        }
    }
    
    /**
     * Fires a event on the Executor Thread
     */
    protected void execute(Runnable evt) {
        executor.execute(evt);
    }
    
    /**
     * Replaces the current Executor
     */
    public void setExecutor(Executor executor) {
        if (executor == null) {
            throw new NullPointerException("Executor is null");
        }
        
        this.executor = executor;
    }
}    