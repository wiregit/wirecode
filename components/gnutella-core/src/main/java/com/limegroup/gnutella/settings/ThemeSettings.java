package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.*;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
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
     * The normal 'LimeWire' theme.
     */
    public static final String LIMEWIRE_THEME_NAME =
		"limewire_theme."+EXTENSION;
    
    /**
     * The default name of the theme file name for OS X.
     */
    public static final String PINSTRIPES_OSX_THEME_NAME =
		"pinstripes_theme_osx."+EXTENSION;
		
    /**
     * The metal theme name.
     */
    public static final String BRUSHED_METAL_OSX_THEME_NAME =
        "brushed_metal_theme_osx."+EXTENSION;
    
    /**
     * The default name of the windows laf theme file name.
     */
    public static final String WINDOWS_LAF_THEME_NAME =
        "windows_theme."+EXTENSION;
        
    /**
     * The default name of the theme file name for non-OS X pro users.
     */
    public static final String PRO_THEME_NAME =
        "limewirePro_theme."+EXTENSION;
    
    /**
     * The full path to the LimeWire theme file.
     */
    static final File LIMEWIRE_THEME_FILE =
		new File(THEME_DIR_FILE, LIMEWIRE_THEME_NAME);
    
    /**
     * The full path to the default theme file on OS X.
     */
    static final File PINSTRIPES_OSX_THEME_FILE =
		new File(THEME_DIR_FILE, PINSTRIPES_OSX_THEME_NAME);
		
    /**
     * The full path to the metal theme file on OS X.
     */
    static final File BRUSHED_METAL_OSX_THEME_FILE =
		new File(THEME_DIR_FILE, BRUSHED_METAL_OSX_THEME_NAME);		
		
    /** 
     * The full path to the windows theme file for the windows LAF
     */
    static final File WINDOWS_LAF_THEME_FILE =
        new File(THEME_DIR_FILE, WINDOWS_LAF_THEME_NAME);
        
        
    /**
     * The full path to the pro only theme.
     */
    static final File PRO_THEME_FILE =
        new File(THEME_DIR_FILE, PRO_THEME_NAME);
    
    /**
     * Statically expand any zip files in our jar if they're newer than the
     * ones on disk.
     */
    static {
        File themesJar = new File("themes.jar");
        
        // workaround for when the jar file is only in your classpath --
        // this uses the default theme instead of the jar to check
        // the timestamp -- added by Jens-Uwe Mager
        // this also is used for running LimeWire from CVS
        if(!themesJar.isFile()) {
            String url =
            ThemeSettings.class.getClassLoader().
                getResource(LIMEWIRE_THEME_NAME).toString();
            if (url != null && url.startsWith("jar:file:")) {
                url = url.substring("jar:file:".length(), url.length());
                url = url.substring(0, url.length()-
                    LIMEWIRE_THEME_NAME.length()-"!/".length());
                themesJar = new File(url);
            }
        }
        
        if(themesJar.isFile()) {
            ZipFile zf = null;
            try {            
                long jarMod = themesJar.lastModified();
                zf = new ZipFile(themesJar);
                Enumeration entries = zf.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry ze = (ZipEntry)entries.nextElement();
                    String name = ze.getName();
                    if(!name.endsWith(".lwtp"))
                        continue;
                    File existingFile = new File(THEME_DIR_FILE, name);
                    if(existingFile.isFile()) {
                        if(jarMod > existingFile.lastModified())
                            copyZipWithNewTimestamp(name, existingFile);
                    } else if(!existingFile.exists()) {
                        copyZipWithNewTimestamp(name, existingFile);
                    }
                }
            } catch(IOException ioe) {
                ErrorService.error(ioe);
            } finally {
                if(zf != null) {
                    try {
                        zf.close();
                    } catch(IOException ignored) {}
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
    static boolean expandTheme(File themeFile, File themeDir, 
                            boolean overwrite) {
        themeDir.mkdirs();
        try {
            FileUtils.setWriteable(themeDir);
            Expand.expandFile(themeFile, themeDir, overwrite);
        } catch(ZipException ze) {
            // invalid theme, tell the user.
            MessageService.showError("ERROR_APPLYING_INVALID_THEME_FILE");
            return false;
        } catch(IOException e) {
            // this should never really happen, so report it
            ErrorService.error(e);
            return false;
        }
        return true;
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
        return THEME_FILE.getValue().equals(THEME_DEFAULT);
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
     * Determines if the theme is the brushed metal theme.
     */
    public static boolean isBrushedMetalTheme() {
        return THEME_FILE.getValue().equals(BRUSHED_METAL_OSX_THEME_FILE);
    }
    
    /**
     * Determines if the theme is the pinstripes theme.
     */
    public static boolean isPinstripesTheme() {
        return THEME_FILE.getValue().equals(PINSTRIPES_OSX_THEME_FILE);
    }
    
    /**
     * Determines if the current theme is the native OSX theme.
     */
    public static boolean isNativeOSXTheme() {
        return CommonUtils.isMacOSX() &&
              (isPinstripesTheme() || isBrushedMetalTheme());
    }
    
    /**
     * Determines if the current theme is the native theme.
     */
    public static boolean isNativeTheme() {
        return isNativeOSXTheme() || isWindowsTheme();
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
            if(next.equals("osx"))
                next = "(OSX)";
            else if(next.equals("limewire"))
                next = "LimeWire";
            else if(next.equals("limeWirePro"))
                next = "LimeWire PRO";
            formatted.append(" " + next.substring(0,1).toUpperCase(Locale.US));
            if(next.length() > 1) formatted.append(next.substring(1));
            
        }
        return formatted.toString();
    }        
    
    /**
     * Setting for the default theme file to use for LimeWire display.
     */
    public static final File THEME_DEFAULT;
    public static final File THEME_DEFAULT_DIR;
    static {
        File theme, dir;
        if(CommonUtils.isMacOSX()) {
            theme = PINSTRIPES_OSX_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "pinstripes_theme_osx");
        } else if(CommonUtils.isPro()) {
            theme = PRO_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "limewirePro_theme");
        } else if(CommonUtils.isWindowsXP() && CommonUtils.isJava14OrLater()) {
            theme = WINDOWS_LAF_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "windows_theme");
        } else {
            theme = LIMEWIRE_THEME_FILE;
            dir = new File(THEME_DIR_FILE, "limewire_theme");
        }
        THEME_DEFAULT = theme;
        THEME_DEFAULT_DIR = dir;
    }
	
	/**
	 * Setting for the file name of the theme file.
	 */
	public static final FileSetting THEME_FILE =
		FACTORY.createFileSetting("THEME_FILE", THEME_DEFAULT);
	
	/**
	 * Setting for the file name of the theme directory.
	 */
	public static final FileSetting THEME_DIR =
		FACTORY.createFileSetting("THEME_DIR", THEME_DEFAULT_DIR);
}
