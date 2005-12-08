pbckage com.limegroup.gnutella.connection;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.io.Shutdownable;

/**
 * Bbsic interface allowing various asynchronous message senders.
 */
public interfbce OutputRunner extends Shutdownable {
    
    public void send(Messbge m);

}