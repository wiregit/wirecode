package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.*;
import java.io.*;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new public static member to the list
 * of settings.  Construct settings using the <tt>FACTORY</tt> instance
 * from the <tt>AbstractSettings</tt> superclass.  Each setting factory 
 * constructor takes the name of the key and the default value, and all 
 * settings are typed.  Choose the correct <tt>Setting</tt> factory constructor 
 * for your setting type.  It is also important to choose a unique string key 
 * for your setting name -- otherwise there will be conflicts, and a runtime
 * exception will be thrown.
 */
public final class Settings extends AbstractSettings {

	
	private static final File THEME_DIR_FILE =
		new File(CommonUtils.getUserSettingsDir(), "themes");

	private static final String DEFAULT_THEME_NAME = 
		"default_theme.zip";

	private static final File DEFAULT_THEME_FILE = 
		new File(THEME_DIR_FILE, DEFAULT_THEME_NAME);

	/**
	 * The array of all theme files that should be copied by default
	 * from the themes jar file.
	 */
	private static final String[] THEMES = {
		DEFAULT_THEME_NAME,
		"black_theme.zip",
	}; 

	static {
		for(int i=0; i<THEMES.length; i++) { 
			CommonUtils.copyResourceFile(THEMES[i], 
										 new File(THEME_DIR_FILE, THEMES[i]));
		}
	}

	/**
	 * Setting for the default theme file to use for LimeWire display.
	 */
	public static final FileSetting THEME_DEFAULT = 
		FACTORY.createFileSetting("THEME_DEFAULT", DEFAULT_THEME_FILE); 

	/**
	 * Setting for the default theme directory to use in LimeWire display.
	 */
	public static final FileSetting THEME_DEFAULT_DIR = 
		FACTORY.createFileSetting("THEME_DEFAULT_DIR",  
								  new File(THEME_DIR_FILE, "default_theme"));

	/**
	 * Setting for the file name of the theme file.
	 */
	public static final FileSetting THEME_FILE =
		FACTORY.createFileSetting("THEME_FILE", THEME_DEFAULT.getValue());

	/**
	 * Setting for the file name of the theme directory.
	 */
	public static final FileSetting THEME_DIR =
		FACTORY.createFileSetting("THEME_DIR", THEME_DEFAULT_DIR.getValue());
}
