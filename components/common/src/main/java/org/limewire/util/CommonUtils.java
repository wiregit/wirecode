package org.limewire.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CommonUtils {
    
    /**
     * Several arrays of illegal characters on various operating systems.
     * Used by convertFileName
     */
    private static final char[] ILLEGAL_CHARS_ANY_OS = {
        '/', '\n', '\r', '\t', '\0', '\f' 
    };
    private static final char[] ILLEGAL_CHARS_UNIX = {'`'};
    private static final char[] ILLEGAL_CHARS_WINDOWS = { 
        '?', '*', '\\', '<', '>', '|', '\"', ':'
    };
    private static final char[] ILLEGAL_CHARS_MACOS = {':'};
    

    /**
     * Returns the user home directory.
     *
     * @return the <tt>File</tt> instance denoting the abstract pathname of
     *  the user's home directory, or <tt>null</tt> if the home directory
     *  does not exist
     */
    public static File getUserHomeDir() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Return the user's name.
     *
     * @return the <tt>String</tt> denoting the user's name.
     */
    public static String getUserName() {
        return System.getProperty("user.name");
    }

    /**
     * Gets a resource file using the CommonUtils class loader,
     * or the system class loader if CommonUtils isn't loaded.
     */
    public static File getResourceFile(String location) {
        ClassLoader cl = CommonUtils.class.getClassLoader();            
        URL resource = null;
    
        if(cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }
        
        if( resource == null ) {
            // note: this will probably not work,
            // but it will ultimately trigger a better exception
            // than returning null.
            return new File(location);
        }
        
        //NOTE: The resource URL will contain %20 instead of spaces.
        // This is by design, but will not work when trying to make a file.
        // See BugParadeID: 4466485
        //(http://developer.java.sun.com/developer/bugParade/bugs/4466485.html)
        // The recommended workaround is to use the URI class, but that doesn't
        // exist until Java 1.4.  So, we can't use it here.
        // Thus, we manually have to parse out the %20s from the URL
        return new File( decode(resource.getFile()) );
    }

    /**
     * Gets an InputStream from a resource file.
     * 
     * @param location the location of the resource in the resource file
     * @return an <tt>InputStream</tt> for the resource
     * @throws IOException if the resource could not be located or there was
     *  another IO error accessing the resource
     */
    public static InputStream getResourceStream(String location) 
      throws IOException {
       ClassLoader cl = CommonUtils.class.getClassLoader();            
       URL resource = null;
    
        if(cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }
        
        if( resource == null) 
            throw new IOException("null resource: "+location);
        else
            return resource.openStream();
    }

    /**
     * Copied from URLDecoder.java
     */
    public static String decode(String s) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char)Integer.parseInt(
                                        s.substring(i+1,i+3),16));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(s);
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        // Undo conversion to external encoding
        String result = sb.toString();
        try {
            byte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } catch (UnsupportedEncodingException e) {
            // The system should always have 8859_1
        }
        return result;
    }

    /**
     * Copies the specified resource file into the current directory from
     * the jar file. If the file already exists, no copy is performed.
     *
     * @param fileName the name of the file to copy, relative to the jar 
     *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
     */
    public static void copyResourceFile(final String fileName) {
        copyResourceFile(fileName, null);
    }

    /**
     * Copies the specified resource file into the current directory from
     * the jar file. If the file already exists, no copy is performed.
     *
     * @param fileName the name of the file to copy, relative to the jar
     *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
     * @param newFile the new <tt>File</tt> instance where the resource file
     *  will be copied to
     */
    public static void copyResourceFile(final String fileName, File newFile) {
        copyResourceFile(fileName, newFile, false);		
    }

    /**
     * Copies the specified resource file into the current directory from
     * the jar file. If the file already exists, no copy is performed.
     *
     * @param fileName the name of the file to copy, relative to the jar 
     *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
     * @param newFile the new <tt>File</tt> instance where the resource file
     *  will be copied to -- if this argument is null, the file will be
     *  copied to the current directory
     * @param forceOverwrite specifies whether or not to overwrite the 
     *  file if it already exists
     */
    public static void copyResourceFile(final String fileName, File newFile, 
    									final boolean forceOverwrite) {
    	if(newFile == null) newFile = new File(".", fileName);
    
    	// return quickly if the file is already there, no copy necessary
    	if( !forceOverwrite && newFile.exists() )
            return;
        
    	String parentString = newFile.getParent();
        if(parentString == null)
            return;
        
    	File parentFile = new File(parentString);
    	if(!parentFile.isDirectory())
    		parentFile.mkdirs();
    
    	ClassLoader cl = CommonUtils.class.getClassLoader();			
    	
    	BufferedInputStream bis = null;
    	BufferedOutputStream bos = null;            
    	try {
    		//load resource using my class loader or system class loader
    		//Can happen if Launcher loaded by system class loader
            URL resource = cl != null
    			?  cl.getResource(fileName)
    			:  ClassLoader.getSystemResource(fileName);
                
            if(resource == null)
                throw new NullPointerException("resource: " + fileName +
                                               " doesn't exist.");
            
            InputStream is = resource.openStream();
    		
    		//buffer the streams to improve I/O performance
    		final int bufferSize = 2048;
    		bis = new BufferedInputStream(is, bufferSize);
    		bos = 
    			new BufferedOutputStream(new FileOutputStream(newFile), 
    									 bufferSize);
    		byte[] buffer = new byte[bufferSize];
    		int c = 0;
    		
    		do { //read and write in chunks of buffer size until EOF reached
    			c = bis.read(buffer, 0, bufferSize);
                if (c > 0)
                    bos.write(buffer, 0, c);
    		}
    		while (c == bufferSize); //(# of bytes read)c will = bufferSize until EOF
    		
    	} catch(IOException e) {	
            e.printStackTrace();
    		//if there is any error, delete any portion of file that did write
    		newFile.delete();
    	} finally {
            if(bis != null) {
                try {
                    bis.close();
                } catch(IOException ignored) {}
            }
            if(bos != null) {
                try {
                    bos.close();
                } catch(IOException ignored) {}
            }
    	} 
    }

    /**
     * Returns the stack traces of all current Threads.
     */
    public static String getAllStackTraces() {
        try {
            Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
            
            List<Map.Entry<Thread, StackTraceElement[]>> sorted =
                new ArrayList<Map.Entry<Thread, StackTraceElement[]>>(map.entrySet());
            Collections.sort(sorted, new Comparator<Map.Entry<Thread, StackTraceElement[]>>() {
                public int compare(Map.Entry<Thread, StackTraceElement[]> a, Map.Entry<Thread, StackTraceElement[]> b) {
                    return a.getKey().getName().compareTo(b.getKey().getName());
                }
            });
            
            StringBuilder buffer = new StringBuilder();
            for(Map.Entry<Thread, StackTraceElement[]> entry : sorted) {
                Thread key = entry.getKey();
                StackTraceElement[] value = entry.getValue();
                
                buffer.append(key.getName()).append("\n");
                for(int i = 0; i < value.length; i++) {
                    buffer.append("    ").append(value[i]).append("\n");
                }
                buffer.append("\n");
            }
            
            // Remove the last '\n'
            if (buffer.length() > 0) {
                buffer.setLength(buffer.length()-1);
            }
            
            return buffer.toString();
        } catch (Exception err) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("An error occured during getting the StackTraces of all active Threads");
            err.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        }
    }

    /**
     * Converts a value in seconds to:
     *     "d:hh:mm:ss" where d=days, hh=hours, mm=minutes, ss=seconds, or
     *     "h:mm:ss" where h=hours<24, mm=minutes, ss=seconds, or
     *     "m:ss" where m=minutes<60, ss=seconds
     */
    public static String seconds2time(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        int hours = minutes / 60;
        minutes = minutes - hours * 60;
        int days = hours / 24;
        hours = hours - days * 24;
        // build the numbers into a string
        StringBuilder time = new StringBuilder();
        if (days != 0) {
            time.append(Integer.toString(days));
            time.append(":");
            if (hours < 10) time.append("0");
        }
        if (days != 0 || hours != 0) {
            time.append(Integer.toString(hours));
            time.append(":");
            if (minutes < 10) time.append("0");
        }
        time.append(Integer.toString(minutes));
        time.append(":");
        if (seconds < 10) time.append("0");
        time.append(Integer.toString(seconds));
        return time.toString();
    }

    /** 
     * Replaces OS specific illegal characters from any filename with '_', 
     * including ( / \n \r \t ) on all operating systems, ( ? * \  < > | " ) 
     * on Windows, ( ` ) on unix.
     *
     * @param name the filename to check for illegal characters
     * @return String containing the cleaned filename
     */
    public static String convertFileName(String name) {
    	
    	// ensure that block-characters aren't in the filename.
        name = I18NConvert.instance().compose(name);
    
    	// if the name is too long, reduce it.  We don't go all the way
    	// up to 256 because we don't know how long the directory name is
    	// We want to keep the extension, though.
    	if(name.length() > 180) {
    	    int extStart = name.lastIndexOf('.');
    	    if ( extStart == -1) { // no extension, wierd, but possible
    	        name = name.substring(0, 180);
    	    } else {
    	        // if extension is greater than 11, we concat it.
    	        // ( 11 = '.' + 10 extension characters )
    	        int extLength = name.length() - extStart;		        
    	        int extEnd = extLength > 11 ? extStart + 11 : name.length();
    		    name = name.substring(0, 180 - extLength) +
    		           name.substring(extStart, extEnd);
            }          
    	}
        for (int i = 0; i < ILLEGAL_CHARS_ANY_OS.length; i++) 
            name = name.replace(ILLEGAL_CHARS_ANY_OS[i], '_');
    	
        if ( OSUtils.isWindows() || OSUtils.isOS2() ) {
            for (int i = 0; i < ILLEGAL_CHARS_WINDOWS.length; i++) 
                name = name.replace(ILLEGAL_CHARS_WINDOWS[i], '_');
        } else if ( OSUtils.isLinux() || OSUtils.isSolaris() ) {
            for (int i = 0; i < ILLEGAL_CHARS_UNIX.length; i++) 
                name = name.replace(ILLEGAL_CHARS_UNIX[i], '_');
        } else if (OSUtils.isMacOSX()) {
            for(int i = 0; i < ILLEGAL_CHARS_MACOS.length; i++)
                name = name.replace(ILLEGAL_CHARS_MACOS[i], '_');
        }
        
        return name;
    }

}
