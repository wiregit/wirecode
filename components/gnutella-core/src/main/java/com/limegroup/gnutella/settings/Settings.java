package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.*;
import java.io.*;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new public static member to the list
 * of settings.  Each setting constructor takes the name of the key and 
 * the default value, and all settings are typed.  Choose the correct 
 * <tt>Setting</tt> subclass for your setting type.  It is also important
 * to choose a unique string key for your setting name -- otherwise there
 * will be conflicts.
 */
public class Settings {

    /**
     * Make sure this can never be constructed.
     */
    private Settings() {}

    /**
	 * Default name for the properties file.
	 */
    private static final String PROPS_NAME = "limewire.props";

    /**
     * Constant <tt>File</tt> instance for the properties file
     */
    private static final File PROPS_FILE =
        new File(CommonUtils.getUserSettingsDir(), PROPS_NAME);

    /**
     * <tt>Properties</tt> instance for the defualt values.
     */
    protected static final Properties DEFAULT_PROPS = new Properties();


    ////////////////// PLACE ALL SETTINGS BELOW //////////////////

    /**
     * <tt>Setting</tt> for whether or not the statistics window should
     * be enabled.
     */
	//public static BooleanSetting STATISTICS_VIEW_ENABLED = 
    //new BooleanSetting("STATISTICS_VIEW_ENABLED", false);

    
    /**
     * Accessor for the <tt>Properties</tt> instance that stores all settings.
     *
     * @return the <tt>Properties</tt> instance for storing settings
     */
	public static Properties getProperties() {
        return PROPS;
	}

    /**
     * Accessor for the <tt>File</tt> instance that denotes the abstract
     * pathname for the properties file.
     *
     * @return the <tt>File</tt> instance that denotes the abstract
     *  pathname for the properties file
     */
	public static File getPropertiesFile() {
		return PROPS_FILE;
	}

    /**
     * The <tt>Properties</tt> instance containing all settings.
     */
	protected static final Properties PROPS = new Properties(DEFAULT_PROPS);

    /**
     * Static initialization block loads the settings file from disk.  This
     * is done last to ensure that the default values for all settings are
     * already loaded by the time this is called.
     */
    static {
        try {
            FileInputStream fis = new FileInputStream(PROPS_FILE);
            PROPS.load(fis);
            fis.close(); 
        } catch(FileNotFoundException e) {
            // the default properties will be used
        } catch(IOException e) {
            // the default properties will be used            
        }       
    }

    
    /**
     * Private abstract class for an individual setting.  Subclasses of this
     * class provide typing for settings.
     */
    private abstract static class Setting {

        /**
         * The constant key for this property, specified upon construction.
         */
        protected final String KEY;

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
        private Setting(String key, String defaultValue) {
            KEY = key;
            if(DEFAULT_PROPS.containsKey(key)) {
                throw new IllegalArgumentException("duplicate setting key");
            }
            DEFAULT_PROPS.put(KEY, defaultValue);
        }
    }

    /**
     * Class for a boolean setting.
     */
	public static final class BooleanSetting extends Setting {

        /**
         * Creates a new <tt>SettingBool</tt> instance with the specified
         * key and defualt value.
         *
         * @param key the constant key to use for the setting
         * @param defaultBool the default value to use for the setting
         */
        private BooleanSetting(String key, boolean defaultBool) {
			super(key, String.valueOf(defaultBool));
		}
        
        /**
         * Accessor for the value of this setting.
         * 
         * @return the value of this setting
         */
		public boolean getValue() {
			return Boolean.valueOf(PROPS.getProperty(KEY)).booleanValue();
		}

        /**
         * Mutator for this setting.
         *
         * @param bool the <tt>boolean</tt> to store
         */
		public void setValue(boolean bool) {
			PROPS.put(KEY, String.valueOf(bool));
		}
	}

    /**
     * Class for a string setting.
     */
	public static final class StringSetting extends Setting {

        /**
         * Creates a new <tt>SettingBool</tt> instance with the specified
         * key and defualt value.
         *
         * @param key the constant key to use for the setting
         * @param defaultStr the default value to use for the setting
         */
        private StringSetting(String key, String defaultStr) {
			super(key, defaultStr);
		}
        
        /**
         * Accessor for the value of this setting.
         * 
         * @return the value of this setting
         */
		public String getValue() {
            return PROPS.getProperty(KEY);
		}

        /**
         * Mutator for this setting.
         *
         * @param str the <tt>String</tt> to store
         */
		public void setValue(String str) {
			PROPS.put(KEY, str);
		}
	}

    /**
     * Class for an int setting.
     */
	public static final class IntSetting extends Setting {

        /**
         * Creates a new <tt>SettingBool</tt> instance with the specified
         * key and defualt value.
         *
         * @param key the constant key to use for the setting
         * @param defaultInt the default value to use for the setting
         */
        private IntSetting(String key, int defaultInt) {
			super(key, String.valueOf(defaultInt));
		}
        
        /**
         * Accessor for the value of this setting.
         * 
         * @return the value of this setting
         */
		public int getValue() {
            return Integer.parseInt(PROPS.getProperty(KEY));
		}

        /**
         * Mutator for this setting.
         *
         * @param value the value to store
         */
		public void setValue(int value) {
            PROPS.put(KEY, String.valueOf(value));
		}
	}

    /**
     * Class for a byte setting.
     */
	public static final class ByteSetting extends Setting {

        /**
         * Creates a new <tt>SettingBool</tt> instance with the specified
         * key and defualt value.
         *
         * @param key the constant key to use for the setting
         * @param defaultByte the default value to use for the setting
         */
        private ByteSetting(String key, byte defaultByte) {
			super(key, String.valueOf(defaultByte));
		}
        
        /**
         * Accessor for the value of this setting.
         * 
         * @return the value of this setting
         */
		public byte getValue() {
            return Byte.parseByte(PROPS.getProperty(KEY));
		}

        /**
         * Mutator for this setting.
         *
         * @param value the value to store
         */
		public void setValue(byte value) {
            PROPS.put(KEY, String.valueOf(value));
		}
	}

    /**
     * Class for a long setting.
     */
	public static final class LongSetting extends Setting {

        /**
         * Creates a new <tt>SettingBool</tt> instance with the specified
         * key and defualt value.
         *
         * @param key the constant key to use for the setting
         * @param defaultLong the default value to use for the setting
         */
        private LongSetting(String key, long defaultLong) {
			super(key, String.valueOf(defaultLong));
		}
        
        /**
         * Accessor for the value of this setting.
         * 
         * @return the value of this setting
         */
		public long getValue() {
            return Long.parseLong(PROPS.getProperty(KEY));
		}

        /**
         * Mutator for this setting.
         *
         * @param value the value to store
         */
		public void setValue(long value) {
            PROPS.put(KEY, String.valueOf(value));
		}
	}


    /**
     * Class for a file setting.
     */
	public static final class FileSetting extends Setting {

        /**
         * Creates a new <tt>SettingBool</tt> instance with the specified
         * key and defualt value.
         *
         * @param key the constant key to use for the setting
         * @param defaultFile the default value to use for the setting
         */
        private FileSetting(String key, File defaultFile) {
			super(key, defaultFile.getAbsolutePath());
		}
        
        /**
         * Accessor for the value of this setting.
         * 
         * @return the value of this setting
         */
		public File getValue() {
            return new File(PROPS.getProperty(KEY));
		}

        /**
         * Mutator for this setting.
         *
         * @param value the value to store
         */
		public void setValue(File value) {
            PROPS.put(KEY, value.getAbsolutePath());
		}
	}

}
