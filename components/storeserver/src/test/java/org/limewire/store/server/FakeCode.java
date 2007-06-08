package org.limewire.store.server;

import java.util.Map;

import javax.swing.JOptionPane;

import org.limewire.store.server.LocalServerDelegate;
import org.limewire.store.server.AbstractRemoteServer;
import org.limewire.store.server.ServerImpl;


/**
 * This class represents fake javascript code that can
 * communicate to the remote server and local server on the client.
 * 
 * @author jpalm
 */
final class FakeCode {

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
	
	FakeCode(ServerImpl local, AbstractRemoteServer remote) {
		this.toLocalServer = new LocalServerDelegate("localhost", local.getPort());
		this.toRemoteServer = new LocalServerDelegate("localhost", remote.getPort());
	}
	
	protected final void sendLocalMsg(String msg, Map<String, String> args, Handler h) {
		h.handle(removeHeaders(toLocalServer.sendMsgToRemoteServer(msg, args)));
	}

	protected final void sendRemoteMsg(String msg, Map<String, String> args, Handler h) {
		h.handle(removeHeaders(toRemoteServer.sendMsgToRemoteServer(msg, args)));
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
