package org.limewire.setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.limewire.setting.evt.SettingsHandlerEvent;
import org.limewire.setting.evt.SettingsHandlerListener;
import org.limewire.setting.evt.SettingsHandlerEvent.Type;


/**
 * Groups all {@link Settings} objects in one location to reload, revert to
 * a default value, save, or mark as save-able all <code>Settings</code> 
 * objects at once.
 */
public final class SettingsHandler {
    
    private static final SettingsHandler INSTANCE = new SettingsHandler();
    
    public static SettingsHandler instance() {
        return INSTANCE;
    }
    
    private final Collection<Settings> PROPS = Collections.synchronizedList(new ArrayList<Settings>());

    private final Collection<SettingsHandlerListener> listeners = new CopyOnWriteArrayList<SettingsHandlerListener>();
    
    private volatile Executor executor = Executors.newSingleThreadExecutor();
    
    // never instantiate this class.
    private SettingsHandler() {
    }
    
    /**
     * 
     */
    public void addSettingsHandlerListener(SettingsHandlerListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsHandlerListener is null");
        }
        
        listeners.add(l);
    }
    
    /**
     * 
     */
    public void removeSettingsHandlerListener(SettingsHandlerListener l) {
        if (l == null) {
            throw new NullPointerException("SettingsHandlerListener is null");
        }
        
        listeners.remove(l);
    }
    
    /**
     * Adds a settings class to the list of factories that 
     * this handler will act upon.
     */
    public void addSettings(Settings settings) {
        PROPS.add(settings);
        fireSettingsHandlerEvent(Type.SETTINGS_ADDED, settings);
    }
    
    /**
     * Removes a settings class from the list of factories that
     * this handler will act upon.
     */
    public void removeSettings(Settings settings) {
        if (PROPS.remove(settings)) {
            fireSettingsHandlerEvent(Type.SETTINGS_REMOVED, settings);
        }
    }

    /**
     * Reload settings from both the property and configuration files.
     */
    public void reload() {
        synchronized (PROPS) {
            for (Settings settings : PROPS) {
                settings.reload();
            }
        }
        
        fireSettingsHandlerEvent(Type.RELOAD, null);
    }
    
    /**
     * Save property settings to the property file.
     */
    public void save() {
        synchronized (PROPS) {
            for (Settings settings : PROPS) {
                settings.save();
            }
        }
        
        fireSettingsHandlerEvent(Type.SAVE, null);
    }
    
    /**
     * Revert all settings to their default value.
     */
    public void revertToDefault() {
        synchronized (PROPS) {
            for (Settings settings : PROPS) {
                settings.revertToDefault();
            }
        }
        
        fireSettingsHandlerEvent(Type.REVERT_TO_DEFAULT, null);
    }
    
    /**
     * Mutator for shouldSave.
     */
    public void setShouldSave(boolean shouldSave) {
        synchronized (PROPS) {
            for (Settings settings : PROPS) {
                settings.setShouldSave(shouldSave);
            }
        }
        
        fireSettingsHandlerEvent(Type.SHOULD_SAVE, null);
    }
    
    protected void fireSettingsHandlerEvent(Type type, Settings settings) {
        fireSettingsHandlerEvent(new SettingsHandlerEvent(type, this, settings));
    }
    
    protected void fireSettingsHandlerEvent(final SettingsHandlerEvent evt) {
        if (evt == null) {
            throw new NullPointerException("SettingsHandlerEvent is null");
        }
        
        Runnable command = new Runnable() {
            public void run() {
                for (SettingsHandlerListener l : listeners) {
                    l.handleSettingsHandlerEvent(evt);
                }
            }
        };
        
        fireEvent(command);
    }
    
    public void setExecutor(Executor executor) {
        if (executor == null) {
            throw new NullPointerException("Executor is null");
        }
        
        this.executor = executor;
    }
    
    public void fireEvent(Runnable evt) {
        executor.execute(evt);
    }
}    