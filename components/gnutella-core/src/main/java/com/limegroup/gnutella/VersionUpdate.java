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
import java.awt.event.*;
import com.limegroup.gnutella.gui.Main;
import com.limegroup.gnutella.gui.Utilities;
import com.limegroup.gnutella.gui.UpdateHandler;
import com.limegroup.gnutella.gui.UpdateTimedOutException;
import com.limegroup.gnutella.downloader.*;

public class VersionUpdate
{
	private static final VersionUpdate _updater = new VersionUpdate();	
	private String _latest;
	private String _newVersion;
	private String _currentDirectory;
	private int    _amountRead;
	private int    _updateSize;
	private UpdateHandler _updateHandler;
	private SettingsManager _settings;
	private Timer _updateTimer;
	private boolean _timedOut;

	// private constructor for singleton
	private VersionUpdate() 
	{
		_settings = SettingsManager.instance();
		_latest = _settings.getLastVersionChecked();
		_timedOut = false;
	}

	// static method for getting an instance of VersionUpdate
	public static VersionUpdate instance() 
	{
		return _updater;
	}
		
	/** check for available updates and prompt the user
	 *  if we find an update available. */
	public void check() throws UpdateTimedOutException 
	{
		_updateTimer = new Timer(1500, new UpdateTimerListener());		
		_updateTimer.setRepeats(false);
		_updateTimer.start();
		_currentDirectory = System.getProperty("user.dir");
		if(!_currentDirectory.endsWith(File.separator))
			_currentDirectory += File.separator;	
		boolean delete = _settings.getDeleteOldJAR();
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
		} catch(MalformedURLException mue) {
			return;
		} catch(IOException ioe) {
			return;
		}
		_updateTimer.stop();
		if(_timedOut) {
			throw new UpdateTimedOutException();
		}
		else {
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
				
				if( (compare(lastChecked, latest) == 0) ) {
					// dont ask 
				}
				else {
					// otherwise ask...
					_newVersion = latest;
					askUpdate(current, latest);
				}
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
	 *  getting the new jar file from the server,
	 *  replacing it, and making the call to change
	 *  the launch anywhere "LAX" file. */
	public void update() throws CantConnectException
	{			
		StringBuffer newFileBuf = new StringBuffer("LimeWire");
		StringTokenizer fileTok = new StringTokenizer(_newVersion,".");
		newFileBuf.append(fileTok.nextToken());
		newFileBuf.append(fileTok.nextToken());
		newFileBuf.append(".jar");	   
		String newFileName = newFileBuf.toString();
		String fullPath = _currentDirectory + newFileName;	
		File jarFile = new File(fullPath);
		// delete the file with the same name as our
		// new jar if it exists.  this should never
		// happen, but would cause a serious problem
		// if it did, so we delete it.
		if(jarFile.exists()) {
			jarFile.delete();
		}
		try {
			// open the http connection, and grab 
			// the file with the version number in it.
			String pathName = "/"+newFileName;
			URL url = new URL("http", "www.limewire.com", 
							  pathName);
			URLConnection conn = url.openConnection();
			conn.connect();
			_updateSize = conn.getContentLength();
			if(_updateSize == -1) {
				cancelUpdate("finding the new file on the server.");
			}
			else {
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
					if(updateLAXFile(newFileName)) {
						String message = "Your LimeWire update has successfully "+
						"completed.  Please restart LimeWire to use your new version.";
						Utilities.showMessage(message);						
						_settings.setLastVersionChecked(_newVersion);
					}
					_settings.writeProperties();
					System.exit(0);
				}
			}
			
		} catch(MalformedURLException mue) {
		} catch(IOException ioe) {
		}
	}

	// updates the LimeWire.LAX file that is used by
	// InstallAnywhere to set the classpath
	private boolean updateLAXFile(final String newFileName) {
		File laxFile = new File(_currentDirectory, "LimeWire.lax");
		File tempLaxFile = new File(_currentDirectory, "LimeWireTemp.lax");
		String laxPath = "";
		try {
			laxPath = laxFile.getCanonicalPath();
		}
		catch(IOException ioe) {
			cancelUpdate("finding the full path name of your configuration file.");
			return false;
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
					return false;
				}
				BufferedReader br = new BufferedReader(fr);
				BufferedWriter bw = new BufferedWriter(fw);
				StringTokenizer st;
				String pathSeparator = System.getProperty("path.separator");
				String line = br.readLine();
				String curTok = "";
				String endFlag = "update.jar";
				while(line != null) {
					if(line.startsWith(newClasspaths)) {
						line = line.substring(15);
						st = new StringTokenizer(line, pathSeparator);
						while(st.hasMoreTokens()) {
							curTok = st.nextToken();							
							if(curTok.startsWith("LimeWire")) {
								if(!curTok.endsWith(endFlag)) {
									_settings.setOldJARName(curTok);
									curTok = newFileName;
								}
							}
							sb.append(curTok + pathSeparator);
						}
						line = sb.toString();
					}
					bw.write(line, 0, line.length());						
					bw.newLine();
					line = br.readLine();
				}
				// close the buffered reader and the file reader
				// the readers, of course, do not flush
				br.close();
				fr.close();

				// flush and close the writers
				fw.flush();
				bw.flush();
				fw.close();
				bw.close();
			} catch(java.io.FileNotFoundException fnfe) {
				cancelUpdate("finding your configuration file.");
				return false;
			} catch(IOException ioe) {
				cancelUpdate("accessing your file system.");
				return false;
			}
			String str = "";
			try {
				str = laxFile.getCanonicalPath();
			}
			catch(IOException ioe) {}
			laxFile.delete();
			if(!tempLaxFile.renameTo(laxFile)) {
				cancelUpdate("renaming your configuration file.");
				return false;
			}
			_settings.setDeleteOldJAR(true);
		}
		else {
			cancelUpdate("locating your configuration file.");
			return false;
		}
		return true;
	}

	/** cancels the udpate and prompts the user 
	 *  to send a message to limewire support.*/
	private void cancelUpdate(String error) {
		resetSettings();
		String message = "Your LimeWire update has encountered an "+
		"internal error ";
		message += error;
		message += "  Please e-mail LimeWire support at "+
		"\"support@limewire.com\" with this error.  You can download "+
		"the new version of LimeWire from www.limewire.com.";
		Utilities.showMessage(message);							  
	}

	/** private helper method that is called when the update
	 *  has failed for some reason.  this sets the properties
	 *  in the SettingsManager that signal not to delete the
	 *  old jar file on the next LimeWire run because the
	 *  update failed in this case. */
	private void resetSettings() {
		_settings.setDeleteOldJAR(false);
		_settings.setOldJARName("");
	}

	/** private timer class that tells the program to continue
	 *  loading if the update has timed out. */
	private class UpdateTimerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_timedOut = true;
			Main.initialize();
			resetSettings();
		}	  	  
	}
}
