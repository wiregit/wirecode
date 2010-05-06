package org.limewire.lws.server;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.limewire.net.SocketsManager;

/**
 * A simple remote {@link RemoteServer}.
 */
public class RemoteServerImpl extends AbstractServer implements RemoteServer {

    /** The port on which we'll connect this server. */
    public final static int PORT = 8080;
    
    private final PrivateKey privateKey;
    
//    private final LocalServerDelegate del;

    public RemoteServerImpl(SocketsManager socketsManager, int otherPort, KeyPair keypair) {
        super(PORT, "Remote Server", keypair);
        privateKey = keypair.getPrivate();
        setDispatcher(new DispatcherImpl());
//        this.del = new LocalServerDelegate(socketsManager, "localhost", otherPort);
    }
    
    /**
     * We DO want the IP of the incoming request to go to our handlers.
     */
    @Override
    protected final boolean sendIPToHandlers() {
        return true;
    }
    
    /**
     * Returns the equivalent of
     * 
     * <pre>
     * {&quot;public&quot; -&gt; &quot;PCURJKKTXE&quot;, &quot;private&quot; -&gt; &quot;BMBTVRVCSX&quot; }
     * </pre>
     * 
     * for request
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     * 
     * @param request request of the form
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     * 
     * @return the equivalent of
     * 
     * <pre>
     * {&quot;public&quot; -&gt; &quot;PCURJKKTXE&quot;, &quot;private&quot; -&gt; &quot;BMBTVRVCSX&quot; }
     * </pre>
     * 
     * for request
     * 
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     */
    protected static Map<String, String> getArgs(String request) {
        Map<String,String> args = new HashMap<String,String>();
        String target = "ClientCom";
        int itarget = request.indexOf(target);
        if (itarget == -1) return args;
        String rest = request.substring(itarget + target.length());
        if (rest.equals("")) return args;
        if (!Character.isLetterOrDigit(rest.charAt(0))) rest = rest.substring(1);
        if (rest.equals("")) return args;
        for (StringTokenizer st = new StringTokenizer(rest,"/\\" + File.separatorChar, false);
             st.hasMoreTokens();) {
            String name = st.nextToken();
            if (!st.hasMoreTokens()) {
                args.put(name, null);
                break;
            }
            String value = st.nextToken();
            args.put(name,value);
        }
        return args;
    }

    /**
     * Returns <code>StoreKey</code> for the following String:
     * <pre>
     * store\app\pages\client\ClientCom\command\StoreKey\public\PCURJKKTXE\private\BMBTVRVCSX/
     * </pre>
     * 
     * @param request
     * @return
     */
    public static String getCommand(String request) {
        String target = "command";
        int itarget = request.indexOf(target);
        if (itarget == -1) return null;
        String rest = request.substring(itarget + target.length());
        if (rest.equals("")) return null;
        if (!Character.isLetterOrDigit(rest.charAt(0))) rest = rest.substring(1);
        if (rest.equals("")) return null;
        StringBuffer res = new StringBuffer();
        for (int i=0; i<rest.length(); i++) {
            char c = rest.charAt(i);
            if (!Character.isLetterOrDigit(c)) break;
            res.append(c);
        }
        return res.toString();
    }
    
    private final class DispatcherImpl extends LWSDispatcherSupport {
        
        @Override
        protected final Handler[] createHandlers() {
            return new Handler[] { new Download() };

        }
        
        @Override
        protected Map<String, String> getArgs(String request) {
            return RemoteServerImpl.getArgs(request);
        }

        @Override
        protected String getCommand(String request) {
            return RemoteServerImpl.getCommand(request);
        }
        
        // ------------------------------------------------------------
        // Handlers
        // ------------------------------------------------------------
        
        /**
         * Sent from local code to retrieve the download-url
         * {@link Parameters#URL}.
         * 
         */
        class Download extends HandlerWithCallback {
            
            @Override
            public void handleRest(final Map<String, String> args, StringCallback cb) {
                String downloadURL = generateDownloadURL(args.get("hash"), args.get("browserIP"));
                cb.process(downloadURL);
            }
        }

    }    

    // ---------------------------------------------------------------
    // RemoteServer
    // ---------------------------------------------------------------
    
    public String generateDownloadURL(String hash, String ip){
        StringBuilder sb = new StringBuilder("/store/downloads");
        
        if(hash != null && hash.length() > 0){
            sb.append("hash=").append(hash).append("&signedHash=");
        }
        
        if(ip != null && ip.length() > 0){
            sb.append("browserIP=").append(ip).append("&signedBrowserIP=");
        }
        
        return sb.toString();
    }
    
    public PrivateKey getPrivateKey(){
        return this.privateKey;
    }
    
}
