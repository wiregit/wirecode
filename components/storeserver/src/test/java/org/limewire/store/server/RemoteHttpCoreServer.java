package org.limewire.store.server;

/*
 * $HeadURL$
 * $Revision: 1.1.4.1 $
 * $Date: 2007-06-08 15:04:31 $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;

public class RemoteHttpCoreServer extends AbstractHttpCoreServer {

    public static final int PORT = 5555;

    public static void main(String[] args) throws Exception {
        new RemoteHttpCoreServer().start();
    }

    protected int getPort() {
        return PORT;
    }
    
    private final RemoteServer remoteServerDelegate = new DefaultRemoteServer();
    
    protected HttpRequestHandler createDispatcher() {
        Dispatchee dispatchee = new AbstractDispatchee() {

            @Override
            protected void connectionChanged(boolean isConnected) {
            }

            public String dispatch(String cmd, Map<String, String> args) {
                if (cmd.equals(DispatcherSupport.Commands.STORE_KEY)) {
                    String publicKey = Util.getArg(args, DispatcherSupport.Parameters.PUBLIC);
                    if (publicKey == null) {
                        return DispatcherSupport.report(DispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER);
                    }
                    String privateKey = Util.getArg(args, DispatcherSupport.Parameters.PRIVATE);
                    if (privateKey == null) {
                        return DispatcherSupport.report(DispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER);
                    }
                    String ip = Util.getArg(args, DispatcherSupport.Parameters.IP);
                    if (ip == null) {
                        return DispatcherSupport.report(DispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER);
                    }                
                    if (Util.isEmpty(publicKey)) {
                        return DispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY;
                    }
                    remoteServerDelegate.storeKey(publicKey, privateKey, ip);
                    return DispatcherSupport.Responses.OK;                    
                } else if (cmd.equals(DispatcherSupport.Commands.GIVE_KEY)) {
                    String publicKey = Util.getArg(args, DispatcherSupport.Parameters.PUBLIC);
                    if (publicKey == null) {
                        return DispatcherSupport.report(DispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER);
                    }
                    String ip = Util.getArg(args, DispatcherSupport.Parameters.IP);
                    if (ip == null) {
                        return DispatcherSupport.report(DispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER);
                    }
                    return remoteServerDelegate.lookUpPrivateKey(publicKey, ip);                    
                    
                }
                return null;
            }
            
        };
        SendsMessagesToServer sender = new LocalServerDelegate("localhost", LocalHttpCoreServer.PORT);
        return Dispatcher.CREATOR.newInstance(sender, dispatchee);
    }

}
