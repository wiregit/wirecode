package com.limegroup.gnutella.tests;

import java.util.*;
import java.io.*;

/**
 * This is a test for locale-specific properties files.  Running
 * this test will output a file called "LOCALE_[INPUT_LANGUAGE]_TEST.txt" 
 * to the working directory.  Usage instructions are available via the
 * usual command line options.
 */
public final class LocaleTester {

	private final String BUNDLE_NAME = "MessagesBundle";
	private final ResourceBundle STANDARD_BUNDLE =
		ResourceBundle.getBundle(BUNDLE_NAME);

	private final ResourceBundle TEST_BUNDLE;

	private static String _testLanguageCode;
	
	private Locale _locale;

	private final String[] NON_ERROR_KEYS = {
		"OPTIONS_ROOT_NODE",
		"MENU_FILE_COMMUNITY_CONNECT",
		"MENU_FILE_COMMUNITY_CONNECT_ACCESSIBLE",
		"MENU_FILE_COMMUNITY_LOGOUT",
		"MENU_FILE_COMMUNITY_LOGOUT_ACCESSIBLE",
		"MENU_NAV_COMM",
		"MENU_NAV_COMM_ACCESSIBLE",
		"COMMUNITY_LOGOUT_BUTTON_LABEL",
		"MENU_VIEW_MONITOR_MNEMONIC",
		"MENU_HELP_FAQ_MNEMONIC",
		"MENU_HELP_FORUM_MNEMONIC",
		"MENU_NAV_MONITOR_MNEMONIC",
		"MENU_HELP_TITLE_MNEMONIC",
		"MENU_FILE_LIBRARY_RENAME_FOLDER_MNEMONIC",
		"MENU_FILE_LIBRARY_LAUNCH_FILES_MNEMONIC",
		"MENU_FILE_LIBRARY_ADD_SHARED_FOLDER_MNEMONIC",
		"MENU_FILE_LIBRARY_NEW_FOLDER_MNEMONIC",
		"MENU_RESOURCES_LIMESHOP_MNEMONIC",
		"MENU_TOOLS_TITLE_MNEMONIC",
		"MENU_VIEW_MONITOR_MNEMONIC",
		"MENU_FILE_LIBRARY_REFRESH_MNEMONIC",
		"MENU_NAV_SEARCH_MNEMONIC",
		"MENU_RESOURCES_TITLE_MNEMONIC",
		"MENU_TOOLS_OPTIONS_MNEMONIC",
		"MENU_NAV_TITLE_MNEMONIC",
		"MENU_RESOURCES_PRO_MNEMONIC",
		"MENU_TOOLS_STATISTICS_MNEMONIC",
		"MENU_FILE_LIBRARY_DELETE_FILES_MNEMONIC",
		"MENU_FILE_LIBRARY_DELETE_FOLDER_MNEMONIC"
	};

	public LocaleTester() {		
		if(_testLanguageCode.equals("german")) {
			_locale = Locale.GERMAN;
		} else if(_testLanguageCode.equals("french")) {
			_locale = Locale.FRENCH;
		} else if(_testLanguageCode.equals("japanese")) {
			_locale = Locale.JAPANESE;
		} else if(_testLanguageCode.equals("chinese")) {
			_locale = Locale.CHINESE;
		} else if(_testLanguageCode.equals("korean")) {
			_locale = Locale.KOREAN;
		} else if(_testLanguageCode.equals("italian")) {
			_locale = Locale.ITALIAN;
		} else {
			System.out.println("Locale not supported in test.");
			System.exit(0);
		}
		TEST_BUNDLE = 
		    ResourceBundle.getBundle(BUNDLE_NAME, _locale);
		
		File outputFile = 
		    new File("LOCALE_"+
					 _locale.getDisplayLanguage().toUpperCase()+
					 "_TEST.txt");
		outputFile.delete();
		
		FileOutputStream fos = null;
		try {
		    fos = new FileOutputStream(outputFile);
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(0);
		}
		Enumeration keys = STANDARD_BUNDLE.getKeys();
		String curKey = "";
		String standardKey = "";
		String testKey = "";
		String standardValue = "";
		String testValue = "";
		boolean errorEncountered = false;
		try {
			while(keys.hasMoreElements()) {
				curKey = (String)keys.nextElement();
				try {
					testValue = TEST_BUNDLE.getString(curKey);
				} catch(MissingResourceException mre) {
					if(!shouldAddToErrors(curKey)) continue;
					errorEncountered = true;
					String str = "MISSING KEY: "+curKey+"\n";
					fos.write(str.getBytes());
				}
				try {
					standardValue = STANDARD_BUNDLE.getString(curKey);
					if(testValue.equals(standardValue)) {
						if(!shouldAddToErrors(curKey)) continue;
						errorEncountered = true;
						String str = "IDENTICAL VALUES IN ENGLISH "+
						"AND "+_locale.getDisplayLanguage().toUpperCase()+
						" VERSIONS -- "+
						"POSSIBLE ERROR FOR KEY: "+curKey+"\n";
						fos.write(str.getBytes());
					}
				} catch(MissingResourceException mre) {
					errorEncountered = true;
					System.out.println("MissingResourceException caught "+
									   "from standard bundle.  This should "+
									   "never happen.  Aborting.");
					
					outputFile.delete();
					System.exit(0);
				}
			}
			
			if(!errorEncountered) {
				fos.write("TEST SUCCESSFUL -- NO ERRORS IN FILE".getBytes());
			}
			fos.close();
			fos.flush();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(0);
		}			
	}

	private boolean shouldAddToErrors(String key) {
		for(int i=0; i<NON_ERROR_KEYS.length; i++) {
			if(key.equals(NON_ERROR_KEYS[i])) return false;
		}
		return true;
	}


	public static void main(String[] args) {
		String usageLine = "Usage: java -cp [path to locale property "+
		"files directory] com.limegroup.gnutell.gui."+
		"tests.LocaleTester [language]";
		if(args.length != 1) {
			System.out.println();
			System.out.println();
			System.out.println(usageLine);
			System.out.println();
			System.out.println();
			System.exit(0);
		}
		LocaleTester._testLanguageCode = args[0].toLowerCase();
		if(args[0].toLowerCase().endsWith("help")) {
			System.out.println(usageLine);
			System.out.println();
			System.out.println("The locale-specific properties file "+
							   "that you are testing must be in your "+
							   "classpath.");
			System.out.println();
			System.out.println();
		}
		LocaleTester tester = new LocaleTester();
	}
}

