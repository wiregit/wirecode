package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import java.io.*;
import java.util.Properties;
import java.util.Enumeration;
import java.util.zip.*;
import java.awt.*;

/**
 * This class contains key/value pairs for the current "theme."  The
 * theme defines values for the colors, fonts, etc, of the application.
 */
public final class ThemeFileHandler {

	private static final Properties DEFAULT_PROPS = new Properties();

	/**
	 * Handle to the <tt>SettingsFactory</tt> for theme settings.
	 */
	private static SettingsFactory FACTORY;	

	static {
		reload();
	}

	/**
	 * Reloads the file from disk to read values from.
	 */
	public static void reload() {
		File themeFile = ThemeSettings.THEME_FILE.getValue();
		String dirName = themeFile.getName();
		dirName = dirName.substring(0, dirName.length()-5);
		File themeDir = 
			new File(new File(CommonUtils.getUserSettingsDir(),"themes"), 
					 dirName);

		final File THEME_PROPS = new File(themeDir, "theme.txt");

		// unpack the zip if we haven't already
		if(!themeDir.isDirectory() ||
		   (themeDir.lastModified() < themeFile.lastModified()) ||
		   !THEME_PROPS.isFile()) {

			themeDir.mkdirs();
            try {
                Expand.expandFile(themeFile, themeDir);
            } catch(IOException e) {
				// this should never really happen, so report it
				RouterService.error(e);						
            }
		} 
		handleFactory(THEME_PROPS);		
		ThemeSettings.THEME_DIR.setValue(themeDir);
	}

	/**
	 * Either creates the factory or reloads it as needed.
	 */
	private static void handleFactory(File file) {
		if(FACTORY == null) {
			FACTORY = SettingsFactory.createFromFile(file, DEFAULT_PROPS);				
		} else {
			FACTORY.reload(file);
		}
	}

	/////////////////// FONTS //////////////////////
	/**
	 * Setting for the control text font name.
	 */
	public static final StringSetting CONTROL_TEXT_FONT_NAME =
		FACTORY.createStringSetting("CONTROL_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the control text font style.
	 */
	public static final IntSetting CONTROL_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("CONTROL_TEXT_FONT_STYLE", 1);

	/**
	 * Setting for the control text font size.
	 */
	public static final IntSetting CONTROL_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("CONTROL_TEXT_FONT_SIZE", 11);

	/**
	 * Setting for the system text font name.
	 */
	public static final StringSetting SYSTEM_TEXT_FONT_NAME =
		FACTORY.createStringSetting("SYSTEM_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the system text font style.
	 */
	public static final IntSetting SYSTEM_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("SYSTEM_TEXT_FONT_STYLE", 0);

	/**
	 * Setting for the system text font size.
	 */
	public static final IntSetting SYSTEM_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("SYSTEM_TEXT_FONT_SIZE", 11);	

	/**
	 * Setting for the user text font name.
	 */
	public static final StringSetting USER_TEXT_FONT_NAME =
		FACTORY.createStringSetting("USER_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the user text font style.
	 */
	public static final IntSetting USER_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("USER_TEXT_FONT_STYLE", 0);

	/**
	 * Setting for the user text font size.
	 */
	public static final IntSetting USER_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("USER_TEXT_FONT_SIZE", 11);	

	/**
	 * Setting for the menu text font name.
	 */
	public static final StringSetting MENU_TEXT_FONT_NAME =
		FACTORY.createStringSetting("MENU_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the menu text font style.
	 */
	public static final IntSetting MENU_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("MENU_TEXT_FONT_STYLE", 1);

	/**
	 * Setting for the menu text font size.
	 */
	public static final IntSetting MENU_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("MENU_TEXT_FONT_SIZE", 11);	

	/**
	 * Setting for the window title font name.
	 */
	public static final StringSetting WINDOW_TITLE_FONT_NAME =
		FACTORY.createStringSetting("WINDOW_TITLE_FONT_NAME", "dialog");

	/**
	 * Setting for the window title font style.
	 */
	public static final IntSetting WINDOW_TITLE_FONT_STYLE =
		FACTORY.createIntSetting("WINDOW_TITLE_FONT_STYLE", 1);

	/**
	 * Setting for the window title font size.
	 */
	public static final IntSetting WINDOW_TITLE_FONT_SIZE =
		FACTORY.createIntSetting("WINDOW_TITLE_FONT_SIZE", 11);	

	/**
	 * Setting for the sub text font name.
	 */
	public static final StringSetting SUB_TEXT_FONT_NAME =
		FACTORY.createStringSetting("SUB_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the sub text font style.
	 */
	public static final IntSetting SUB_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("SUB_TEXT_FONT_STYLE", 0);

	/**
	 * Setting for the sub text font size.
	 */
	public static final IntSetting SUB_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("SUB_TEXT_FONT_SIZE", 10);

	/////////////////// END FONTS //////////////////////

	/**
	 * Setting for the primary 1 color.
	 */
	public static final ColorSetting PRIMARY1_COLOR = 
		FACTORY.createColorSetting("PRIMARY1_COLOR", 
								   new Color(74,110,188));

	/**
	 * Setting for the primary 2 color.
	 */
	public static final ColorSetting PRIMARY2_COLOR = 
		FACTORY.createColorSetting("PRIMARY2_COLOR", 
								   new Color(135,145,170));

	/**
	 * Setting for the primary 3 color.
	 */
	public static final ColorSetting PRIMARY3_COLOR = 
		FACTORY.createColorSetting("PRIMARY3_COLOR", 
								   new Color(216,225,244));

	/**
	 * Setting for the secondary 1 color.
	 */
	public static final ColorSetting SECONDARY1_COLOR =
		FACTORY.createColorSetting("SECONDARY1_COLOR", 
								   new Color(50,68,107));

	/**
	 * Setting for the secondary 2 color.
	 */
	public static final ColorSetting SECONDARY2_COLOR =
		FACTORY.createColorSetting("SECONDARY2_COLOR", 
								   new Color(167,173,190));

	/**
	 * Setting for the secondary 3 color.
	 */
	public static final ColorSetting SECONDARY3_COLOR =
		FACTORY.createColorSetting("SECONDARY3_COLOR", 
								   new Color(199,201,209));

	/**
	 * Setting for the window 1 color.
	 */
	public static final ColorSetting WINDOW1_COLOR =
		FACTORY.createColorSetting("WINDOW1_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 2 color.
	 */
	public static final ColorSetting WINDOW2_COLOR =
		FACTORY.createColorSetting("WINDOW2_COLOR", 
								   new Color(199,201,209));

	/**
	 * Setting for the window 3 color.
	 */
	public static final ColorSetting WINDOW3_COLOR =
		FACTORY.createColorSetting("WINDOW3_COLOR", 
								   new Color(199,201,209));

	/**
	 * Setting for the window 4 color.
	 */
	public static final ColorSetting WINDOW4_COLOR =
		FACTORY.createColorSetting("WINDOW4_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 5 color.
	 */
	public static final ColorSetting WINDOW5_COLOR =
		FACTORY.createColorSetting("WINDOW5_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 6 color.
	 */
	public static final ColorSetting WINDOW6_COLOR =
		FACTORY.createColorSetting("WINDOW6_COLOR", 
								   new Color(255,255,255));

	/**
	 * Setting for the window 7 color.
	 */
	public static final ColorSetting WINDOW7_COLOR =
		FACTORY.createColorSetting("WINDOW7_COLOR", 
								   new Color(255,255,255));

	/**
	 * Setting for the window 8 color.
	 */
	public static final ColorSetting WINDOW8_COLOR =
		FACTORY.createColorSetting("WINDOW8_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 9 color.
	 */
	public static final ColorSetting WINDOW9_COLOR =
		FACTORY.createColorSetting("WINDOW9_COLOR", 
								   new Color(0,0,0));	

	/**
	 * Setting for the window 10 color.
	 */
	public static final ColorSetting WINDOW10_COLOR =
		FACTORY.createColorSetting("WINDOW10_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 11 color.
	 */
	public static final ColorSetting WINDOW11_COLOR =
		FACTORY.createColorSetting("WINDOW11_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 12 color.
	 */
	public static final ColorSetting WINDOW12_COLOR =
		FACTORY.createColorSetting("WINDOW12_COLOR", 
								   new Color(199,201,209));							

	/**
	 * Setting for the table header background color.
	 */
	public static final ColorSetting TABLE_HEADER_BACKGROUND_COLOR =
		FACTORY.createColorSetting("TABLE_HEADER_BACKGROUND_COLOR",
								   new Color(117, 142, 197));

	/**
	 * Setting for the table background color.
	 */
	public static final ColorSetting TABLE_BACKGROUND_COLOR =
		FACTORY.createColorSetting("TABLE_BACKGROUND_COLOR", 
								   new Color(255,255,255));

	/**
	 * Setting for the not sharing label color.
	 */
	public static final ColorSetting NOT_SHARING_LABEL_COLOR =
		FACTORY.createColorSetting("NOT_SHARING_LABEL_COLOR", 
								   new Color(208, 0, 5));
	
	/**
	 * Setting for the search fade color.
	 */
	public static final ColorSetting SEARCH_FADE_COLOR =
		FACTORY.createColorSetting("SEARCH_FADE_COLOR", 
								   new Color(135,146,185));

	/**
	 * Setting for the search button color.
	 */
	public static final ColorSetting SEARCH_BUTTON_COLOR =
		FACTORY.createColorSetting("SEARCH_BUTTON_COLOR", 
								   new Color(255,255,255));

	/**
	 * Setting for the search result speed color.
	 */
	public static final ColorSetting SEARCH_RESULT_SPEED_COLOR =
		FACTORY.createColorSetting("SEARCH_RESULT_SPEED_COLOR", 
								   new Color(7,170,0));

	/**
	 * Setting for the playlist "playing song" color.
	 */
	public static final ColorSetting PLAYING_SONG_COLOR =
		FACTORY.createColorSetting("PLAYING_SONG_COLOR", 
								   new Color(7,170,0));
	/**
	 * Setting for the search ip address color.
	 */
	public static final ColorSetting SEARCH_IP_COLOR =
		FACTORY.createColorSetting("SEARCH_IP_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the search ip private address color.
	 */
	public static final ColorSetting SEARCH_PRIVATE_IP_COLOR =
		FACTORY.createColorSetting("SEARCH_PRIVATE_IP_COLOR", 
								   new Color(255, 0, 0));

	/*
	public static void main(String[] args) {
		ThemeSettings.reload();
	}	
	*/
}
