package com.limegroup.gnutella.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A collection of core-related systems utilities,
 * most of which will require native code to do correctly.
 */
public class SystemUtils {
    
	private static final Log LOG = LogFactory.getLog(SystemUtils.class);
	
	//	BITFIELD values for ICF staus
	public static final int ST_NONE					=	0x0000;
	
	//	FW STATUS
	public static final int ST_FIREWALL_ENABLED		=	0x0001;
	public static final int ST_PROCESS_ENABLED		=	0x0002;
	public static final int ST_PORT_ENABLED			=	0x0004;
	
	//	FW POPUP STATUS
	public static final int ST_POPUP_IMMINENT		=	0x0008;
	
	//	ERRORS
	public static final int ST_UNSUPPORTED_OPERATION=	0x0010;		//	Attempted to call under WinVer < WinXP SP2
	public static final int ST_ERR_GET_GLOBAL_PORTS	=	0x0020;
	public static final int ST_ERR_ADD				=	0x0040;		//	Used for adding Process and Port
	public static final int ST_ERR_PUT				=	0x0080;		//	Used for Filename; Friendly name; Port; & Protocol
	public static final int ST_ERR_GET_ENABLED		=	0x0100;		//	Used for both "Process GetEnabled" and "Port GetEnabled"
	public static final int ST_ERR_ALLOCATION		=	0x0200;		//	Out of memory
	public static final int ST_ERR_GET_AUTH_APPS	=	0x0400;
	public static final int ST_ERR_GET_FW_ENABLED	=	0x0800;
	public static final int ST_ERR_GET_CUR_PROFILE	=	0x1000;
	public static final int ST_ERR_GET_LOCAL_POLICY	=	0x2000;
	public static final int ST_ERR_CO_CREATE_INST	=	0x4000;		//	Used for all COM creation calls
	public static final int ST_ERR_COM_INIT			=	0x8000;
		
    /**
     * Whether or not the native libraries could be loaded.
     */
    private static boolean isLoaded;
    
    static {
        boolean canLoad;
        try {
            // Only load the library on systems where we've made it.
            if(CommonUtils.isMacOSX()) {
                System.loadLibrary("SystemUtilities");
            } else if(CommonUtils.isWindows()) {
                System.loadLibrary("GenericWindowsUtils");
                if(CommonUtils.isWindows2000orXP()) {
                    System.loadLibrary("WindowsV5PlusUtils");
                }
            }
            canLoad = true;
        } catch(UnsatisfiedLinkError noGo) {
        	LOG.error( "Couldn't load system library", noGo );
            canLoad = false;
        }
        isLoaded = canLoad;
    }
    
    private SystemUtils() {}
    
    
    /**
     * Retrieves the amount of time the system has been idle, where
     * idle means the user has not pressed a key, mouse button, or moved
     * the mouse.  The time returned is in milliseconds.
     */
    public static long getIdleTime() {
        if(supportsIdleTime()) 
            return idleTime();
        
        return 0;
    }
    
    /**
     * Returns whether or not the idle time function is supported on this
     * operating system.
     * 
     * @return <tt>true</tt> if we're able to determine the idle time on this
     *  operating system, otherwise <tt>false</tt>
     */
    public static boolean supportsIdleTime() {
        if(isLoaded) {
            if(CommonUtils.isWindows2000orXP())
                return true;
            else if(CommonUtils.isMacOSX())
                return true;
        }
            
        return false;
    }
    
    
    /**
     * 
     * @return a bitfield indicating the status 
     */
     public static int getWinICFStatus(String appPath){
    	if(supportsWinICFStatus())
    		return isFirewallWarningImminent(appPath);
   	
    	return 0;
    }
 
    /**
     * @return true if WinXP ICF status support exists
     */
    public static boolean supportsWinICFStatus(){
    	if(isLoaded){
    		if(CommonUtils.isWindowsXP())
    			return true;
    	}
    	return false;
    }
    
    /**
     * Sets the number of open files, if supported.
     */
    public static long setOpenFileLimit(int max) {
        if(isLoaded && CommonUtils.isMacOSX())
            return setOpenFileLimit0(max);
        else
            return -1;
    }
    
    /**
     * Sets a file to be writeable.  Package-access so FileUtils can delegate
     * the filename given should ideally be a canonicalized filename.
     */
    static void setWriteable(String fileName) {
        if(isLoaded && (CommonUtils.isWindows() || CommonUtils.isMacOSX()))
            setFileWriteable(fileName);
    }
    
    public static void main(String[] args) {
    	System.out.println("I AM RUNNING.");
    	
    	char c=isFirewallWarningImminent("c:\\Program Files\\LimeWire\\LimeWire.exe");
    	System.out.println("Imminent: " + ( (c&0x0008)>0?("true"):("false") ) );
    	System.out.println("Return Value: 0x" + Integer.toHexString(c).toUpperCase() );
    }
            
    
    private static final native long idleTime();
    private static final native int setFileWriteable(String filename);
    private static final native int setOpenFileLimit0(int max);
    private static final native char isFirewallWarningImminent(String appPath);
}
