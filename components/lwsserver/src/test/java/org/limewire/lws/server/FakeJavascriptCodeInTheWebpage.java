package org.limewire.lws.server;

import java.util.Map;

import javax.swing.JOptionPane;

import org.limewire.lws.server.AbstractRemoteServer;
import org.limewire.lws.server.LocalServerDelegate;
import org.limewire.lws.server.ServerImpl;


/**
 * This class represents fake javascript code that can
 * communicate to the remote server and local server on the client.
 */
final class FakeJavascriptCodeInTheWebpage {

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
	
	FakeJavascriptCodeInTheWebpage(ServerImpl local, AbstractRemoteServer remote) {
		this.toLocalServer = new LocalServerDelegate("localhost", local.getPort());
		this.toRemoteServer = new LocalServerDelegate("localhost", remote.getPort());
	}
	
	protected final void sendLocalMsg(String msg, Map<String, String> args, Handler h) {
		h.handle(removeHeaders(toLocalServer.semdMessageToServer(msg, args)));
	}

	protected final void sendRemoteMsg(String msg, Map<String, String> args, Handler h) {
		h.handle(removeHeaders(toRemoteServer.semdMessageToServer(msg, args)));
	}
	
	private String removeHeaders(String response) {
		if (response == null) return null;
		//
		// Search for two NEWLINEs
		// This totally sucks, but it's just testing
		//
		String target = "\n\n";
		int itarget = response.indexOf(target);
		String res = itarget == -1 ? response : response.substring(itarget+target.length());
		res = res.trim();
		return res;
	}
}
