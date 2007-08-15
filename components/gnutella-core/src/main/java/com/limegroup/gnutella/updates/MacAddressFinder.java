package com.limegroup.gnutella.updates;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.limewire.util.OSUtils;


/**
 * Finds out the Mac address of the machine. 
 * @author Sumeet Thadani
 */

public class MacAddressFinder {
    
    /**
     * Package access. Find the Mac address of the machine depending on the 
     * operating system.
     * <p>
     * @return null if we cannot find out. 
     */
    String getMacAddress() {
        try {
            if(OSUtils.isWindows()) {
                return getWindowsMac();
            }
            else if(OSUtils.isMacOSX()) {
                return getOSXMac();
            }
            else if(OSUtils.isSolaris()) {
                return getSolarisMac();
            }
            else if(OSUtils.isLinux()) {
                getLinuxMac();
            }
            else {
                return null;
            }
        } catch (IOException iox) {
            return null;
        }
        return null;
    }
    
    private String getWindowsMac() throws IOException {
        String result = runCommand("ipconfig /all");
        return parseResult(result,":");
    }

    private String getOSXMac() throws IOException {
        String result = runCommand("ifconfig -a");
        return parseResult(result,"ether");
    }   

    private String getLinuxMac() throws IOException {
        String result = runCommand("LANG=C /sbin/ifconfig");
        if(result.length()<17)//unknown result, but it's gotta be bigger than 17
            result = runCommand("LANG=C /bin/ifconfig");
        if(result.length() < 17) //need to try another?
            result = runCommand("LANG=C ifconfig");//getting desperate here.
        return parseResult(result,"hwaddr");
    }   

    private String getSolarisMac() throws IOException {
        String result = runCommand("ifconfig -a");//TODO1: correct command?
        return parseResult(result,"ether");//TODO1: correct delimiter?
    }   

    private String parseResult(String result, String delimiter) {
        result = result.toLowerCase();//lets ignore all case
        StringTokenizer tok = new StringTokenizer(result,"\n");
        while(tok.hasMoreTokens()) {//for each line of result
            String line = tok.nextToken();
            int index = line.indexOf(delimiter);
            if(index >= 0) {//the line contains the delimiter
                String address=line.substring(index+delimiter.length()).trim();
                //address contains the rest of the line after the delimiter.
                address = canonicalizeMacAddress(address);
                if(address!=null)
                    return address;//null if in bad form
            }
        }
        return null;
    }
    

    private String canonicalizeMacAddress(String address) {
        if(address.length()!=17)
            return null;
        //check that we have six pair of numbers, separated by : or -
        StringBuilder ret = new StringBuilder();
        StringTokenizer tok = new StringTokenizer(address,":.-");
        for(int i=0; i<6;i++) {
            String val=null;
            try { 
                val = tok.nextToken();
                if(val.length()!=2)
                    return null;
            } catch (NoSuchElementException nsex) {
                return null;
            } 
            ret.append(val);
            if(i<5)
                ret.append("-");
        }
        return ret.toString();
    }

    /**
     * @return the results of the command we just ran
     * @param command the command - platform dependent.
     */
    private String runCommand(String command) throws IOException {
        //TODO1: make sure the path is set correctly, or we are not going to be
        //able to execute the command
        Process process = Runtime.getRuntime().exec(command);
        InputStream iStream = new BufferedInputStream(process.getInputStream());
        StringBuilder buffer = new StringBuilder();//store the resutls
        while(true) {
            int c = iStream.read();
            if(c==-1) //eof?
                break;
            buffer.append((char)c);
        }//buffer has all the data from the command.
        iStream.close();
        return buffer.toString();        
    }
    

    public static void main(String[] args) {
        MacAddressFinder f = new MacAddressFinder();
        System.out.println("The mac address is "+f.getMacAddress());
    }
}
