package com.limegroup.gnutella;

import java.io.*;
import java.util.*;
//import java.io.IOException;
import java.lang.IllegalArgumentException;
import com.limegroup.gnutella.gui.Backend;

/** 
 *  This class manages the property settings.  It maintains
 *  default settings for values not set in the saved  
 *  settings files and updates those settings based on user 
 *  input, checking for errors where appropriate.  It also 
 *  saves the settings file to disk when the session 
 *  terminates.
 *
 * @author Adam Fisk
 */

public class SettingsManager implements SettingsInterface
{
    /** Variables for the various settings */
    private static byte     ttl_;
    private static byte     softmaxttl_;
    private static byte     maxttl_;
    private static int      maxLength_;
    private static int      timeout_;
    private static String   hostList_;
    private static int      keepAlive_;
    private static int      port_;
    private static int      connectionSpeed_;
    private static byte     searchLimit_;
    private static boolean  stats_;
    private static String   clientID_;
    private static int      maxConn_;
    private static int      localIP_;       // not yet implemented
    private static String   saveDirectory_;
    private static String   directories_;
    private static String   extensions_;
    private static String[] bannedIps_;
    private static String[] bannedWords_;
    private static boolean  filterDuplicates_;
    private static boolean  filterAdult_;

    /** Set up a local variable for the properties */
    private static Properties props_;

    /** Specialized properties file for the
     *  network discoverer
     */
    private static Properties ndProps_;

    /**
     *  Set up the manager instance to follow the
     *  singleton pattern.
     */
    private static SettingsManager instance_;

    private String fileName_;
    private String ndFileName_;

    /**
     * This method provides the only access
     * to an instance of this class in 
     * accordance with the singleton pattern
     */
    public static SettingsManager instance()
    {
	if(instance_ == null)
	    instance_ = new SettingsManager();
	return instance_;
    }

    /** The constructor is private to ensure
     *  that only one copy will be created
     */
    private SettingsManager()
    {
	props_      = new Properties();
	ndProps_    = new Properties();
	fileName_   = System.getProperty("user.home");
	fileName_   = fileName_ + System.getProperty("file.separator");
	ndFileName_ = fileName_;
	fileName_   = fileName_ + SettingsInterface.DEFAULT_FILE_NAME;
	ndFileName_ = ndFileName_ + SettingsInterface.DEFAULT_ND_PROPS_NAME;
	try {
	    FileInputStream fis = new FileInputStream(ndFileName_);
	    try {
		ndProps_.load(fis);
	    }
	    catch(IOException ioe) {}
	}
	catch(FileNotFoundException fne){}	
       	initSettings();
    }

    /** Check the properties file and set the props */
    private void initSettings()
    {
	Properties tempProps = new Properties();
        try {
	    FileInputStream fis = new FileInputStream(fileName_);
	    try {
		tempProps.load(fis);
		loadDefaults();
		try {
		    fis.close();
		    validateFile(tempProps);
		}
		catch (IOException e){loadDefaults();}
	    }
	    catch (IOException e){loadDefaults();}
	}
	catch(FileNotFoundException e){loadDefaults();}
	catch(SecurityException se){loadDefaults();}
    }

    /** Makes sure that each property in the file 
     *  is valid.  If not, it sets that property
     *  to the default value.
     */
    private void validateFile(Properties tempProps) throws IOException
    {
	String p;
	byte b;
	int i;
	Enumeration enum = tempProps.propertyNames();
	while(enum.hasMoreElements()){
	    String key;
	    try {
		key = (String)enum.nextElement();
		p = tempProps.getProperty(key);
		if(key.equals(SettingsInterface.TTL))
		    {
			try {
			    b = Byte.parseByte(p);
			    try {setTTL(b);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }
		else if(key.equals(SettingsInterface.SOFT_MAX_TTL))
		    {
			try {
			    b = Byte.parseByte(p);
			    try {setSoftMaxTTL(b);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}  				
		    }
		else if(key.equals(SettingsInterface.MAX_TTL))
		    {
			try {
			    b = Byte.parseByte(p);
			    try {setMaxTTL(b);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}  				
		    }
		else if(key.equals(SettingsInterface.MAX_LENGTH))
		    {			
			try {
			    i = Integer.parseInt(p);
			    try {setMaxLength(i);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }
		else if(key.equals(SettingsInterface.TIMEOUT))
		    {
			try {
			    i = Integer.parseInt(p);
			    try {setTimeout(i);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }
		else if(key.equals(SettingsInterface.KEEP_ALIVE))
		    {	
			try {
			    i = Integer.parseInt(p);
			    try {setKeepAlive(i);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }
		else if(key.equals(SettingsInterface.PORT))
		    {
			try {
			    i = Integer.parseInt(p);
			    try {setPort(i);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }
		else if(key.equals(SettingsInterface.SPEED))
		    {
			try {
			    i = Integer.parseInt(p);
			    try {setConnectionSpeed(i);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }
		else if(key.equals(SettingsInterface.SEARCH_LIMIT))
		    {
			try {
			    byte s = Byte.parseByte(p);
			    try {setSearchLimit(s);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}			
		    }

		else if(key.equals(SettingsInterface.CLIENT_ID))
		    {
			try {setClientID(p);}
			catch (IllegalArgumentException ie){}
		    }

		else if(key.equals(SettingsInterface.STATS))
		    {
			boolean bs = p.equals("true");
			try {setStats(bs);}
			catch (IllegalArgumentException ie){}
		    }		

		else if(key.equals(SettingsInterface.MAX_CONN))
		    {
			try {
			    i = Integer.parseInt(p);
			    try {setMaxConn(i);}
			    catch (IllegalArgumentException ie){}
			}
			catch(NumberFormatException nfe){}
		    }		
		
		else if(key.equals(SettingsInterface.SAVE_DIRECTORY))
		    {
			try {setSaveDirectory(p);}
			catch (IllegalArgumentException ie){}
		    }
		
		else if(key.equals(SettingsInterface.DIRECTORIES))
		    {
			try {setDirectories(p);}
			catch (IllegalArgumentException ie){}
		    }

		else if(key.equals(SettingsInterface.EXTENSIONS))
		    {
			try {setExtensions(p);}
			catch (IllegalArgumentException ie){}
		    }
	       
		else if(key.equals(SettingsInterface.BANNED_IPS))
		    {
			try {setBannedIps(decode(p));}
			catch (IllegalArgumentException ie){}
		    }
 		else if(key.equals(SettingsInterface.BANNED_WORDS))
		    {
			try {setBannedWords(decode(p));}
			catch (IllegalArgumentException ie){}
		    }
		else if(key.equals(SettingsInterface.FILTER_ADULT))
		    {
			boolean bs;
			if (p.equals("true"))
			    bs=true;
			else if (p.equals("false"))
			    bs=false;
			else
			    return;
			try {setFilterAdult(bs);}
			catch (IllegalArgumentException ie){}
		    }
		else if(key.equals(SettingsInterface.FILTER_DUPLICATES))
		    {
			boolean bs;
			if (p.equals("true"))
			    bs=true;
			else if (p.equals("false"))
			    bs=false;
			else
			    return;
			try {setFilterDuplicates(bs);}
			catch (IllegalArgumentException ie){}
		    }
	    }
	    catch(ClassCastException cce){}
	}
    }

    /** Loads in the default values.  Used
     *  when no properties file exists.
     */
    private void loadDefaults()
    {
	setMaxTTL(SettingsInterface.DEFAULT_MAX_TTL);
	setSoftMaxTTL(SettingsInterface.DEFAULT_SOFT_MAX_TTL);	
	setTTL(SettingsInterface.DEFAULT_TTL);	
	setMaxLength(SettingsInterface.DEFAULT_MAX_LENGTH);
	setTimeout(SettingsInterface.DEFAULT_TIMEOUT);
	setHostList(SettingsInterface.DEFAULT_HOST_LIST);
	setKeepAlive(SettingsInterface.DEFAULT_KEEP_ALIVE);
	setPort(SettingsInterface.DEFAULT_PORT);
	setConnectionSpeed(SettingsInterface.DEFAULT_SPEED);
	setSearchLimit(SettingsInterface.DEFAULT_SEARCH_LIMIT);
	setClientID(SettingsInterface.DEFAULT_CLIENT_ID);
	setStats(SettingsInterface.DEFAULT_STATS);
	setMaxConn(SettingsInterface.DEFAULT_MAX_CONN);
	setDirectories(SettingsInterface.DEFAULT_DIRECTORIES);
	setExtensions(SettingsInterface.DEFAULT_EXTENSIONS);
	setBannedIps(SettingsInterface.DEFAULT_BANNED_IPS);
	setBannedWords(SettingsInterface.DEFAULT_BANNED_WORDS);
	setFilterAdult(SettingsInterface.DEFAULT_FILTER_ADULT);
	setFilterDuplicates(SettingsInterface.DEFAULT_FILTER_DUPLICATES);
	try {setSaveDirectory(SettingsInterface.DEFAULT_SAVE_DIRECTORY);}
	catch(IllegalArgumentException e){
	    setSaveDirectory(System.getProperty("user.home"));
	}
    }

    /** returns the time to live */
    public byte getTTL(){return ttl_;}

    /** return the soft maximum time to live */
    public byte getSoftMaxTTL(){return softmaxttl_;}

    /** returns the maximum time to live*/
    public byte getMaxTTL(){return maxttl_;}

    /** returns the maximum allowable length of packets*/
    public int getMaxLength(){return maxLength_;}

    /** returns the timeout value*/
    public int getTimeout(){return timeout_;}

    /** returns a string specifying the full
     *  pathname of the file listing the hosts */
    public String getHostList(){return hostList_;}

    /** returns the keep alive value */
    public int getKeepAlive(){return keepAlive_;}

    /** returns the client's port number */
    public int getPort(){return port_;}

    /** returns the client's connection speed */
    public int getConnectionSpeed(){return connectionSpeed_;}

    /** returns the client's search speed */
    public byte getSearchLimit(){return searchLimit_;}

    /** returns the client id number */
    public String getClientID(){return clientID_;}

    /** returns a boolean specifying whether or not a 
     *  stats file exists */
    public boolean getStats(){return stats_;}

    /** returns the maximum number of connections to hold */
    public int getMaxConn(){return maxConn_;}

    /** returns the directory to save to */
    public String getSaveDirectory(){return saveDirectory_;}

    /** returns the directories to search */
    public String getDirectories(){return directories_;}

    /** returns the string of file extensions*/
    public String getExtensions(){return extensions_;}

    /** returns the string of file extensions*/
    public String[] getBannedIps(){return bannedIps_;}
    public String[] getBannedWords(){return bannedWords_;}
    public boolean getFilterAdult(){return filterAdult_;}
    public boolean getFilterDuplicates(){return filterDuplicates_;}

    /** specialized method for getting the number 
     *  of files scanned */
    public int getFilesScanned()
    {return FileManager.getFileManager().getNumFiles();}

    // SPECIALIZED METHODS FOR NETWORK DISCOVERY
    /** returns the Network Discovery specialized properties file */
    public Properties getNDProps(){return ndProps_;}

    /** returns the path of the properties and host list files */
    public String getPath()
    {
	String s = System.getProperty("user.home");
	s = s + System.getProperty("file.separator");
	return s;
    }

 
    /** sets the time to live */
    public void setTTL(byte ttl) 
	throws IllegalArgumentException
    {
	if(ttl > softmaxttl_ || ttl < 1)
	    throw new IllegalArgumentException();
	else
	    {
		ttl_ = ttl;
		String s = Byte.toString(ttl_);
		props_.setProperty(SettingsInterface.TTL, s);
		writeProperties();
	    }
    }

    /** sets the soft maximum time to live */
    public void setSoftMaxTTL(byte softmaxttl)
	throws IllegalArgumentException
    {
	if(softmaxttl < 0 || softmaxttl > 16)
	    throw new IllegalArgumentException();
	else
	    {
		softmaxttl_ = softmaxttl;
		String s = Byte.toString(softmaxttl);
		props_.setProperty(SettingsInterface.SOFT_MAX_TTL, s);
		writeProperties();
	    }
    }

    /** sets the hard maximum time to live */
    public void setMaxTTL(byte maxttl)
	throws IllegalArgumentException
    {
	if(maxttl < 0 || maxttl > 50)
	    throw new IllegalArgumentException();
	else
	    {
		maxttl_ = maxttl;
		String s = Byte.toString(maxttl_);
		props_.setProperty(SettingsInterface.MAX_TTL, s);
		writeProperties();
	    }
    }

    /** sets the maximum length of packets (spam protection)*/
    public void setMaxLength(int maxLength)
	throws IllegalArgumentException
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		maxLength_ = maxLength;
		String s = Integer.toString(maxLength_);
		props_.setProperty(SettingsInterface.MAX_LENGTH, s);
		writeProperties();
	    }
    }

    /** sets the timeout */
    public void setTimeout(int timeout)
	throws IllegalArgumentException
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		timeout_ = timeout;
		String s = Integer.toString(timeout_);
		props_.setProperty(SettingsInterface.TIMEOUT, s);
		writeProperties();
	    }
		
    }

    /** sets the keep alive */
    public void setKeepAlive(int keepAlive)
	throws IllegalArgumentException
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		keepAlive_ = keepAlive;
		String s = Integer.toString(keepAlive_);
		props_.setProperty(SettingsInterface.KEEP_ALIVE, s);
		writeProperties();
	    }
    }

    /** sets the port to connect on */
    public void setPort(int port) 
	throws IllegalArgumentException
    {
	// if the entered port is outside accepted 
	// port numbers, throw the exception
	if(port > 65536 || port < 0)
	    throw new IllegalArgumentException();
	else
	    {
		port_ = port;
		String s = Integer.toString(port_);
		props_.setProperty(SettingsInterface.PORT, s);
		writeProperties();		    
	    }
    }

    /** sets the connection speed.  throws an
     *  exception if you try to set the speed
     *  far faster than a T3 line or less than
     *  0.*/
    public void setConnectionSpeed(int speed)
    {
	if(speed < 0 || speed > 20000)
	    throw new IllegalArgumentException();
	else	
	    {
		connectionSpeed_ = speed;
		String s = Integer.toString(connectionSpeed_);
		props_.setProperty(SettingsInterface.SPEED, s);
		writeProperties();
	    }
    }

    /** sets the limit for the number of searches 
     *  throws an exception on negative limits 
     *  and limits of 10,000 or more */
    public void setSearchLimit(byte limit)
    {
	if(limit < 0 || limit > 10000)
	    throw new IllegalArgumentException();
	else
	    {
		searchLimit_ = limit;
		String s = Byte.toString(searchLimit_);
		props_.setProperty(SettingsInterface.SEARCH_LIMIT, s);
		writeProperties();
	    }
    }

    /** sets the client (gu) ID number */
    public void setClientID(String clientID)
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		clientID_ = clientID;
		props_.setProperty(SettingsInterface.CLIENT_ID, clientID_);
		writeProperties();
	    }
    }

    /** sets a boolean that specifies 
     *  whether a stats file exists */
    public void setStats(boolean stats)
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		stats_ = stats;
		Boolean b = new Boolean(stats_);
		String s = b.toString();
		props_.setProperty(SettingsInterface.STATS, s);
		writeProperties();
	    }
    }

    /** set the maximum number of connections to hold */
    public void setMaxConn(int maxConn)
    {
	if(maxConn < 0)
	    throw new IllegalArgumentException();
	else
	    {
		maxConn_ = maxConn;		
		String s = Integer.toString(maxConn_);
		props_.setProperty(SettingsInterface.MAX_CONN, s);
		writeProperties();
	    }
    }

    /** set the directory for saving files */
    public void setSaveDirectory(String dir)
    {
	File f = new File(dir);
	boolean b = f.isDirectory();
	if(b == false)
	    throw new IllegalArgumentException();
	else
	    {
		saveDirectory_ = dir;
		props_.setProperty(SettingsInterface.SAVE_DIRECTORY, dir);
		writeProperties();
	    }
    }

    /** set the directories to search */
    public void setDirectories(String dir)
    {	
	if(dir == null)
	    throw new IllegalArgumentException();
	else
	    {
		FileManager.getFileManager().addDirectories(dir);
		directories_ = dir;
		props_.setProperty(SettingsInterface.DIRECTORIES, dir);
		writeProperties();
	    }
    }
    
    /** set the extensions to search for */
    public void setExtensions(String ext)
    {
	if(ext == null)
	    throw new IllegalArgumentException();
	else
	    {
		FileManager.getFileManager().setExtensions(ext);
		extensions_ = ext;			    
		props_.setProperty(SettingsInterface.EXTENSIONS, ext);
		writeProperties();
	    }
    }

    public void setBannedIps(String[] bannedIps)
    {
	if(bannedIps == null)
	    throw new IllegalArgumentException();
	else
	    {
		bannedIps_ = bannedIps;
		props_.setProperty(SettingsInterface.BANNED_IPS, 
				   encode(bannedIps));
		writeProperties();
	    }
    }

    public void setBannedWords(String[] bannedWords)
    {
	if(bannedWords == null)
	    throw new IllegalArgumentException();
	else
	    {
		bannedWords_ = bannedWords;
		props_.setProperty(SettingsInterface.BANNED_WORDS, 
				   encode(bannedWords));
		writeProperties();
	    }
    }

    public void setFilterAdult(boolean filterAdult)
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		filterAdult_ = filterAdult;
		Boolean b = new Boolean(filterAdult);
		String s = b.toString();
		props_.setProperty(SettingsInterface.FILTER_ADULT, s);
		writeProperties();
	    }
    }
    
    public void setFilterDuplicates(boolean filterDuplicates)
    {
	if(false)
	    throw new IllegalArgumentException();
	else
	    {
		filterDuplicates_ = filterDuplicates;
		Boolean b = new Boolean(filterDuplicates);
		String s = b.toString();
		props_.setProperty(SettingsInterface.FILTER_DUPLICATES, s);
		writeProperties();
	    }
    }

    /**
     *  Sets the pathname String for the file that 
     *  lists the default hosts.  This is a unique
     *  method in that the host list cannot be set
     *  in the properties file
     */
    private void setHostList(String hostList)
    {		
	String fileName = System.getProperty("user.home");
	fileName = fileName + System.getProperty("file.separator");
	fileName = fileName + hostList;
	File f = new File(fileName);
	if(f.isFile() == true)
	    hostList_ = fileName;
	else
	    {		
		try{
		    FileWriter fw = new FileWriter(fileName);
		    hostList_ = fileName;
		}
		catch(IOException e){
		    // not sure what to do if the filewriter
		    // fails to create a file
		}		
	    }
    }


    /** writes out the Network Discovery specialized 
     *  properties file
     */
    public void writeNDProps()
    {
	try {
	    FileOutputStream ostream = new FileOutputStream(ndFileName_);
	    props_.store(ostream, "Properties file for Network Discovery");
	    ostream.close();
	} 
	catch (Exception e){}
    }

    /** writes out the properties file to with the specified
     *  name in the user's home directory
     */
    public void writeProperties()
    {
        try {
	    FileOutputStream ostream = new FileOutputStream(fileName_);
	    props_.store(ostream, SettingsInterface.HEADER);
	    ostream.close();
	} 
	catch (Exception e){}
    }

    private static final String STRING_DELIMETER=";";
    
    /**  Returns a string encoding of array. Inverse of decode. */
    private static String encode(String[] array) {
	//TODO1: ";" ==> "\;"
	StringBuffer buf=new StringBuffer();
	for (int i=0; i<(array.length-1); i++) { //don't put ";" after last word
	    buf.append(array[i]);
	    buf.append(STRING_DELIMETER);
	}
	if (array.length!=0)
	    buf.append(array[array.length-1]); //add last word
	return buf.toString();
    }

    /** Returns the array encoded in s.  Inverse of encode. */
    private static String[] decode(String s) {
	//TODO1: "\;" ==> ";"
	StringTokenizer lexer=new StringTokenizer(s,STRING_DELIMETER);
	Vector buf=new Vector();
	while (lexer.hasMoreTokens())
	    buf.add(lexer.nextToken());
	String[] ret=new String[buf.size()];
	buf.copyInto(ret);
	return ret;
    }    

//      /** Unit test */
//      public static void main(String args[]) {
//  	String[] original=new String[] {"a", "bee", "see"};
//  	String encoded=encode(original);
//  	String[] decoded=decode(encoded);
//  	Assert.that(Arrays.equals(original, decoded));

//  	original=new String[] {"a"};
//  	encoded=encode(original);
//  	decoded=decode(encoded);
//  	Assert.that(Arrays.equals(original, decoded));

//  	original=new String[] {};
//  	encoded=encode(original);
//  	decoded=decode(encoded);
//  	Assert.that(Arrays.equals(original, decoded));
//      }
}














