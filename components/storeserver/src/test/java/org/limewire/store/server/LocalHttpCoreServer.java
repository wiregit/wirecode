package org.limewire.store.server;

/*
 * $HeadURL$
 * $Revision: 1.1.4.4 $
 * $Date: 2007-06-20 20:39:36 $
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

import org.limewire.http.AsyncHttpRequestHandler;

public class LocalHttpCoreServer extends AbstractHttpCoreServer {

    public static final int PORT = 45100;

    public static void main(String[] args) throws Exception {
        new LocalHttpCoreServer().start();
    }

    protected int getPort() {
        return PORT;
    }
    
    protected AsyncHttpRequestHandler createDispatcher() {
        SendsMessagesToServer sender = new LocalServerDelegate("localhost", RemoteHttpCoreServer.PORT);
        return new StoreServerDispatcher(sender);
    }

}
