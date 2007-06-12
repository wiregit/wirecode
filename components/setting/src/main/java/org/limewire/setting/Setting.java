package org.limewire.setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.setting.evt.SettingEvent.EventType;


/**
 * Provides a key-value property as an abstract class. The value is typed
 * in subclasses to avoid casting and ensure your settings are type-safe.
 * The value has a unique key.
 * <p>
 * When you add a new <code>Setting</code> subclass, add a public synchronized 
 * method to {@link SettingsFactory} to create an instance of the setting. 
 * For example, subclass {@link IntSetting}, <code>SettingsFactory</code> has 
 * {@link SettingsFactory#createIntSetting(String, int)} and
 * {@link SettingsFactory#createRemoteIntSetting(String, int, String, int, int)}.
 * <p>
 * <code>Setting</code> includes an abstract method to load a <code>String</code>
 * into the key-value property. You are responsible to convert the 
 * <code>String</code> to the appropriate type in a subclass. 
 * <p>
 * For example, if your subclass is for an integer setting you can
 * have a integer field i.e. <code>myIntValue</code>, in the class. Then you 
 * would set <code>myIntValue</code> with the integer converted 
 * <code>String<code> argument, for example: 
 <pre> 
   protected void loadValue(String sValue) {
        try {
            value = Integer.parseInt(sValue.trim());
        } catch(NumberFormatException nfe) {
            revertToDefault();
        }
    }
 </pre>
 *<p>
 * This class, includes fields for the <code>Setting</code>'s visibility 
 * (public vs. private) and persistence (always save vs don't save). 
 * <p>
 * Visibility and persistence are just fields for a property; what the field 
 * means to your application is up to you. For example, you could give the 
 * setting a "don't save" value and when it's time to store the setting to a 
 * database, you check the setting and take appropriate actions.
 * <p>
 * See {@link SettingsFactory} for an example of creating an 
 * <code>IntSetting</code> object which is a
 * subclass of <code>Setting</code> . Additionally the
 * example shows how to load and save the setting to disk.
 */
public abstract class Setting {
    
	/**
	 * Protected default <tt>Properties</tt> instance for subclasses.
	 */
	protected final Properties DEFAULT_PROPS;

	/**
	 * Protected <tt>Properties</tt> instance containing properties for any
	 * subclasses.
	 */
	protected final Properties PROPS;

	/**
	 * The constant key for this property, specified upon construction.
	 */
	protected final String KEY;

	/**
	 * Constant for the default value for this <tt>Setting</tt>.
	 */
	protected final String DEFAULT_VALUE;
    
	/**
	 * Value for whether or not this setting should always save.
	 */
	private boolean alwaysSave = false;
	
	/**
	 * Setting for whether or not this setting is private and should
	 * not be reported in bug reports.
	 */
	private boolean isPrivate = false;

	/**
	 * List of SettingListener
	 */
    private Collection<SettingListener> listeners = null;
    
	/**
	 * Constructs a new setting with the specified key and default
	 * value.  Private access ensures that only this class can construct
	 * new <tt>Setting</tt>s.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the defaultValue for the setting
	 * @throws <tt>IllegalArgumentException</tt> if the key for this 
	 *  setting is already contained in the map of default settings
	 */
	protected Setting(Properties defaultProps, Properties props, String key, 
                String defaultValue) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;
		KEY = key;
		DEFAULT_VALUE = defaultValue;
		if(DEFAULT_PROPS.containsKey(key)) 
			throw new IllegalArgumentException("duplicate setting key: " + key);
		DEFAULT_PROPS.put(KEY, defaultValue);
        loadValue(defaultValue);
	}
    
	/**
	 * Registers a SettingListener
	 */
	public void addSettingListener(SettingListener l) {
        if (l == null) {
            throw new NullPointerException("SettingListener is null");
        }
        
        synchronized (this) {
            if (listeners == null) {
                listeners = new ArrayList<SettingListener>();
            }
            listeners.add(l);
        }        
    }
    
	/**
	 * Removes a SettingListener
	 */
    public void removeSettingListener(SettingListener l) {
        if (l == null) {
            throw new NullPointerException("SettingListener is null");
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
     * Returns all SettingListeners or null of there are none
     */
    public SettingListener[] getSettingListeners() {
        synchronized (this) {
            if (listeners == null) {
                return null;
            }
            
            return listeners.toArray(new SettingListener[0]);
        }
    }
    
    /**
     * Reload value from properties object
     */
    public void reload() {
        String value = PROPS.getProperty(KEY);
        if (value == null) {
            value = DEFAULT_VALUE;
        }
        loadValue(value);
        fireSettingEvent(EventType.RELOAD);
    }

	/**
	 * Revert to the default value.
	 * It is critically important that the DEFAULT_VALUE is valid,
	 * otherwise an infinite loop will be encountered when revertToDefault
	 * is called, as invalid values call revertToDefault.
	 * Because default values are hard-coded into the program, this is okay.
	 */
	public void revertToDefault() {
        setValue(DEFAULT_VALUE);
        fireSettingEvent(EventType.REVERT_TO_DEFAULT);
	}
	
	/**
	 * Determines whether or not this value should always be saved to disk.
	 */
    public boolean shouldAlwaysSave() {
        return alwaysSave;
    }
    
    /**
     * Sets whether or not this setting should always save, even if
     * it is default.
     * Returns this so it can be used during assignment.
     */
    public Setting setAlwaysSave(boolean alwaysSave) {
        if (this.alwaysSave != alwaysSave) {
            this.alwaysSave = alwaysSave;
            fireSettingEvent(EventType.ALWAYS_SAVE_CHANGED);
        }
        return this;
    }
    
    /**
     * Sets whether or not this setting should be reported in bug reports.
     */
    public Setting setPrivate(boolean isPrivate) {
        if (this.isPrivate != isPrivate) {
            this.isPrivate = isPrivate;
            fireSettingEvent(EventType.PRIVACY_CHANGED);
        }
        return this;
    }
    
    /**
     * Determines whether or not a setting is private.
     */
    public boolean isPrivate() {
        return isPrivate;
    }
	
    /**
     * Determines whether or not the current value is the default value.
     */
    public boolean isDefault() {
        String value = PROPS.getProperty(KEY);
        if (value == null)
            return false;
        return value.equals(DEFAULT_PROPS.getProperty(KEY));
    }
    
    /** Get the key for this setting.     */
    public String getKey() {
        return KEY;
    }
    
    /**  Returns the value as stored in the properties file.    */
    public String getValueAsString() {
        return PROPS.getProperty(KEY);
    }
    
    /**
     * Set new property value
     * @param value new property value
     * 
     * NOTE: This is protected so that only this package
     * can update all kinds of settings using a String value.
     * StringSetting updates the access to public.
     */
    protected void setValue(String value) {
        String old = PROPS.getProperty(KEY);
        if (old == null || !old.equals(value)) {
            PROPS.put(KEY, value);
            loadValue(value);
            fireSettingEvent(EventType.VALUE_CHANGED);
        }
    }

    /**
     * Load value from property string value
     * @param sValue property string value
     */
    abstract protected void loadValue(String sValue);    

    public String toString() {
        return KEY + "=" + getValueAsString();
    }
    
    /**
     * Fires a SettingEvent
     */
    protected void fireSettingEvent(EventType type) {
        fireSettingEvent(new SettingEvent(type, this));
    }
    
    /**
     * Fires a SettingEvent
     */
    protected void fireSettingEvent(final SettingEvent evt) {
        if (evt == null) {
            throw new NullPointerException("SettingEvent is null");
        }
        
        final SettingListener[] listeners = getSettingListeners();
        if (listeners != null) {
            Runnable command = new Runnable() {
                public void run() {
                    for (SettingListener l : listeners) {
                        l.settingChanged(evt);
                    }
                }
            };
            
            SettingsHandler.instance().execute(command);
        }
    }
}
