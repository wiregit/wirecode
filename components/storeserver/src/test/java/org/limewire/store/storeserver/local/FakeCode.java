package org.limewire.store.storeserver.local;

import java.util.Map;

import javax.swing.JOptionPane;

import org.limewire.store.storeserver.core.ServerImpl;
import org.limewire.store.storeserver.core.RemoteServer;


/**
 * This class represents fake javascript code that can
 * communicate to the remote server and local server on the client.
 * 
 * @author jpalm
 */
public class FakeCode {

	private final LocalServerDelegate toLocalServer;
	private final LocalServerDelegate toRemoteServer;
	
	interface Handler {

		public final static Handler ALERT = new Handler() {
			public void handle(String res) {
				JOptionPane.showMessageDialog(null, res);
			}
			
		};

		void handle(String res);
	}
	
	FakeCode(ServerImpl local, RemoteServer remote) {
		this.toLocalServer = new LocalServerDelegate(remote, local.getPort(), false);
		this.toRemoteServer = new LocalServerDelegate(local, remote.getPort(), false);
	}
	
	protected final void sendLocalMsg(String msg, Map<String, String> args, Handler h) {
		h.handle(removeHeaders(this.toLocalServer.sendMsg(msg, args)));
	}

	protected final void sendRemoteMsg(String msg, Map<String, String> args, Handler h) {
		h.handle(removeHeaders(this.toRemoteServer.sendMsg(msg, args)));
	}
	
	private String removeHeaders(String response) {
		if (response == null) return null;
		//
		// Search for two NEWLINEs
		// This totally sucks, but it's just testing
		//
		String target = "\n\n"; //todo //Constants.NEWLINE; // + Constants.NEWLINE;
		int itarget = response.indexOf(target);
		String res = itarget == -1 ? response : response.substring(itarget+target.length());
		res = res.trim();
		return res;
	}
}
