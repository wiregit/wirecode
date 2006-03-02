package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.StringSetting;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Contains some convenience methods for handshaking.
 */
class BlockingHandshakeSupport {
    
    /** Connection string. */
    private String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";
    
    private static final String GNUTELLA_06 = "GNUTELLA/0.6";    
    
    /** Gnutella 0.6 accept connection string. */
    private static final String CONNECT="CONNECT/";
    
    /** End of line for Gnutella 0.6 */
    private static final String CRLF="\r\n";    

    /** Socket we're basing this I/O on. */
    private Socket socket;
    /** InputStream we're reading from */
    private InputStream in;
    /** OutputStream we're writing to. */
    private OutputStream out;
    /** All headers we've read from the remote side. */
    private final Properties readHeaders;
    /** All headers we wrote to the remote side. */
    private final Properties writtenHeaders;
    
    /** Constructs a new Support object based on the given Socket, InputStream & OutputStream. */
    BlockingHandshakeSupport(Socket socket, InputStream in, OutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        readHeaders = new Properties();
        writtenHeaders = new Properties();
    }
    
    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within TIMEOUT milliseconds (as defined by the
     * property manager), throws IOException.  This includes EOF.
     * @return The line of characters read
     * @exception IOException if the characters cannot be read within 
     *            the specified timeout
     */
    String readLine() throws IOException {
        return readLine(Constants.TIMEOUT);
    }

    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within the specified timeout milliseconds,
     * throws IOException.  This includes EOF.
     * @param timeout The time to wait on the socket to read data before 
     *                  IOException is thrown
     * @return The line of characters read
     * @exception IOException if the characters cannot be read within 
     *          the specified timeout
     */
    String readLine(int timeout) throws IOException {
        int oldTimeout=socket.getSoTimeout();
        // _in.read can throw an NPE if we closed the connection,
        // so we must catch NPE and throw the CONNECTION_CLOSED.
        try {
            socket.setSoTimeout(timeout);
            String line=(new ByteReader(in)).readLine();
            if (line==null)
                throw new IOException("read null line");
            BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(line.length());
            return line;
        } catch(NullPointerException npe) {
            throw new IOException(); // TODO: use cached Exception
        } finally {
            //Restore socket timeout.
            socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Reads all headers.
     */
    void readHeaders() throws IOException {
        readHeaders(Constants.TIMEOUT);
    }
    
    /**
     * Reads the properties from the network and stores them locally.
     * 
     * @param timeout The time to wait on the socket to read data before 
     *                IOException is thrown
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    void readHeaders(int timeout) throws IOException {
        // TODO: limit number of headers read
        while (true) {
            // This doesn't distinguish between \r and \n. That's fine.
            String line = readLine(timeout);
            if (line == null)
                throw new IOException("unexpected end of file"); // unexpected EOF
            if (line.equals(""))
                break; // blank line ==> done
            int i = line.indexOf(':');
            if (i < 0)
                continue; // ignore lines without ':'
            String key = line.substring(0, i);
            String value = line.substring(i + 1).trim();
            if (HeaderNames.REMOTE_IP.equals(key))
                changeAddress(value);
            readHeaders.put(key, value);
        }
    }
    
    /** Creates their response, based on the given connectLine. */
    HandshakeResponse createRemoteResponse(String connectLine) throws IOException {
        return HandshakeResponse.createRemoteResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(), 
                    readHeaders);
    }
    
    /** Determines if the given connect line is valid. */
    boolean isConnectLineValid(String s) {
        return s.startsWith(GNUTELLA_06);
    }
    
    /** Writes the initial connection line. */
    void writeConnectLine() throws IOException {
        //1. Send "GNUTELLA CONNECT/0.6" and headers
        writeLine(GNUTELLA_CONNECT_06+CRLF);
    }
    
    /** Writes a response using the given HandshakeResponse. */
    void writeResponse(HandshakeResponse response) throws IOException {        
        writeLine(GNUTELLA_06 + " " + response.getStatusLine() + CRLF);
        sendHeaders(response.props());
    }

    /**
     * Writes s to out, with no trailing linefeeds. Called only from initialize().
     */
    void writeLine(String s) throws IOException {
        if (s == null || s.equals("")) {
            throw new NullPointerException("null or empty string: " + s);
        }

        // TODO: character encodings?
        byte[] bytes = s.getBytes();
        BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(bytes.length);
        out.write(bytes);
        out.flush();
    }
    
    /**
     * Returns true iff line ends with "CONNECT/N", where N is a number greater than or equal "0.6".
     */
    boolean notLessThan06(String line) {
        int i = line.indexOf(CONNECT);
        if (i < 0)
            return false;
        try {
            float f = Float.parseFloat(line.substring(i + CONNECT.length()));
            return f >= 0.6f;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Writes the properties in props to network, including the blank line at the end.
     * Throws IOException if there are any problems.
     * 
     * @param props The headers to be sent. Note: null argument is acceptable,
     *  if no headers need to be sent the trailer will be sent.
     */
    void sendHeaders(Properties props) throws IOException {
        if (props != null) {
            Enumeration names = props.propertyNames();
            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();
                String value = props.getProperty(key);
                // Ensure we put their remote-ip correctly.
                if (HeaderNames.REMOTE_IP.equals(key))
                    value = socket.getInetAddress().getHostAddress();
                if (value == null)
                    value = "";
                writeLine(key + ": " + value + CRLF);
                writtenHeaders.put(key, value);
            }
        }
        // send the trailer
        writeLine(CRLF);
    }
    
    /**
     * Determines if the address should be changed and changes it if
     * necessary.
     */
    void changeAddress(final String v) {
        InetAddress ia = null;
        try {
            ia = InetAddress.getByName(v);
        } catch(UnknownHostException uhe) {
            return; // invalid.
        }
        
        // invalid or private, exit
        if(!NetworkUtils.isValidAddress(ia) ||
            NetworkUtils.isPrivateAddress(ia))
            return;
            
        // If we're forcing, change that if necessary.
        if( ConnectionSettings.FORCE_IP_ADDRESS.getValue() ) {
            StringSetting addr = ConnectionSettings.FORCED_IP_ADDRESS_STRING;
            if(!v.equals(addr.getValue())) {
                addr.setValue(v);
                RouterService.addressChanged();
            }
        }
        // Otherwise, if our current address is invalid, change.
        else if(!NetworkUtils.isValidAddress(RouterService.getAddress())) {
            // will auto-call addressChanged.
            RouterService.getAcceptor().setAddress(ia);
        }
        
        RouterService.getAcceptor().setExternalAddress(ia);
    }
    
    /** Constructs a HandshakeResponse object wrapping the headers we've read. */
    public HandshakeResponse getReadHandshakeResponse() {
        return HandshakeResponse.createResponse(readHeaders);
    }
    
    /** Constructs a HandshakeResponse object wrapping the headers we've written. */
    public HandshakeResponse getWrittenHandshakeResponse() {
        return HandshakeResponse.createResponse(writtenHeaders);
    }
    
}
