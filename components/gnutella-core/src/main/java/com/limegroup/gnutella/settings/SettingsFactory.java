package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.zip.*;
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
public final class SettingsFactory {

    /**
     * <tt>Properties</tt> instance for the defualt values.
     */
    protected final Properties DEFAULT_PROPS;

    /**
     * The <tt>Properties</tt> instance containing all settings.
     */
	protected final Properties PROPS;

	
	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified file
	 * to read from and write to.
	 *
	 * @param settingsFile the file to read from and to write to
	 */
	SettingsFactory(File settingsFile, Properties defaultProps, Properties props) {
		DEFAULT_PROPS = defaultProps;
		PROPS = props;		
		try {
			reload(new FileInputStream(settingsFile));
		} catch(IOException e) {			
			// this should never really happen, so report it
			RouterService.error(e);
		}
	}

	/**
	 * Creates a new <tt>SettingsFactory</tt> instance with the specified stream
	 * to read properties from.
	 *
	 * @param stream the <tt>InputStream</tt> to read properties from
	 */
	SettingsFactory(InputStream stream, Properties defaultProps) {
		DEFAULT_PROPS = defaultProps; 
		PROPS = new Properties(defaultProps);
		reload(stream);
	}

	static SettingsFactory createFromFile(File file, Properties defaultProps) {
		return new SettingsFactory(file, defaultProps, new Properties(defaultProps));
	}

	static SettingsFactory createFromZip(File file, String propsName, 
										 Properties defaultProps) {
		try {
			ZipFile zip = new ZipFile(file, ZipFile.OPEN_READ);
			return new SettingsFactory(zip.getInputStream(zip.getEntry(propsName)),
									   defaultProps);
		} catch(Exception e) {
			System.out.println("unexpected exception in createFromZip::file: "+file+
							   " props name: "+propsName); 
			// this should never really happen, so report it
			RouterService.error(e);
			return null;
		}
	}

	/**
	 * Reloads the settings with the specified settings file from disk.
	 *
	 * @param settingsStream the <tt>InputStream</tt> to load
	 */
	public void reload(InputStream settingsStream) {
        try {
            PROPS.load(settingsStream);
        } catch(IOException e) {
            // the default properties will be used            
        }		
	}


	/**
	 * Creates a new <tt>StringSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public StringSetting createStringSetting(String key, String defaultValue) {
		return new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}

	/**
	 * Creates a new <tt>BooleanSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public BooleanSetting createBooleanSetting(String key, boolean defaultValue) {
		return new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}

	/**
	 * Creates a new <tt>IntSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public IntSetting createIntSetting(String key, int defaultValue) {
		return new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}


	/**
	 * Creates a new <tt>ByteSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public ByteSetting createByteSetting(String key, byte defaultValue) {
		return new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}


	/**
	 * Creates a new <tt>LongSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public LongSetting createLongSetting(String key, long defaultValue) {
		return new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}


	/**
	 * Creates a new <tt>FileSetting</tt> instance with the specified
	 * key and default value.
	 *
	 * @param key the key for the setting
	 * @param defaultValue the default value for the setting
	 */
	public FileSetting createFileSetting(String key, File defaultValue) {
		File parent = new File(defaultValue.getParent());
		if(!parent.isDirectory()) parent.mkdirs();

		return new FileSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
	}
    
//     /**
//      * Private abstract class for an individual setting.  Subclasses of this
//      * class provide typing for settings.
//      */
//     private abstract class Setting {

//         /**
//          * The constant key for this property, specified upon construction.
//          */
//         protected final String KEY;

//         /**
//          * Constructs a new setting with the specified key and default
//          * value.  Private access ensures that only this class can construct
//          * new <tt>Setting</tt>s.
//          *
//          * @param key the key for the setting
//          * @param defaultValue the defaultValue for the setting
//          * @throws <tt>IllegalArgumentException</tt> if the key for this 
//          *  setting is already contained in the map of default settings
//          */
//         private Setting(String key, String defaultValue) {
//             KEY = key;
//             if(DEFAULT_PROPS.containsKey(key)) {
//                 throw new IllegalArgumentException("duplicate setting key");
//             }
//             DEFAULT_PROPS.put(KEY, defaultValue);
//         }
//     }

//     /**
//      * Class for a boolean setting.
//      */
// 	public final class BooleanSetting extends Setting {

//         /**
//          * Creates a new <tt>SettingBool</tt> instance with the specified
//          * key and defualt value.
//          *
//          * @param key the constant key to use for the setting
//          * @param defaultBool the default value to use for the setting
//          */
//         private BooleanSetting(String key, boolean defaultBool) {
// 			super(key, String.valueOf(defaultBool));
// 		}
        
//         /**
//          * Accessor for the value of this setting.
//          * 
//          * @return the value of this setting
//          */
// 		public boolean getValue() {
// 			return Boolean.valueOf(PROPS.getProperty(KEY)).booleanValue();
// 		}

//         /**
//          * Mutator for this setting.
//          *
//          * @param bool the <tt>boolean</tt> to store
//          */
// 		public void setValue(boolean bool) {
// 			PROPS.put(KEY, String.valueOf(bool));
// 		}
// 	}

//     /**
//      * Class for a string setting.
//      */
// 	public final class StringSetting extends Setting {

//         /**
//          * Creates a new <tt>SettingBool</tt> instance with the specified
//          * key and defualt value.
//          *
//          * @param key the constant key to use for the setting
//          * @param defaultStr the default value to use for the setting
//          */
//         private StringSetting(String key, String defaultStr) {
// 			super(key, defaultStr);
// 		}
        
//         /**
//          * Accessor for the value of this setting.
//          * 
//          * @return the value of this setting
//          */
// 		public String getValue() {
//             return PROPS.getProperty(KEY);
// 		}

//         /**
//          * Mutator for this setting.
//          *
//          * @param str the <tt>String</tt> to store
//          */
// 		public void setValue(String str) {
// 			PROPS.put(KEY, str);
// 		}
// 	}

//     /**
//      * Class for an int setting.
//      */
// 	public final class IntSetting extends Setting {

//         /**
//          * Creates a new <tt>SettingBool</tt> instance with the specified
//          * key and defualt value.
//          *
//          * @param key the constant key to use for the setting
//          * @param defaultInt the default value to use for the setting
//          */
//         private IntSetting(String key, int defaultInt) {
// 			super(key, String.valueOf(defaultInt));
// 		}
        
//         /**
//          * Accessor for the value of this setting.
//          * 
//          * @return the value of this setting
//          */
// 		public int getValue() {
//             return Integer.parseInt(PROPS.getProperty(KEY));
// 		}

//         /**
//          * Mutator for this setting.
//          *
//          * @param value the value to store
//          */
// 		public void setValue(int value) {
//             PROPS.put(KEY, String.valueOf(value));
// 		}
// 	}

//     /**
//      * Class for a byte setting.
//      */
// 	public final class ByteSetting extends Setting {

//         /**
//          * Creates a new <tt>SettingBool</tt> instance with the specified
//          * key and defualt value.
//          *
//          * @param key the constant key to use for the setting
//          * @param defaultByte the default value to use for the setting
//          */
//         private ByteSetting(String key, byte defaultByte) {
// 			super(key, String.valueOf(defaultByte));
// 		}
        
//         /**
//          * Accessor for the value of this setting.
//          * 
//          * @return the value of this setting
//          */
// 		public byte getValue() {
//             return Byte.parseByte(PROPS.getProperty(KEY));
// 		}

//         /**
//          * Mutator for this setting.
//          *
//          * @param value the value to store
//          */
// 		public void setValue(byte value) {
//             PROPS.put(KEY, String.valueOf(value));
// 		}
// 	}

//     /**
//      * Class for a long setting.
//      */
// 	public final class LongSetting extends Setting {

//         /**
//          * Creates a new <tt>SettingBool</tt> instance with the specified
//          * key and defualt value.
//          *
//          * @param key the constant key to use for the setting
//          * @param defaultLong the default value to use for the setting
//          */
//         private LongSetting(String key, long defaultLong) {
// 			super(key, String.valueOf(defaultLong));
// 		}
        
//         /**
//          * Accessor for the value of this setting.
//          * 
//          * @return the value of this setting
//          */
// 		public long getValue() {
//             return Long.parseLong(PROPS.getProperty(KEY));
// 		}

//         /**
//          * Mutator for this setting.
//          *
//          * @param value the value to store
//          */
// 		public void setValue(long value) {
//             PROPS.put(KEY, String.valueOf(value));
// 		}
// 	}


//     /**
//      * Class for a file setting.
//      */
// 	public final class FileSetting extends Setting {

//         /**
//          * Creates a new <tt>SettingBool</tt> instance with the specified
//          * key and defualt value.
//          *
//          * @param key the constant key to use for the setting
//          * @param defaultFile the default value to use for the setting
//          */
//         private FileSetting(String key, File defaultFile) {
// 			super(key, defaultFile.getAbsolutePath());
// 		}
        
//         /**
//          * Accessor for the value of this setting.
//          * 
//          * @return the value of this setting
//          */
// 		public File getValue() {
//             return new File(PROPS.getProperty(KEY));
// 		}

//         /**
//          * Mutator for this setting.
//          *
//          * @param value the value to store
//          */
// 		public void setValue(File value) {
//             PROPS.put(KEY, value.getAbsolutePath());
// 		}
// 	}

}
