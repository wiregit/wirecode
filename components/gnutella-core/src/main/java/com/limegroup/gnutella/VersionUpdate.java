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

public class VersionUpdate implements Runnable
{

	private String VERSION_FILE = "version.txt";
	private static VersionUpdate _updater;
	
	private String _latest;
	private String _newVersion;
	private int _amountRead;
	private String _currentDirectory;
	private int _updateSize;

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
				_newVersion = latest;
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
		"http://www.limewire.com/download/. \n" +
		"If using Windows or Unix, you may also"+
		" run the auto-update feature from your LimeWire folder.";
		
		//Utilities.showVersionMessage(msg);
		Utilities.showUpdatePrompt();
		// SettingsManager.instance().setCheckAgain(response);			
	}

	public void update() throws CantConnectException
	{	
		_currentDirectory = System.getProperty("user.dir");
		if(!_currentDirectory.endsWith(File.separator))
			_currentDirectory += File.separator;		
		StringBuffer newFileBuf = new StringBuffer("LimeWire");
		StringTokenizer fileTok = new StringTokenizer(_newVersion,".");
		newFileBuf.append(fileTok.nextToken());
		newFileBuf.append(fileTok.nextToken());
		newFileBuf.append(".jar");	   
		String newFileName = newFileBuf.toString();
		String fullPath = _currentDirectory + newFileName;	
		System.out.println("file to download: "+newFileName);
		try {
			// open the http connection, and grab 
			// the file with the version number in it.
			String pathName = "/"+newFileName;
			URL url = new URL("http", "www.limewire.com", 
							  pathName);
			URLConnection conn = url.openConnection();
			conn.connect();
			_updateSize = conn.getContentLength();
			UpdateWindow window = new UpdateWindow(_updateSize);
			
			InputStream is = conn.getInputStream();
			ByteReader byteReader = new ByteReader(is);
			FileOutputStream fos = new FileOutputStream(fullPath, true);

			int percentRead = 0;
			_amountRead = 0;
			int newBytes = -1;
			byte[] buf = new byte[1024];
			while(true) {
				System.out.println("amountRead: "+_amountRead);
				window.setAmountRead(_amountRead);
				SwingUtilities.invokeLater(window);
				//window.updatePercentRead(_amountRead);
				if(_amountRead == _updateSize)
					break;
				if(_amountRead > _updateSize) {
					//handleError();
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
			
			if(_amountRead == _updateSize) {
				window.setComplete();
				updateLAXFile(newFileName);
			}
			
		} catch(MalformedURLException mue) {
		} catch(IOException ioe) {
		}
	}

	/** returns the amount of the update file that has 
	 *  been read.  */
	public int getAmountRead() {
		return _amountRead;
	}

	public int getUpdateSize() {
		return _updateSize;
	}

	// updates the LimeWire.LAX file that is used by
	// InstallAnywhere to set the classpath
	public void updateLAXFile(final String newFileName) {
		File laxFile = new File(_currentDirectory, "LimeWire.lax");
		File tempLaxFile = new File(_currentDirectory, "LimeWireTemp.lax");
		String laxPath = "";
		//final String newFileName = _newFileName;
		try {
			laxPath = laxFile.getCanonicalPath();
		}
		catch(IOException ioe) {
			showErrorMessage("lax file get canonical path error");
			return;
		}
		if(laxFile.exists()) {
			String newClasspaths = "lax.class.path=";;
			try {
				FileReader fr = new FileReader(laxFile);
				FileWriter fw = null;
				try {
					fw = new FileWriter(tempLaxFile);
				}
				catch(IOException ioe) {
					showErrorMessage("updateLAXFile::IOException from FileWriter");
					System.out.println("exception caught");
				}
				BufferedReader br = new BufferedReader(fr);
				BufferedWriter bw = new BufferedWriter(fw);
				String line = br.readLine();
				//char[] lineChars = new char[line.length()];
				while(line != null) {
					//line.getChars(0,line.length(),lineCh`ars,0);					
					if(line.startsWith(newClasspaths)) {
						System.out.println("lax line1: "+line);
						line = line.substring(15);
						System.out.println("lax line2: "+line);
						StringBuffer sb = new StringBuffer(newClasspaths);
						StringTokenizer st = new StringTokenizer(line, ";");
						String curTok = st.nextToken()+";";
						while(st.hasMoreTokens()) {							
							System.out.println("current token: "+curTok);
							if(curTok.startsWith("LimeWire")) {
								if(!curTok.endsWith("update.jar;")) {
									curTok = newFileName+";";
									System.out.println("found it");
								}
							}
							sb.append(curTok);
							curTok = st.nextToken()+";";
						}
						line = sb.toString();
						System.out.println("new classpath string: "+line);
						//bw.write(newClasspaths, 0, newClasspaths.length());
					}
					bw.write(line, 0, line.length());						
					bw.newLine();
					line = br.readLine();
				}
				System.out.println("got outside of the loop");
				br.close();
				System.out.println("closed br");
				fr.close();
				System.out.println("closed fr");

				fw.flush();
				System.out.println("flushed fw");

				bw.flush();
				System.out.println("flushed bw");

				fw.close();
				System.out.println("closed fw");
				bw.close();
				System.out.println("closed bw");
			} catch(java.io.FileNotFoundException fnfe) {
				showErrorMessage("updateLAXFile::FileNotFoundException");
			} catch(IOException ioe) {
				showErrorMessage("updateLAXFile::IOException");
			}
			String str = "";
			try {
				str = laxFile.getCanonicalPath();
			}
			catch(IOException ioe) {}
			laxFile.delete();
			if(!tempLaxFile.renameTo(laxFile)) {
				String message = "Your LimeWire update has encountered an "+
				"internal error. Please go to your LimeWire installation folder "+
				"and rename the file \"LimeWireTemp.lax\" to \"LimeWire.lax\" to "+
				"complete your update.";
				Utilities.showMessage(message);
			}
		}
		else {
			showErrorMessage("lax file does not exist");
		}
	}

	private static void showErrorMessage(String error) {
		String message = "LimeWire update failed. Please download "+
		"the new version of LimeWire at www.limewire.com.";
		Utilities.showMessage(error);
	}

	public static void main(String args[]) {
		VersionUpdate vu = VersionUpdate.instance();
		vu.updateLAXFile("LimeWire12.jar");
	}
}
