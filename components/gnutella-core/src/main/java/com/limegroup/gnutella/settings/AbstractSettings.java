package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.util.Properties;
import java.io.*;


/**
 * This class is the superclass for all LimeWire "general" settings classes.
 * General settings are all stored in limewire.props in the appropriate
 * platform-specific directory.  This class allows settings to be conveniently
 * grouped into subclasses by setting type.  For example, a "DownloadSettings"
 * subclass could contain any settings specifically for downloads.
 */
abstract class AbstractSettings {
   

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
    private static final Properties DEFAULT_PROPS = new Properties();

    /**
     * The <tt>Properties</tt> instance containing all settings.
     */
	private static final Properties PROPS = new Properties(DEFAULT_PROPS);

	/**
	 * Constant for the <tt>SettingsFactory</tt> that subclasses can use
	 * to create new settings.
	 */
	protected static final SettingsFactory FACTORY = 
		new SettingsFactory(PROPS_FILE, DEFAULT_PROPS, PROPS);


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
}
