/*
 * $HeadURL: http://svn.apache.org/repos/asf/jakarta/httpcomponents/httpcore/trunk/module-nio/src/test/java/org/apache/http/nio/mockup/TestHttpServer.java $
 * $Revision: 1.1.4.1 $
 * $Date: 2007-04-27 18:28:35 $
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

package org.limewire.http;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.HttpIOReactor;

public class HttpTestServer {

    private final HttpRequestHandlerRegistry registry;

    private HttpIOReactor reactor;

    private HttpParams params = new BasicHttpParams();

    public HttpTestServer() throws IOException {
        this.params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, 2000)
           .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 2000)
           .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)
           .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false)
           .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
           .setParameter(HttpProtocolParams.USER_AGENT, "TEST-SERVER/1.1");

        this.registry = new HttpRequestHandlerRegistry();
    }

    public void registerHandler(final String pattern,
            final HttpRequestHandler handler) {
        this.registry.register(pattern, handler);
    }

    public void execute(EventListener listener) throws IOException {
        BasicHttpProcessor processor = new BasicHttpProcessor();
        processor.addInterceptor(new ResponseDate());
        processor.addInterceptor(new ResponseServer());
        processor.addInterceptor(new ResponseContent());
        processor.addInterceptor(new ResponseConnControl());

        BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(
                processor, new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(), params);

        serviceHandler.setEventListener(listener);

        serviceHandler.setHandlerResolver(this.registry);

        reactor = new HttpIOReactor(params);
        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, reactor.getHttpParams());
        reactor.execute(ioEventDispatch);
    }

    public void acceptConnection(String word, Socket socket) {
        reactor.acceptConnection(word, socket);
    }
    
}
