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
public final class ThemeSettings extends AbstractSettings {

    static final File THEME_DIR_FILE =
		new File(CommonUtils.getUserSettingsDir(), "themes");

	/**
	 * The default name of the theme file for all operating systems other than
	 * OS X.
	 */
	private static final String DEFAULT_THEME_NAME = 
		"default_theme.zip";

	/**
	 * The default name of the theme file name for OS X.
	 */
	private static final String DEFAULT_OSX_THEME_NAME = 
		"default_osx_theme.zip";

	
	/**
	 * The full path to the default theme file.
	 */
	static final File DEFAULT_THEME_FILE = 
		new File(THEME_DIR_FILE, DEFAULT_THEME_NAME);

	/**
	 * The full path to the default theme file on OS X.
	 */
	static final File DEFAULT_OSX_THEME_FILE = 
		new File(THEME_DIR_FILE, DEFAULT_OSX_THEME_NAME);


	/**
	 * The array of all theme files that should be copied by default
	 * from the themes jar file.
	 */
	private static final String[] THEMES = {
		DEFAULT_THEME_NAME,
		DEFAULT_OSX_THEME_NAME,
		"black_theme.zip",
	}; 
	
	/**
	 * Statically expand any zip files in our jar if they're newer than the
	 * ones on disk.
	 */
	
	static {
		File themesJar = new File("themes.jar");
		if(!themesJar.isFile()) {
			themesJar = new File(new File("lib"), "themes.jar");
		}

        // workaround for when the jar file is only in your classpath --
        // this uses the default theme instead of the jar to check
        // the timestamp -- added by Jens-Uwe Mager
		if(!themesJar.isFile()) {
			String url = 
                ThemeSettings.class.getClassLoader().
                    getResource(DEFAULT_THEME_NAME).toString();
			if (url != null && url.startsWith("jar:file:")) {
				url = url.substring("jar:file:".length(), url.length());
				url = 
                    url.substring(0, url.length()-
                                  DEFAULT_THEME_NAME.length()-"!/".length());
				themesJar = new File(url);
			}
		}
		if(themesJar.isFile()) {
			long jarMod = themesJar.lastModified();
			for(int i=0; i<THEMES.length; i++) { 
				File zipFile = new File(THEME_DIR_FILE, THEMES[i]);
				if(zipFile.isFile()) {
					long zipMod = zipFile.lastModified();
					if(jarMod > zipMod) {
						CommonUtils.copyResourceFile(THEMES[i], zipFile, true);
					}
				} else if(!zipFile.exists()) {
					CommonUtils.copyResourceFile(THEMES[i], zipFile, true);
				}
			}
		}
	}
	
	/**
	 * Setting for the default theme file to use for LimeWire display.
	 */
	public static final FileSetting THEME_DEFAULT = 
		FACTORY.createFileSetting("THEME_DEFAULT", CommonUtils.isMacOSX() ? 
								  DEFAULT_OSX_THEME_FILE : 
								  DEFAULT_THEME_FILE); 

	/**
	 * Setting for the default theme directory to use in LimeWire display.
	 */
	public static final FileSetting THEME_DEFAULT_DIR = 
		FACTORY.createFileSetting("THEME_DEFAULT_DIR", CommonUtils.isMacOSX() ?
								  new File(THEME_DIR_FILE, 
										   "default_osx_theme") :
								  new File(THEME_DIR_FILE, 
										   "default_theme"));

	/**
	 * Setting for the file name of the theme file.
	 */
	public static final FileSetting THEME_FILE =
		FACTORY.createFileSetting("THEME_FILE", 
								  THEME_DEFAULT.getValue());

	/**
	 * Setting for the file name of the theme directory.
	 */
	public static final FileSetting THEME_DIR =
		FACTORY.createFileSetting("THEME_DIR", 
								  THEME_DEFAULT_DIR.getValue());
}
