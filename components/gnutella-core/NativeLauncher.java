/**
 * Wrapper for calls to native code that launches files in their 
 * associated applications.
 *
 * @author Adam Fisk
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|


public class NativeLauncher {

	/**
	 * flag for whether or not we succssfully loaded the native
	 * library.
	 */
	private boolean _loadSucceeded = true;

	public NativeLauncher() {
		// load the native libary only if we're on Windows
		String os = System.getProperty("os.name");
		if(os.indexOf("Windows") != -1) {
			try {
				System.loadLibrary("LimeWire");
			} catch(UnsatisfiedLinkError ule) {
				_loadSucceeded = false;
				ule.printStackTrace();
			}
		}
	}

	/**
	 * launches the file with it's associated application on Windows. 
	 * @requires that we are running on Windows and that the String 
	 *  specify a valid file pathname or a valid URL.
	 */
	public int launchFileWindows(String name) {
		if(!_loadSucceeded) return -1;
		int launchCode = -1;
		try {
			launchCode = nativeLaunchFileWindows(name);
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
		return launchCode;
	}

	/** 
	 * native method for launching the specific file.
	 */
	private static native int nativeLaunchFileWindows(String name);
}
