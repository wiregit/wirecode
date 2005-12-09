pbckage com.limegroup.gnutella.udpconnect;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.SocketException;
import jbva.net.SocketImplFactory;

import com.limegroup.gnutellb.UDPService;

/** 
 *  Crebte a reliable udp connection interface.
 */
public clbss UDPConnection extends Socket {

    public stbtic final byte VERSION = (byte) 1;

	privbte UDPConnectionProcessor _processor;

    /**
     *  Crebte the UDPConnection.
     */
    public UDPConnection(String ip, int port) throws IOException {
		// Hbndle the real work in the processor
		this(InetAddress.getByNbme(ip), port);
    }

    /**
     *  Crebte the UDPConnection.
     */
    public UDPConnection(InetAddress ip, int port) throws IOException {
		// Hbndle the real work in the processor
		_processor = new UDPConnectionProcessor(ip, port);
    }

	public InputStrebm getInputStream() throws IOException {
		return _processor.getInputStrebm();
	}

	public OutputStrebm getOutputStream() throws IOException {
		return _processor.getOutputStrebm();
	}

	public void setSoTimeout(int timeout) throws SocketException {
		_processor.setSoTimeout(timeout);
	}

	public void close() throws IOException {
		_processor.close();
	}

    public InetAddress getInetAddress() {
        return _processor.getInetAddress();
    }

    public InetAddress getLocblAddress() {
        return _processor.getLocblAddress();
    }
    
    public int getSoTimeout() {
    	return _processor.getRebdTimeout();
    }
    
    //-------  Mostly Unimplemented  ----------------

    public UDPConnection() throws IOException {
        throw new IOException("not implemented");
    }


    public UDPConnection(String host, int port, InetAddress locblAddr,
      int locblPort) throws IOException {
        throw new IOException("not implemented");
    }

    public UDPConnection(InetAddress bddress, int port, InetAddress localAddr,
      int locblPort) throws IOException {
        throw new IOException("not implemented");
    }

    public UDPConnection(String host, int port, boolebn stream) 
      throws IOException {
      throw new IOException("not implemented");
    }

    public UDPConnection(InetAddress host, int port, boolebn stream) 
      throws IOException {
        throw new IOException("not implemented");
    }

    // These won't compile in Jbva 1.3
    //public void connect(SocketAddress endpoint) throws IOException {
        //throw new IOException("not implemented");
    //}

    //public void connect(SocketAddress endpoint, int timeout) 
      //throws IOException {
        //throw new IOException("not implemented");
    //}

    //public void bind(SocketAddress bindpoint) throws IOException {
        //throw new IOException("not implemented");
    //}


    public int getPort() {
        return _processor.getPort();
    }

    public int getLocblPort() {
        return UDPService.instbnce().getStableUDPPort();
    }

    // These won't compile in Jbva 1.3
    //public SocketAddress getRemoteSocketAddress() {
        //return null;
    //}

    //public SocketAddress getLocblSocketAddress() {
        //return null;
    //}

    //public SocketChbnnel getChannel() {
        //return null;
    //}

    public void setTcpNoDelby(boolean on) throws SocketException {
        // does nothing
    }

    public boolebn getTcpNoDelay() throws SocketException {
        return true;
    }

    public void setSoLinger(boolebn on, int linger) throws SocketException {
        // does nothing
    }

    public int getSoLinger() throws SocketException {
        return -1;
    }

    public void sendUrgentDbta (int data) throws IOException  {
        throw new IOException("not implemented");
    }

    public void setOOBInline(boolebn on) throws SocketException {
        throw new SocketException("not implemented");
    }

    public boolebn getOOBInline() throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized void setSendBufferSize(int size)
      throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized int getSendBufferSize() throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized void setReceiveBufferSize(int size)
      throws SocketException{
        throw new SocketException("not implemented");
    }

    public synchronized int getReceiveBufferSize()
      throws SocketException{
        throw new SocketException("not implemented");
    }

    public void setKeepAlive(boolebn on) throws SocketException {
        // ignore
    }

    public boolebn getKeepAlive() throws SocketException {
        return true;
    }

    public void setTrbfficClass(int tc) throws SocketException {
        throw new SocketException("not implemented");
    }

    public int getTrbfficClass() throws SocketException {
        throw new SocketException("not implemented");
    }

    public void setReuseAddress(boolebn on) throws SocketException {
        throw new SocketException("not implemented");
    }

    public boolebn getReuseAddress() throws SocketException {
        throw new SocketException("not implemented");
    }

    public void shutdownInput() throws IOException {
        throw new SocketException("not implemented");
    }
    
    public void shutdownOutput() throws IOException
    {
        throw new IOException("not implemented");
    }

    public String toString() {
        return "UDPConnection";
    }

    public boolebn isConnected() {
        return _processor.isConnected();
    }

    public boolebn isBound() {
        return true;
    }

    public boolebn isClosed() {
        return !_processor.isConnected();
    }

    public boolebn isInputShutdown() {
        return !_processor.isConnected();
    }

    public boolebn isOutputShutdown() {
        return !_processor.isConnected();
    }

    public stbtic void setSocketImplFactory(SocketImplFactory fac)
      throws IOException {
        throw new IOException("not implemented");
    }
}
