/**
 * 
 */
package org.limewire.ui.swing.browser;

import org.mozilla.interfaces.nsICancelable;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsIMIMEInfo;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;
import org.mozilla.interfaces.nsIWebProgress;

final class NoOpMozillaDownload implements nsIDownload {
	@Override
	public long getAmountTransferred() {
		throw new UnsupportedOperationException();
	}

	@Override
	public nsICancelable getCancelable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDisplayName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getId() {
		// This currently gets called by mozilla, the exception gets eaten if we
		// throw an UnsupportedOperationException,
		// returning hashcode as id for now. since we are overriding the
		// download manager, this id should not be used anywhere theoretically
		return hashCode();
	}

	@Override
	public nsIMIMEInfo getMIMEInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPercentComplete() {
		throw new UnsupportedOperationException();
	}

	@Override
	public nsIURI getReferrer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getResumable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public nsIURI getSource() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getSpeed() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getStartTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public nsIURI getTarget() {
		throw new UnsupportedOperationException();
	}

	@Override
	public nsILocalFile getTargetFile() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void init(nsIURI arg0, nsIURI arg1, String arg2, nsIMIMEInfo arg3,
			double arg4, nsILocalFile arg5, nsICancelable arg6) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onProgressChange64(nsIWebProgress arg0, nsIRequest arg1,
			long arg2, long arg3, long arg4, long arg5) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onRefreshAttempted(nsIWebProgress arg0, nsIURI arg1,
			int arg2, boolean arg3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onLocationChange(nsIWebProgress arg0, nsIRequest arg1,
			nsIURI arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onProgressChange(nsIWebProgress arg0, nsIRequest arg1,
			int arg2, int arg3, int arg4, int arg5) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onSecurityChange(nsIWebProgress arg0, nsIRequest arg1, long arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onStateChange(nsIWebProgress arg0, nsIRequest arg1, long arg2,
			long arg3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onStatusChange(nsIWebProgress arg0, nsIRequest arg1, long arg2,
			String arg3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public nsISupports queryInterface(String arg0) {
		throw new UnsupportedOperationException();
	}
}