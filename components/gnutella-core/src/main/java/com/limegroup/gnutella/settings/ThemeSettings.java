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
public final class ThemeSettings extends LimeProps {
    
    private ThemeSettings() {}
            
    /**
     * The extension for theme packs to allow people to search for them --
     * stands for "LimeWire Theme Pack".
     */
    public static final String EXTENSION = "lwtp";
    
    public static final File THEME_DIR_FILE =
		new File(CommonUtils.getUserSettingsDir(), "themes");
    
    /**
     * The default name of the theme file for all operating systems other than
     * OS X.
     */
    public static final String DEFAULT_THEME_NAME =
		"default_theme."+EXTENSION;
    
    /**
     * The default name of the theme file name for OS X.
     */
    public static final String DEFAULT_OSX_THEME_NAME =
		"default_osx_theme."+EXTENSION;
    
    /**
     * The default name of the windows laf theme file name.
     */
    public static final String WINDOWS_LAF_THEME_NAME =
        "windows_theme."+EXTENSION;
        
    /**
     * The default name of the theme file name for non-OS X pro users.
     */
    public static final String DEFAULT_PRO_THEME_NAME =
        "limewirePro_theme."+EXTENSION;
        
    /**
     * The default name of the theme file name for the new LimeWire theme.
     */
    public static final String LIMEWIRE_THEME_NAME =
        "limewire_theme."+EXTENSION;
    
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
     * The full path to the windows theme file for the windows LAF
     */
    static final File WINDOWS_LAF_THEME_FILE =
        new File(THEME_DIR_FILE, WINDOWS_LAF_THEME_NAME);
        
        
    /**
     * The full path to the default theme file on OS X.
     */
    static final File DEFAULT_PRO_THEME_FILE =
        new File(THEME_DIR_FILE, DEFAULT_PRO_THEME_NAME);
    
    /**
     * The array of all theme files that should be copied by default
     * from the themes jar file.
     */
    private static final String[] THEMES = {
        DEFAULT_THEME_NAME,
        DEFAULT_PRO_THEME_NAME,
        DEFAULT_OSX_THEME_NAME,
        WINDOWS_LAF_THEME_NAME,
        "black_theme."+EXTENSION,
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
                url = url.substring(0, url.length()-
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
                        copyZipWithNewTimestamp(THEMES[i], zipFile);
                    }
                } else if(!zipFile.exists()) {
                    copyZipWithNewTimestamp(THEMES[i], zipFile);
                }
            }
        }
    }

    /**
     * Utility method that copies and expends the specified theme 
     * file from the themes jar.
     *
     * @param name the name of the file in the jar
     * @param themeFile the location on disk to copy the zip to
     */
    private static void copyZipWithNewTimestamp(String name, File themeFile) {
        CommonUtils.copyResourceFile(name, themeFile, true);
        File themeDir = extractThemeDir(themeFile);
        expandTheme(themeFile, themeDir, true);
    }

    /**
     * Expands the specified theme zip file to the specified directory.
     *
     * @param themeFile the theme zip file to expand
     * @param themeDir the directory to expand to -- the themes directory
     *  plus the name of the theme
     * @param overwrite whether or not to force the overwriting of existing
     *  files in the destination folder when we expand the zip, regardless 
     *  of File/ZipEntry timestamp
     */
    static void expandTheme(File themeFile, File themeDir, 
                            boolean overwrite) {
        themeDir.mkdirs();
        try {
            FileUtils.setWriteable(themeDir);
            Expand.expandFile(themeFile, themeDir, overwrite);
        } catch(IOException e) {
            // this should never really happen, so report it
            ErrorService.error(e);						
        }        
    }

    /**
     * Convenience method for determing in the path of the themes directory
     * for a given theme file.  The directory is the path of the themes
     * directory plus the name of the theme.
     *
     * @param themeFile the <tt>File</tt> instance denoting the location 
     *  of the theme file on disk
     * @return a new <tt>File</tt> instance denoting the appropriate path
     *  for the directory for this specific theme
     */
    static File extractThemeDir(File themeFile) {
		String dirName = themeFile.getName();
		dirName = dirName.substring(0, dirName.length()-5);
		return new File(new File(CommonUtils.getUserSettingsDir(),"themes"), 
                        dirName);
        
    }
    
    /**
     * Determines whether or not the specified file is a theme file.
     */
    static boolean isThemeFile(File f) {
        return f.getName().toLowerCase().endsWith("." + EXTENSION);
    }
    
    /**
     * Determines whether or not the current theme file is the default theme
     * file.
     *
     * @return <tt>true</tt> if the current theme file is the default,
     *  otherwise <tt>false</tt>
     */
    public static boolean isDefaultTheme() {
        return THEME_FILE.getValue().equals(THEME_DEFAULT.getValue());
    }
    
    /** 
     * Determines whether or not the current theme is the windows theme,
     * designed to be used for the windows laf.
     * @return <tt>true</tt> if the current theme is the windows theme,
     *  otherwise <tt>false</tt>
     */
    public static boolean isWindowsTheme() {
        return THEME_FILE.getValue().equals(WINDOWS_LAF_THEME_FILE);
    }
    
    /**
     * Formats a theme name, removing the underscore characters,
     * capitalizing the first letter of each word, and removing
     * the 'lwtp'.
     */
    public static String formatName(String name) {
        // strip off the .lwtp
        name = name.substring(0, name.length()-5);
        StringBuffer formatted = new StringBuffer(name.length());
        StringTokenizer st = new StringTokenizer(name, "_");
        String next;
        for(; st.hasMoreTokens(); ) {
            next = st.nextToken();
            formatted.append(" " + next.substring(0,1).toUpperCase(Locale.US));
            if(next.length() > 1) formatted.append(next.substring(1));
        }
        return formatted.toString();
    }        
    
    /**
     * Setting for the default theme file to use for LimeWire display.
     */
    public static final FileSetting THEME_DEFAULT =
		FACTORY.createFileSetting("THEME_DEFAULT", CommonUtils.isMacOSX() ?
								  DEFAULT_OSX_THEME_FILE :
                                  CommonUtils.isPro() ?
                                  DEFAULT_PRO_THEME_FILE :
							      DEFAULT_THEME_FILE);
	
	/**
	 * Setting for the default theme directory to use in LimeWire display.
	 */
	public static final FileSetting THEME_DEFAULT_DIR =
        FACTORY.createFileSetting("THEME_DEFAULT_DIR",
								  CommonUtils.isMacOSX() ?
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
