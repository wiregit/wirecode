package com.limegroup.gnutella.version;

import java.util.StringTokenizer;

import org.limewire.util.OSUtils;


/**
 * An abstraction for representing an operating system.
 */
class OS {
    
    /**
     * The string representation of the OS.
     */
    private final String os;
    
    /**
     * Whether or not the OS of this machine is a match.
     */
    private final boolean acceptable;
    
    /**
     * Constructs a new OS based on the given string representation.
     */
    OS(String s) {
        this.os = s;
        this.acceptable = accept(s.toLowerCase());
    }
    
    /**
     * Returns the OS as a string.
     */
    public String toString() {
        return os;
    }
    
    /**
     * Determines if the current machine's OS is a match for what this OS
     * object is representing.
     */
    public boolean isAcceptable() {
        return acceptable;
    }
    
    /**
     * Creates an array of OSes from a comma delimited list of strings.
     * Whitespace is ignored.
     */
    static OS[] createFromList(String oses) {
        StringTokenizer st = new StringTokenizer(oses, ",");
        OS[] all = new OS[st.countTokens()];
        for(int i = 0; st.hasMoreTokens(); i++) {
            all[i] = new OS(st.nextToken().trim());
        }
        return all;
    }
    
    /**
     * Determines if any OS object in the array matches the current machine.
     */
    static boolean hasAcceptableOS(OS[] oses) {
        for(int i = 0; i < oses.length; i++)
            if(oses[i].isAcceptable())
                return true;
        return false;
    }
    
    /**
     * Prints out a comma separated list of the OSes.
     */
    static String toString(OS[] oses) {
        if (oses == null)
            return "";
        
        String s = "";
        for(int i = 0; i < oses.length; i++) {
            s += oses[i].toString();
            if( i < oses.length - 2)
                s += ", ";
        }
        return s;
    }
    
    /**
     * Determines whether or not the current machine matches the string representation
     * of an OS.
     *
     * An exact match of System.getProperty("os.name") is allowed, as are the special:
     * "windows", "mac", "linux" and "unix" values (representing all OSes that are of
     * that variety).  "other" is allowed, representing all OSes not of those varieties.
     * "*" is also allowed, representing all OSes.
     */
    private boolean accept(String s) {
        String os = OSUtils.getOS().toLowerCase();
        if(s.equals(os))
            return true;
        
        if("windows".equals(s))
            return OSUtils.isWindows();
        else if("mac".equals(s))
            return OSUtils.isAnyMac();
        else if("linux".equals(s))
            return OSUtils.isLinux();
        else if("unix".equals(s))
            return OSUtils.isUnix() && !OSUtils.isLinux();
        else if("other".equals(s))
            return !OSUtils.isWindows() && !OSUtils.isAnyMac() &&
                   !OSUtils.isUnix() && !OSUtils.isLinux();
        else if("*".equals(s))
            return true;
        
        return false;
    }
}