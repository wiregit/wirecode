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
import javax.swing.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.downloader.*;

public class VersionUpdate
{

	private String VERSION_FILE = "version.txt";
	private static VersionUpdate _updater;
	
	private String _latest;
	private String _newVersion;
	private String _currentDirectory;
	private int    _amountRead;
	private int    _updateSize;
	private UpdateHandler _updateHandler;
	private SettingsManager _settings;

	// private constructor for singleton
	private VersionUpdate() 
	{
		_settings = SettingsManager.instance();
		_latest = _settings.getLastVersionChecked();
	}

	// static method for getting an instance of VersionUpdate
	public static VersionUpdate instance() 
	{
		if (_updater == null)
			_updater = new VersionUpdate();
		return _updater;
	}
		
	/** check for available updates and prompt the user
	 *  if we find an update available. */
	public void check() 
	{
		_currentDirectory = System.getProperty("user.dir");
		if(!_currentDirectory.endsWith(File.separator))
			_currentDirectory += File.separator;		
		if(_settings.getDeleteOldJAR()) {
			String oldJARName = _settings.getOldJARName();
			File oldCharFile = new File(_currentDirectory, oldJARName);
			if(oldCharFile.delete()) {
				_settings.setDeleteOldJAR(false);
				_settings.setOldJARName("");
			}
		}
		boolean checkAgain;		
		checkAgain = _settings.getCheckAgain();

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
				if (str != null) {
					latest = str;
				}
            } catch (Exception e) {
				br.close();
                return;
            }			
            //EOF?
            if (str==null || str.equals(""))
                break;
		}

		String current = _settings.getCurrentVersion(); 
		int version = compare(current, latest);

		if (version == -1) {
			// the current version is not the newest

			String lastChecked;
			lastChecked = _settings.getLastVersionChecked();
			checkAgain = _settings.getCheckAgain();

			// if( (checkAgain == false) && 
			// (compare(lastChecked, latest) == 0)) {
			if( (compare(lastChecked, latest) == 0) ) {
				// dont ask 
			}
			else {
				// otherwise ask...
				_newVersion = latest;
				askUpdate(current, latest);
			}
		}
		br.close();
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

	/** send a message to the gui to ask the user if they
	 *  want to update to the latest version.  if they do, 
	 *  return true. */
	public void askUpdate(String oldV, String newV) 
	{		
		if (!_settings.getCheckAgain())
			return;
		_updateHandler = new UpdateHandler();
		_updateHandler.showUpdatePrompt();
	}

	/** this method attempts to perform an update -- 
	 *  getting thew new jar file from the server,
	 *  replacing it, and making the call to change
	 *  the launch anywhere LAX file. */
	public void update() throws CantConnectException
	{	
		StringBuffer newFileBuf = new StringBuffer("LimeWire");
		StringTokenizer fileTok = new StringTokenizer(_newVersion,".");
		newFileBuf.append(fileTok.nextToken());
		newFileBuf.append(fileTok.nextToken());
		newFileBuf.append(".jar");	   
		String newFileName = newFileBuf.toString();
		String fullPath = _currentDirectory + newFileName;	
		try {
			// open the http connection, and grab 
			// the file with the version number in it.
			String pathName = "/"+newFileName;
			URL url = new URL("http", "www.limewire.com", 
							  pathName);
			URLConnection conn = url.openConnection();
			conn.connect();
			_updateSize = conn.getContentLength();
			_updateHandler.showProgressWindow(_updateSize);
			
			InputStream is = conn.getInputStream();
			ByteReader byteReader = new ByteReader(is);
			FileOutputStream fos = new FileOutputStream(fullPath, true);

			int percentRead = 0;
			_amountRead = 0;
			int newBytes = -1;
			byte[] buf = new byte[1024];
			while(true) {
				_updateHandler.update(_amountRead);
				if(_amountRead == _updateSize)
					break;
				if(_amountRead > _updateSize) {
					break;					
				}
				newBytes = byteReader.read(buf);
				if(newBytes == -1) 
					break;
				fos.write(buf, 0, newBytes);
				_amountRead += newBytes;
			}

			byteReader.close();
			fos.close();
			
			// the file transfer of the new jar has completed.
			// update the lax file and notify the user. 
			if(_amountRead == _updateSize) {
				_updateHandler.hideProgressWindow();
				updateLAXFile(newFileName);
				String message = "Your LimeWire update has successfully "+
				"completed.  Please restart LimeWire to use your new version.";
				Utilities.showMessage(message);
				_settings.setLastVersionChecked(_newVersion);
				System.exit(0);
			}
			
		} catch(MalformedURLException mue) {
		} catch(IOException ioe) {
		}
	}

	// updates the LimeWire.LAX file that is used by
	// InstallAnywhere to set the classpath
	private void updateLAXFile(final String newFileName) {
		File laxFile = new File(_currentDirectory, "LimeWire.lax");
		File tempLaxFile = new File(_currentDirectory, "LimeWireTemp.lax");
		String laxPath = "";
		try {
			laxPath = laxFile.getCanonicalPath();
		}
		catch(IOException ioe) {
			cancelUpdate("finding the full path name of your configuration file.");
			return;
		}
		if(laxFile.exists()) {
			String newClasspaths = "lax.class.path=";
			StringBuffer sb = new StringBuffer(newClasspaths);
			try {
				FileReader fr = new FileReader(laxFile);
				FileWriter fw = null;
				try {
					fw = new FileWriter(tempLaxFile);
				}
				catch(IOException ioe) {
					cancelUpdate("writing to a temporary file.");
				}
				BufferedReader br = new BufferedReader(fr);
				BufferedWriter bw = new BufferedWriter(fw);
				StringTokenizer st;
				String pathSeparator = System.getProperty("path.separator");
				String line = br.readLine();
				String curTok = "";
				while(line != null) {
					if(line.startsWith(newClasspaths)) {
						line = line.substring(15);
						st = new StringTokenizer(line, pathSeparator);
						curTok = st.nextToken()+pathSeparator;
						while(st.hasMoreTokens()) {							
							if(curTok.startsWith("LimeWire")) {
								if(!curTok.endsWith("update.jar"+pathSeparator)) {
									_settings.setOldJARName(curTok);
									curTok = newFileName+pathSeparator;
								}
							}
							sb.append(curTok);
							curTok = st.nextToken()+pathSeparator;
						}
						line = sb.toString();
					}
					bw.write(line, 0, line.length());						
					bw.newLine();
					line = br.readLine();
				}
				br.close();
				fr.close();
				fw.flush();
				bw.flush();
				fw.close();
				bw.close();
			} catch(java.io.FileNotFoundException fnfe) {
				cancelUpdate("finding your configuration file.");
			} catch(IOException ioe) {
				cancelUpdate("accessing your file system.");
			}
			String str = "";
			try {
				str = laxFile.getCanonicalPath();
			}
			catch(IOException ioe) {}
			laxFile.delete();
			if(!tempLaxFile.renameTo(laxFile)) {
				cancelUpdate("renaming your configuration file.");
			}
			else
				_settings.setDeleteOldJAR(true);
		}
		else {
			cancelUpdate("locating your configuration file.");
		}
	}

	/** cancels the udpate and prompts the user 
	 *  to send a message to limewire support.*/
	private void cancelUpdate(String error) {
		_settings.setDeleteOldJAR(false);
		_settings.setOldJARName("");
		String message = "Your LimeWire update has encountered an "+
		"internal error ";
		message += error;
		message += "Please e-mail LimeWire support at "+
		"\"support@limewire.com\" with this error.  You can download "+
		"the new version of LimeWire from www.limewire.com.";
		Utilities.showMessage(message);							  
	}
}
