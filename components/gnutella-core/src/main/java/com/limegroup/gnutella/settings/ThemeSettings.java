package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import java.io.*;
import java.util.Properties;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * This class contains key/value pairs for the current "theme."  The
 * theme defines values for the colors, fonts, etc, of the application.
 */
public final class ThemeSettings {

	private static final Properties DEFAULT_PROPS = new Properties();


	private static SettingsFactory FACTORY;

	static {
		reload();
	}

	/**
	 * Reloads the file from disk to read values from.
	 */
	public static void reload() {
		File themeFile = Settings.THEME_FILE.getValue();
		String dirName = themeFile.getName();
		dirName = dirName.substring(0, dirName.length()-4);
		File themeDir = 
			new File(new File(CommonUtils.getUserSettingsDir(),"themes"), 
					 dirName);

		final File THEME_PROPS = new File(themeDir, "theme.txt");

		// unpack the zip if we haven't already
		if(!themeDir.isDirectory() ||
		   (themeDir.lastModified() < themeFile.lastModified())) {

			themeDir.mkdirs();
			
			try {
				ZipFile zf = 
					new ZipFile(themeFile, ZipFile.OPEN_READ);		

				
				Enumeration list = zf.entries();
				while (list.hasMoreElements()) {
					ZipEntry ze = (ZipEntry)list.nextElement();
					BufferedInputStream bis =
						new BufferedInputStream(zf.getInputStream(ze));
					FileOutputStream fos = 
						new FileOutputStream(new File(themeDir, ze.getName()));
					int sz = (int)ze.getSize();
					final int N = 1024;
					byte buf[] = new byte[N];
					int ln = 0;
					while (sz > 0 &&  // workaround for bug
						   (ln = bis.read(buf, 0, Math.min(N, sz))) != -1) {
						fos.write(buf, 0, ln);
						sz -= ln;
					}
					bis.close();
					fos.flush();				
				}
				
				handleFactory(THEME_PROPS);
			} catch(IOException e) {
				// this should never really happen, so report it
				RouterService.error(e);						
			}
		} 
		handleFactory(THEME_PROPS);		
		Settings.THEME_DIR.setValue(themeDir);
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

	/**
	 * Setting for the primary 1 color red value.
	 */
	public static final IntSetting PRIMARY1_COLOR_R = 
		FACTORY.createIntSetting("PRIMARY1_COLOR_R", 74);

	/**
	 * Setting for the primary 1 color green value.
	 */
	public static final IntSetting PRIMARY1_COLOR_G = 
		FACTORY.createIntSetting("PRIMARY1_COLOR_G", 110);

	/**
	 * Setting for the primary 1 color blue value.
	 */
	public static final IntSetting PRIMARY1_COLOR_B = 
		FACTORY.createIntSetting("PRIMARY1_COLOR_B", 188);

	/**
	 * Setting for the primary 2 color red value.
	 */
	public static final IntSetting PRIMARY2_COLOR_R = 
		FACTORY.createIntSetting("PRIMARY2_COLOR_R", 135);

	/**
	 * Setting for the primary 2 color green value.
	 */
	public static final IntSetting PRIMARY2_COLOR_G = 
		FACTORY.createIntSetting("PRIMARY2_COLOR_G", 145);

	/**
	 * Setting for the primary 2 color blue value.
	 */
	public static final IntSetting PRIMARY2_COLOR_B = 
		FACTORY.createIntSetting("PRIMARY2_COLOR_B", 170);


	/**
	 * Setting for the primary 3 color red value.
	 */
	public static final IntSetting PRIMARY3_COLOR_R = 
		FACTORY.createIntSetting("PRIMARY3_COLOR_R", 216);

	/**
	 * Setting for the primary 3 color green value.
	 */
	public static final IntSetting PRIMARY3_COLOR_G = 
		FACTORY.createIntSetting("PRIMARY3_COLOR_G", 225);


	/**
	 * Setting for the primary 3 color blue value.
	 */
	public static final IntSetting PRIMARY3_COLOR_B = 
		FACTORY.createIntSetting("PRIMARY3_COLOR_B", 244);


	/**
	 * Setting for the secondary 1 color red value.
	 */
	public static final IntSetting SECONDARY1_COLOR_R = 
		FACTORY.createIntSetting("SECONDARY1_COLOR_R", 50);

	/**
	 * Setting for the secondary 1 color green value.
	 */
	public static final IntSetting SECONDARY1_COLOR_G = 
		FACTORY.createIntSetting("SECONDARY1_COLOR_G", 68);


	/**
	 * Setting for the secondary 1 color blue value.
	 */
	public static final IntSetting SECONDARY1_COLOR_B = 
		FACTORY.createIntSetting("SECONDARY1_COLOR_B", 107);

	/**
	 * Setting for the secondary 2 color red value.
	 */
	public static final IntSetting SECONDARY2_COLOR_R = 
		FACTORY.createIntSetting("SECONDARY2_COLOR_R", 167);

	/**
	 * Setting for the secondary 2 color green value.
	 */
	public static final IntSetting SECONDARY2_COLOR_G = 
		FACTORY.createIntSetting("SECONDARY2_COLOR_G", 173);


	/**
	 * Setting for the secondary 2 color blue value.
	 */
	public static final IntSetting SECONDARY2_COLOR_B = 
		FACTORY.createIntSetting("SECONDARY2_COLOR_B", 190);


	/**
	 * Setting for the secondary 3 color red value.
	 */
	public static final IntSetting SECONDARY3_COLOR_R = 
		FACTORY.createIntSetting("SECONDARY3_COLOR_R", 199);

	/**
	 * Setting for the secondary 3 color green value.
	 */
	public static final IntSetting SECONDARY3_COLOR_G = 
		FACTORY.createIntSetting("SECONDARY3_COLOR_G", 201);


	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting SECONDARY3_COLOR_B = 
		FACTORY.createIntSetting("SECONDARY3_COLOR_B", 209);
		
	
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW1_COLOR_R = 
		FACTORY.createIntSetting("WINDOW1_COLOR_R", 209);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW1_COLOR_G = 
		FACTORY.createIntSetting("WINDOW1_COLOR_G", 209);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW1_COLOR_B = 
		FACTORY.createIntSetting("WINDOW1_COLOR_B", 209);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW2_COLOR_R = 
		FACTORY.createIntSetting("WINDOW2_COLOR_R", 209);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW2_COLOR_G = 
		FACTORY.createIntSetting("WINDOW2_COLOR_G", 209);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW2_COLOR_B = 
		FACTORY.createIntSetting("WINDOW2_COLOR_B", 209);			
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW3_COLOR_R = 
		FACTORY.createIntSetting("WINDOW3_COLOR_R", 209);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW3_COLOR_G = 
		FACTORY.createIntSetting("WINDOW3_COLOR_G", 209);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW3_COLOR_B = 
		FACTORY.createIntSetting("WINDOW3_COLOR_B", 209);			
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW4_COLOR_R = 
		FACTORY.createIntSetting("WINDOW4_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW4_COLOR_G = 
		FACTORY.createIntSetting("WINDOW4_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW4_COLOR_B = 
		FACTORY.createIntSetting("WINDOW4_COLOR_B", 0);			
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW5_COLOR_R = 
		FACTORY.createIntSetting("WINDOW5_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW5_COLOR_G = 
		FACTORY.createIntSetting("WINDOW5_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW5_COLOR_B = 
		FACTORY.createIntSetting("WINDOW5_COLOR_B", 0);			

		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW6_COLOR_R = 
		FACTORY.createIntSetting("WINDOW6_COLOR_R", 255);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW6_COLOR_G = 
		FACTORY.createIntSetting("WINDOW6_COLOR_G", 255);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW6_COLOR_B = 
		FACTORY.createIntSetting("WINDOW6_COLOR_B", 255);			
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW7_COLOR_R = 
		FACTORY.createIntSetting("WINDOW7_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW7_COLOR_G = 
		FACTORY.createIntSetting("WINDOW7_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW7_COLOR_B = 
		FACTORY.createIntSetting("WINDOW7_COLOR_B", 0);		
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW8_COLOR_R = 
		FACTORY.createIntSetting("WINDOW8_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW8_COLOR_G = 
		FACTORY.createIntSetting("WINDOW8_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW8_COLOR_B = 
		FACTORY.createIntSetting("WINDOW8_COLOR_B", 0);	
		
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW9_COLOR_R = 
		FACTORY.createIntSetting("WINDOW9_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW9_COLOR_G = 
		FACTORY.createIntSetting("WINDOW9_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW9_COLOR_B = 
		FACTORY.createIntSetting("WINDOW9_COLOR_B", 0);			
		
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW10_COLOR_R = 
		FACTORY.createIntSetting("WINDOW10_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW10_COLOR_G = 
		FACTORY.createIntSetting("WINDOW10_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW10_COLOR_B = 
		FACTORY.createIntSetting("WINDOW10_COLOR_B", 0);			
		
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW11_COLOR_R = 
		FACTORY.createIntSetting("WINDOW11_COLOR_R", 0);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW11_COLOR_G = 
		FACTORY.createIntSetting("WINDOW11_COLOR_G", 0);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW11_COLOR_B = 
		FACTORY.createIntSetting("WINDOW11_COLOR_B", 0);			
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW12_COLOR_R = 
		FACTORY.createIntSetting("WINDOW12_COLOR_R", 199);	
		

	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW12_COLOR_G = 
		FACTORY.createIntSetting("WINDOW12_COLOR_G", 201);
		
	/**
	 * Setting for the secondary 3 color blue value.
	 */
	public static final IntSetting WINDOW12_COLOR_B = 
		FACTORY.createIntSetting("WINDOW12_COLOR_B", 209);								

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
	 * Setting for the table header background color red value.
	 */
	public static final IntSetting TABLE_HEADER_BACKGROUND_COLOR_R = 
		FACTORY.createIntSetting("TABLE_HEADER_BACKGROUND_COLOR_R", 117);

	/**
	 * Setting for the table header background color green value.
	 */
	public static final IntSetting TABLE_HEADER_BACKGROUND_COLOR_G = 
		FACTORY.createIntSetting("TABLE_HEADER_BACKGROUND_COLOR_G", 142);

	/**
	 * Setting for the table header background color blue value.
	 */
	public static final IntSetting TABLE_HEADER_BACKGROUND_COLOR_B = 
		FACTORY.createIntSetting("TABLE_HEADER_BACKGROUND_COLOR_B", 197);


	/**
	 * Setting for the table background color red value.
	 */
	public static final IntSetting TABLE_BACKGROUND_COLOR_R = 
		FACTORY.createIntSetting("TABLE_BACKGROUND_COLOR_R", 255);

	/**
	 * Setting for the table background color green value.
	 */
	public static final IntSetting TABLE_BACKGROUND_COLOR_G = 
		FACTORY.createIntSetting("TABLE_BACKGROUND_COLOR_G", 255);

	/**
	 * Setting for the table background color blue value.
	 */
	public static final IntSetting TABLE_BACKGROUND_COLOR_B = 
		FACTORY.createIntSetting("TABLE_BACKGROUND_COLOR_B", 255);


	/**
	 * Setting for the not sharing label color red value.
	 */
	public static final IntSetting NOT_SHARING_LABEL_COLOR_R = 
		FACTORY.createIntSetting("NOT_SHARING_LABEL_COLOR_R", 208);

	/**
	 * Setting for the not sharing label color green value.
	 */
	public static final IntSetting NOT_SHARING_LABEL_COLOR_G = 
		FACTORY.createIntSetting("NOT_SHARING_LABEL_COLOR_G", 0);

	/**
	 * Setting for the not sharing label color blue value.
	 */
	public static final IntSetting NOT_SHARING_LABEL_COLOR_B = 
		FACTORY.createIntSetting("NOT_SHARING_LABEL_COLOR_B", 5);

	
	/**
	 * Setting for the search fade color red value.
	 */
	public static final IntSetting SEARCH_FADE_COLOR_R = 
		FACTORY.createIntSetting("SEARCH_FADE_COLOR_R", 135);

	/**
	 * Setting for the search fade color green value.
	 */
	public static final IntSetting SEARCH_FADE_COLOR_G = 
		FACTORY.createIntSetting("SEARCH_FADE_COLOR_G", 146);

	/**
	 * Setting for the search fade color blue value.
	 */
	public static final IntSetting SEARCH_FADE_COLOR_B = 
		FACTORY.createIntSetting("SEARCH_FADE_COLOR_B", 185);


	/**
	 * Setting for the search button color red value.
	 */
	public static final IntSetting SEARCH_BUTTON_COLOR_R = 
		FACTORY.createIntSetting("SEARCH_BUTTON_COLOR_R", 255);

	/**
	 * Setting for the search button color green value.
	 */
	public static final IntSetting SEARCH_BUTTON_COLOR_G = 
		FACTORY.createIntSetting("SEARCH_BUTTON_COLOR_G", 255);

	/**
	 * Setting for the search button color blue value.
	 */
	public static final IntSetting SEARCH_BUTTON_COLOR_B = 
		FACTORY.createIntSetting("SEARCH_BUTTON_COLOR_B", 255);

	/**
	 * Setting for the search result speed color red value.
	 */
	public static final IntSetting SEARCH_RESULT_SPEED_COLOR_R = 
		FACTORY.createIntSetting("SEARCH_RESULT_SPEED_COLOR_R", 7);

	/**
	 * Setting for the search result speed color green value.
	 */
	public static final IntSetting SEARCH_RESULT_SPEED_COLOR_G = 
		FACTORY.createIntSetting("SEARCH_RESULT_SPEED_COLOR_G", 170);

	/**
	 * Setting for the search result speed color blue value.
	 */
	public static final IntSetting SEARCH_RESULT_SPEED_COLOR_B = 
		FACTORY.createIntSetting("SEARCH_RESULT_SPEED_COLOR_B", 0);


	/**
	 * Setting for the playlist "playing song" color red value.
	 */
	public static final IntSetting PLAYING_SONG_COLOR_R = 
		FACTORY.createIntSetting("PLAYING_SONG_COLOR_R", 7);

	/**
	 * Setting for the playlist "playing song" color green value.
	 */
	public static final IntSetting PLAYING_SONG_COLOR_G = 
		FACTORY.createIntSetting("PLAYING_SONG_COLOR_G", 170);

	/**
	 * Setting for the playlist "playing song" color blue value.
	 */
	public static final IntSetting PLAYING_SONG_COLOR_B = 
		FACTORY.createIntSetting("PLAYING_SONG_COLOR_B", 0);

	public static void main(String[] args) {
		ThemeSettings.reload();
	}	
}
