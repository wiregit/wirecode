/**
 * This class handles user notification events on Windows.  It forwards
 * events to native c++ code that reduces LimeWire to the system tray.
 *
 * @author Adam Fisk
 */
 //2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

import com.limegroup.gnutella.gui.notify.NotifyUser;
import com.limegroup.gnutella.gui.notify.NotifyCallback;

public class WindowsNotifyUser implements NotifyUser {
	
	/**
	 * @requires that we are running on Windows.
	 */
	public WindowsNotifyUser() {
		try {
			System.loadLibrary("LimeWire");
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
	}
	
	/**
	 * implements the NotifyUser interface.
	 */
    public void addNotify () {
		try {
			nativeAddNotify();
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
	}

	/**
	 * implements the NotifyUser interface.
	 */
    public void removeNotify () {
		try {
			nativeRemoveNotify();
		} catch(UnsatisfiedLinkError ule) {
			ule.printStackTrace();
		}
	}

	/**
	 * implements the NotifyUser interface.
	 */
    public void installNotifyCallback(NotifyCallback callback) {
		try {
			nativeInstallNotifyCallback(callback);
		} catch(UnsatisfiedLinkError ule) {
		}
	}

	/**
	 * makes the call to the associated method in the native code.
	 */
    private native void nativeAddNotify ();

	/**
	 * makes the call to the associated method in the native code.
	 */
    private native void nativeRemoveNotify ();

	/**
	 * makes the call to the associated method in the native code.
	 */
    private native void nativeInstallNotifyCallback(NotifyCallback callback);	
}
