package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.zip.*;
import java.util.Properties;
import java.util.Enumeration;
import java.io.*;
import java.awt.*;
import com.sun.java.util.collections.*;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new public static member to the list
 * of settings.  Each setting constructor takes the name of the key and 
 * the default value, and all settings are typed.  Choose the correct 
 * <tt>Setting</tt> subclass for your setting type.  It is also important
 * to choose a unique string key for your setting name -- otherwise there
 * will be conflicts.
 */
public final class SettingsFactory {
    
    /**
     * Bytes used to ensure that we can write to the settings file.
     */
    private final byte[] PRE_HEADER = "#LimeWire Properties IO Test\n".getBytes();
    
    /** 
	 * <tt>File</tt> object from which settings are loaded and saved 
	 */    
    private File SETTINGS_FILE;
    
    private final String HEADING;

    /**
     * <tt>Properties</tt> instance for the defualt values.
     */
    protected final Properties DEFAULT_PROPS = new Properties();

    /**
     * The <tt>Properties</tt> instance containing all settings.
     */
	protected final Properties PROPS = new Properties(DEFAULT_PROPS);
    
    /* List of all settings associated with this factory 
     * LOCKING: must hold this monitor
     */
    private ArrayList /* of Settings */ settings = new ArrayList(10);

	
	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified file
	 * to read from and write to.
	 *
	 * @param settingsFile the file to read from and to write to
	 */
	SettingsFactory(File settingsFile) {
        this(settingsFile, "");
    }
    
	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified file
	 * to read from and write to.
	 *
	 * @param settingsFile the file to read from and to write to
     * @param heading heading to use when writing property file
	 */
	SettingsFactory(File settingsFile, String heading) {
        SETTINGS_FILE = settingsFile;
        if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
        HEADING = heading;
		reload();
	}

	/**
	 * Reloads the settings with the predefined settings file from
     * disk.
	 */
	public synchronized void reload() {
		// If the props file doesn't exist, the init sequence will prompt
		// the user for the required values, so return.  If this is not 
		// loading limewire.props, but rather something like themes.txt,
		// we also return, as attempting to load an invalid file will
		// not do any good.
		if(!SETTINGS_FILE.isFile()) return;
        try {
            PROPS.load(new FileInputStream(SETTINGS_FILE));
        } catch(IOException e) {
			ErrorService.error(e);
            // the default properties will be used -- this is fine and expected
        }		
        
        // Reload all setting values
        Iterator ii = settings.iterator(); 
        while (ii.hasNext()) {
            Setting set = (Setting)ii.next();
            set.reload();
        }
	}
	
	/**
	 * Changes the backing file to use for this factory.
	 */
    public synchronized void changeFile(File toUse) {
        SETTINGS_FILE = toUse;
        if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
        revertToDefault();
        reload();
    }
	
	/**
	 * Reverts all settings to their factory defaults.
	 */
	public synchronized void revertToDefault() {
	    Iterator ii = settings.iterator();
	    while( ii.hasNext() ) {
	        Setting set = (Setting)ii.next();
	        set.revertToDefault();
	    }
	}
    
    /**
     * Save setting information to property file
     * We want to NOT save any properties which are the default value.
     * To avoid having to manually encode the file, we copy the props,
     * remove any values which are default, and then save it.
     * (Note that we cannot use 'store' since it's only available in 1.2)
     * Do not call this often, it is expensive.
     * (Perhaps a cheaper way would be to store all the keys that are removed,
     *  remove them, save the properties, then re-add them?)
     */
    public synchronized void save() {
        Properties tempProps = (Properties)PROPS.clone();

        // First scan over it for any settings that are in this factory
        Iterator ii = settings.iterator();
        while( ii.hasNext() ) {
            Setting set = (Setting)ii.next();
            if ( set.isDefault() ) {
                tempProps.remove( set.getKey() );
            }
        }
        // Then rescan over it for settings that are from the old
        // SettingsManager
        String key = null;
        for (Enumeration e = tempProps.keys(); e.hasMoreElements();) {
            key = (String)e.nextElement();
            if ( SettingsManager.instance().isDefault(key) ) {
                tempProps.remove(key);
            }
        }
        
        FileOutputStream out = null;
        try {
            // since we can't use store, we must test to make sure we can
            // write the output (because save doesn't throw an IOException)
            if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
            out = new FileOutputStream(SETTINGS_FILE);
            out.write( PRE_HEADER );
            tempProps.save( out, HEADING);            
        } catch (IOException e) {
			ErrorService.error(e);
        } finally {
            if ( out != null )
            try {
                out.close();
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Return settings properties
     */
    Properties getProperties() {
        return PROPS;
    }

	/**
	 * Creates a new <tt>StringSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized StringSetting createStringSetting(String key, String defaultValue) {
		StringSetting result = 
			new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized BooleanSetting createBooleanSetting(String key, boolean defaultValue) {
		BooleanSetting result = 
			new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

	/**
	 * Creates a new <tt>IntSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized IntSetting createIntSetting(String key, int defaultValue) {
		IntSetting result = 
            new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}


	/**
	 * Creates a new <tt>ByteSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized ByteSetting createByteSetting(String key, byte defaultValue) {
		ByteSetting result = 
                new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}


	/**
	 * Creates a new <tt>LongSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized LongSetting createLongSetting(String key, long defaultValue) {
		 LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
         settings.add(result);
         result.reload();
         return result;
	}


	/**
	 * Creates a new <tt>FileSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized FileSetting createFileSetting(String key, File defaultValue) {
		File parent = new File(defaultValue.getParent());        
		if(!parent.isDirectory()) parent.mkdirs();

		FileSetting result = 
            new FileSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

	/**
	 * Creates a new <tt>ColorSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public synchronized ColorSetting createColorSetting(String key, Color defaultValue) {
		ColorSetting result = 
            ColorSetting.createColorSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
	}

    /**
     * Creates a new <tt>CharArraySetting</tt> instance for a character array 
     * setting with the specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized CharArraySetting 
        createCharArraySetting(String key, char[] defaultValue) {
        
        CharArraySetting result =
            CharArraySetting.createCharArraySetting(DEFAULT_PROPS, PROPS, 
                                                    key, defaultValue);
        settings.add(result);
        result.reload();
        return result;
    }
}
