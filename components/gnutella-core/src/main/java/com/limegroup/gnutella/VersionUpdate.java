/**
 * file: VersionUpdate.java
 * auth: rsoule
 * desc: This class will establish an HTTP connection to the 
 *       limewire server, and look for a file called "Version".
 *       That file will contain the current version number, and
 *       if the user's version is older than the version in the 
 *       file, it will update (or ask the user if they want to
 *       update).
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.util.*;
import java.net.*;
import java.io.*;
import com.limegroup.gnutella.gui.*;
import javax.swing.*;

public class VersionUpdate implements Runnable
{

	private String VERSION_FILE = "version.txt";
	private static VersionUpdate _updater;
	
	private String _latest;


	// private constructor for singleton
	private VersionUpdate() 
	{
		_latest = SettingsManager.instance().getLastVersionChecked();
	}

	// static method for getting an instance of VersionUpdate
	public static VersionUpdate instance() 
	{
		if (_updater == null)
			_updater = new VersionUpdate();
		return _updater;
	}
	
	public void run() {
		this.check();
	}
	
	public void check() 
	{

		SettingsManager sm = SettingsManager.instance();
		boolean checkAgain;		
		checkAgain = sm.getCheckAgain();

		if (!checkAgain)
			return;

		ByteReader br;
		String latest = "";

		try {
			// open the http connection, and grab 
			// the file with the version number in it.
			URL url = new URL("http", "www.limewire.com", 
							  "/version.txt");
			URLConnection conn = url.openConnection();
			conn.connect();
            //The try-catch below works around JDK bug 4091706.
			InputStream input = conn.getInputStream();
			br = new ByteReader(input);
		} catch(Exception e) {
			return;
		}
		// read in the version number
		while (true) {

			String str = " ";

			try {
                str = br.readLine();
				// this should get us the version number
				if (str != null)
					latest = str;
            } catch (Exception e) {
				br.close();
                return;
            }
			
            //EOF?
            if (str==null || str.equals(""))
                break;

		}



		String current = sm.getCurrentVersion(); 
		int version = compare(current, latest);

		if (version == -1) {
			// the current version is not the newest

			String lastChecked;
			lastChecked = sm.getLastVersionChecked();

			checkAgain = sm.getCheckAgain();

			// if( (checkAgain == false) && 
			// (compare(lastChecked, latest) == 0)) {
			if( (compare(lastChecked, latest) == 0) ) {
				// dont ask 
			}

			else {
				// otherwise ask...
				askUpdate(current, latest);
			}

		}

		// sm.setCheckAgain(true);

		// sm.setLastVersionChecked(latest);  

		_latest = latest;

		br.close();

	}

	public void setLastVersionChecked() {
		SettingsManager sm = SettingsManager.instance();
		sm.setLastVersionChecked(_latest);
	}


	// This methos will compare two strings representing a 
	// version number.  if old is greater than, it returns
	// 1, if they are equal, it returns 0, and if old is 
	// less than it returns -1.  If there is a problem, it
	// will just return 0, altough it might be better to 
	// throw an exception.
	private int compare(String old_version, String new_version)
	{

		// first check to see if the values are null or blank
		// and return 0 if there is an error.
		if ( (old_version == null) || (old_version.equals("")) ||
			 (new_version == null) || (new_version.equals("")) ) {
			return 0;
		}	

		// the version number should be some combination
		// of period-delimeted numbers concatonated with 
		// a letter.  Ex. 1.2a
		// However, we don't really care about the letter
		// for determining whether or not to update.

		// the tokenizer, period-delimited
		StringTokenizer tokenizer_old = new StringTokenizer(old_version, ".");
		StringTokenizer tokenizer_new = new StringTokenizer(new_version, ".");

		// there are two numbers (with possibly more than one digit)
		// for each of the the version numbers

		String strOld;  // first number in string representation, old
		String strNew;  // first number in string representation, new

		int numOld;  // first number, old
		int numNew;  // first number, new
		
		// start with the left-most characters...
		strOld = tokenizer_old.nextToken();
		strNew = tokenizer_new.nextToken();
		
		// convert to int's...
		try {
			numOld = java.lang.Integer.parseInt(strOld);
			numNew = java.lang.Integer.parseInt(strNew);
		} catch (Exception e) {
			return 0;
		}
	   
		if  (numOld < numNew) {
			// old version is less than the new, so return -1
			return -1;
		}   
		else if  (numOld > numNew) {
			// old version is greater than the new, so return 1
			return 1;
		}

		// we really only need to continue if the values are equal



		// the next token should be a cobination of a number 
		// and a letter
		// start with the left-most characters...
		strOld = tokenizer_old.nextToken();
		strNew = tokenizer_new.nextToken();

		// i don't really like too do this, but i guess
		// the easiest thing to do is expect that there
		// will only be two charcters, that the first is
		// a number, and the second is a letter, and
		// that i can use the index to get them.
		
		String subNew; 
		String subOld; 
		
		char endOld = strOld.charAt(strOld.length()-1);
		char endNew = strNew.charAt(strNew.length()-1);

		if(!( (endOld >= '0') && (endOld <= '9') )) {
			// there is a letter at the end...
			// so i need to remove it to do the comparison
			subNew = strNew.substring(0,strNew.length() -1);
			subOld = strOld.substring(0,strOld.length() -1);
		} else {
			subOld = strOld;
			subNew = strNew;
		}
		
		// convert to int's...
		try {
			numNew = java.lang.Integer.parseInt(subNew);
			numOld = java.lang.Integer.parseInt(subOld);

		} catch (Exception e) {
			return 0;
		}

		if  (numOld < numNew) {
			// old version is less than the new, so return -1
			return -1;
		}
		else if  (numOld > numNew) {
			// old version is greater than the new, so return 1
			return 1;
		}

		// they are the same.. 
		return 0;  

	} 

	// send a message to the gui to ask the user if they
	// want to update to the latest version.  if they do, 
	// return true.
	public void askUpdate(String oldV, String newV) 
	{
		
		if (!(SettingsManager.instance().getCheckAgain()))
			return;
		
		String msg = "You are currently running version " +
		oldV + " of LimeWire.  Version " + newV + 
		" is now available for download at " + 
		"http://www.limewire.com/download/";
		
		Utilities.showVersionMessage(msg);
		// SettingsManager.instance().setCheckAgain(response);

	
		
	}
	

}
