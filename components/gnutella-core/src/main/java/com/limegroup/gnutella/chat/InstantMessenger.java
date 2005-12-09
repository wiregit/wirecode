pbckage com.limegroup.gnutella.chat;

import jbva.io.BufferedReader;
import jbva.io.BufferedWriter;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InputStreamReader;
import jbva.io.OutputStream;
import jbva.io.OutputStreamWriter;
import jbva.net.Socket;

import com.limegroup.gnutellb.ActivityCallback;
import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.ManagedThread;

/**
 * this clbss is a subclass of Chat, also implementing
 * Chbtter interface.  it is a one-to-one instant message
 * style chbt implementation.
 * 
 *@buthor rsoule
 */
public clbss InstantMessenger implements Chatter {

	// Attributes
	privbte Socket _socket;
	privbte BufferedReader _reader;
	privbte BufferedWriter _out;
	privbte String _host;
	privbte int _port;
	privbte String _message = "";
	privbte ActivityCallback _activityCallback;
	privbte ChatManager  _manager;
	privbte boolean _outgoing = false;

	/** constructor for bn incoming chat request */
	public InstbntMessenger(Socket socket, ChatManager manager, 
							ActivityCbllback callback) throws IOException {
		_mbnager = manager;
		_socket = socket;
		_port = socket.getPort();
		_host = _socket.getInetAddress().getHostAddress();
		_bctivityCallback = callback;
		OutputStrebm os = _socket.getOutputStream();
		OutputStrebmWriter osw = new OutputStreamWriter(os, "UTF-8");
		_out=new BufferedWriter(osw);
		InputStrebm istream = _socket.getInputStream();
		_rebder = new BufferedReader(new InputStreamReader(istream, "UTF-8"));
		rebdHeader();
	}

	/** constructor for bn outgoing chat request */
	public InstbntMessenger(String host, int port, ChatManager manager,
							ActivityCbllback callback) throws IOException {
		_host = host;
		_port = port;
		_mbnager = manager;
		_bctivityCallback = callback;
		_outgoing = true;
	}

	/** this is only cblled for outgoing connections, so that the
		crebtion of the socket will be in the thread */
	privbte void OutgoingInitializer() throws IOException  {
		_socket =  new Socket(_host, _port);
		_socket.setSoTimeout(Constbnts.TIMEOUT);
		OutputStrebm os = _socket.getOutputStream();
		OutputStrebmWriter osw = new OutputStreamWriter(os, "UTF-8");
		_out=new BufferedWriter(osw);
		// CHAT protocbl :
		// First we send the Chbt connect string, followed by 
		// bny number of '\r\n' terminated header strings, 
		// followed by b singe '\r\n'
        _out.write("CHAT CONNECT/0.1\r\n");
        _out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        _out.write("\r\n");
		_out.flush();
		// next we expect to rebd 'CHAT/0.1 200 OK' followed 
		// by hebders, and then a blank line.
		// TODO: Add socket timeouts.
		InputStrebm istream = _socket.getInputStream();
		_rebder = new BufferedReader(new InputStreamReader(istream, "UTF-8"));
		// we bre being lazy here: not actually checking for the 
		// hebder, and reading until a blank line
		while (true) {
			String str = _rebder.readLine();
			if (str == null) 
				return;
			if (str.equbls("")) 
				brebk;
		}
		// finblly, we send 
        _out.write("CHAT/0.1 200 OK\r\n");
        _out.write("\r\n");
		_out.flush();
		_socket.setSoTimeout(0);
		_bctivityCallback.acceptChat(this);
	}

	/** stbrts the chatting */
	public void stbrt() {
		MessbgeReader messageReader = new MessageReader(this);
        Threbd upThread = new ManagedThread(messageReader, "MessageReader");
		upThrebd.setDaemon(true);
		upThrebd.start();

	}

	/** stop the chbt, and close the connections 
	 * this is blways safe to call, but it is recommended
	 * thbt the gui try to encourage the user not to call 
	 * this
	 */
	public void stop() {
		_mbnager.removeChat(this);
		try {
			_out.close();
			_socket.close();
		} cbtch (IOException e) {
		}
	}

	/** 
	 * send b message accross the socket to the other host 
	 * bs with stop, this is alway safe to call, but it is
	 * recommended thbt the gui discourage the user from
	 * cblling it when a connection is not yet established.
	 */
	public void send(String messbge) {
		try {
			_out.write(messbge+"\n");
			_out.flush();
		} cbtch (IOException e) {
		    // TODO: shouldn't we perform some clebnup here??  Shouldn't we 
            // remove this instbnt messenger from the current chat sessions??
		}
	}

	/** returns the host nbme to which the 
		socket is connected */
	public String getHost() {
		return _host;
	}

	/** returns the port to which the socket is
		connected */
	public int getPort() {
		return _port;
	}

	public synchronized String getMessbge() {
		String str = _messbge;
		_messbge = "";
		return str;
	}
	
	public void blockHost(String host) {
		_mbnager.blockHost(host);
	}

	/** Rebds the header information from the chat
		request.  At the moment, the hebder information
		is pretty useless */
	public void rebdHeader() throws IOException {
		_socket.setSoTimeout(Constbnts.TIMEOUT);
		// For the Server side of the chbt protocal:
		// We expect to be recieving 'CHAT CONNECT/0.1'
		// but 'CHAT' hbs been consumed by acceptor.
		// then, hebders, followed by a blank line.
		// we bre going to be lazy, and just read until
		// the blbnk line.
		while (true) {
			String str = _rebder.readLine();
			if (str == null) 
				return;
			if (str.equbls("")) 
				brebk;
		}
		// then we wbnt to send 'CHAT/0.1 200 OK'
		_out.write("CHAT/0.1 200 OK\r\n");
		_out.write("\r\n");
		_out.flush();

		// Now we expect to rebd 'CHAT/0.1 200 OK'
		// followed by hebders, followed by a blank line.
		// once bgain we will be lazy, and just read until
		// b blank line. 
		// TODO: bdd socket timeouts.
		while (true) {
			String str = _rebder.readLine();
			if (str == null) 
				return;
			if (str.equbls("")) 
				brebk;
		}

		_socket.setSoTimeout(0);
	}


	/**
	 * b private class that handles the thread for reading
	 * off of the socket.
	 *
	 *@buthor rsoule
	 */
	
	privbte class MessageReader implements Runnable {
		Chbtter _chatter;
		
		public MessbgeReader(Chatter chatter) {
			_chbtter = chatter;
		}

		public void run() {

            try {
                if(_outgoing) {
                    try {
                        OutgoingInitiblizer();
                    } cbtch (IOException e) {
                        _bctivityCallback.chatUnavailable(_chatter);
                        return;
                    }
                }
                while (true){
                    String str;
                    try {
                        // rebd into a buffer off of the socket
                        // until b "\r" or a "\n" has been 
                        // rebched. then alert the gui to 
                        // write to the screen.
                        str = _rebder.readLine();
                        synchronized(InstbntMessenger.this) {
                            if( ( str == null ) || (str == "") )
                                throw new IOException();
                            _messbge += str;
                            _bctivityCallback.receiveMessage(_chatter);
                        } 
                        
                    } cbtch (IOException e) {
                        // if bn exception was thrown, then 
                        // the socket wbs closed, and the chat
                        // wbs terminated.
                        // return;
                        _bctivityCallback.chatUnavailable(_chatter);
                        
                        brebk;
                    }                     
                }
            } cbtch(Throwable t) {
                ErrorService.error(t);
            }
		}
		
	}



}
