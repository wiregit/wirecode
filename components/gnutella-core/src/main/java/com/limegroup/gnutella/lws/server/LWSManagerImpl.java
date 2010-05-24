package com.limegroup.gnutella.lws.server;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.limewire.inject.EagerSingleton;
import org.limewire.lws.server.AbstractReceivesCommandsFromDispatcher;
import org.limewire.lws.server.LWSCommandValidator;
import org.limewire.lws.server.LWSConnectionListener;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherFactory;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSReceivesCommandsFromDispatcher;

import com.google.inject.Inject;


/**
 * Encapsulates a {@link LWSDispatcher} and {@link LWSReceivesCommandsFromDispatcher}.
 */
@EagerSingleton
public final class LWSManagerImpl implements LWSManager {
        
    private final LWSDispatcher dispatcher;
    private final Map<String, LWSManagerCommandResponseHandler> commands2handlers = new HashMap<String, LWSManagerCommandResponseHandler>();
    
    
    private boolean isConnected;
    
    @Inject
    public LWSManagerImpl(LWSDispatcherFactory lwsDispatcherFactory, LWSCommandValidator verifier) {      
        this.dispatcher = lwsDispatcherFactory.createDispatcher(new AbstractReceivesCommandsFromDispatcher() {
            public String receiveCommand(String cmd, Map<String, String> args) {
                return LWSManagerImpl.this.dispatch(cmd, args);
            }
        },
        verifier);

        //
        // remember when we're connected
        //
        dispatcher.addConnectionListener(new LWSConnectionListener() {

            public void connectionChanged(boolean isConnected) {
                LWSManagerImpl.this.isConnected = isConnected;
            }
            
        });
        //
        // Add handler to check if we are still connected
        // 
        registerHandler("IsConnected", new LWSManagerCommandResponseHandlerWithCallback("IsConnected") {
            @Override
            protected String handleRest(Map<String, String> args) {
                //
                // If we're connected with the correct private key this will be
                // fine
                //
                return LWSDispatcherSupport.Responses.OK;
            } 
        });
    }
 
    public final boolean addConnectionListener(LWSConnectionListener lis) {
        return dispatcher.addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(LWSConnectionListener lis) {
        return dispatcher.removeConnectionListener(lis);
    }
    
    
    // -----------------------------------------------------------------
    // Implementation of LWSManager
    // -----------------------------------------------------------------

    public final NHttpRequestHandler getHandler() {
        return dispatcher;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
  
    public final boolean registerHandler(String cmd, LWSManagerCommandResponseHandler lis) {
        if (lis == null) throw new NullPointerException("Handlers can't be null");
        String hash = hash(cmd);
        commands2handlers.put(hash, lis);
        return true;
    }
    
    public final boolean unregisterHandler(String cmd) {
        String hash = hash(cmd);
        return commands2handlers.remove(hash) != null;
    }
 
    public final void clearHandlers() {
        commands2handlers.clear();
    }
     
    // ---------------------------------------------------------------------------------
    // Private
    // ---------------------------------------------------------------------------------    
    private String hash(String cmd) {
        return cmd.toLowerCase(Locale.US);
    }    

    private String dispatch(String command, Map<String, String> arguments) {
        String hash = hash(command);
        String result = null;

        LWSManagerCommandResponseHandler handler = commands2handlers.get(hash);
        if (handler != null) {
            result = handler.handle(arguments);

            if (result == null) {
				// does this ever happen? could just assert.
                result = "OK";
            }            
        }
        
        return result;
    } 
}
