/*
 * $HeadURL: http://svn.apache.org/repos/asf/jakarta/httpcomponents/httpcore/trunk/module-nio/src/test/java/org/apache/http/nio/mockup/TestHttpClient.java $
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

package com.limegroup.gnutella.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.nio.protocol.BufferingHttpClientHandler;
import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpSessionRequest;

public class HttpTestClient {

    private HttpIOReactor ioReactor;
    private DefaultClientIOEventDispatch ioEventDispatch;
    private HttpParams params = new BasicHttpParams();
    
    public HttpTestClient() throws IOException {
        this.params
            .setIntParameter(HttpConnectionParams.SO_TIMEOUT, 2000)
            .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 2000)
            .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
            .setParameter(HttpProtocolParams.USER_AGENT, "TEST-CLIENT/1.1");
    }
    
    public void execute(final HttpRequestExecutionHandler execHandler) throws IOException {
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        httpproc.addInterceptor(new RequestUserAgent());
        
        BufferingHttpClientHandler handler = new BufferingHttpClientHandler(
                httpproc,
                execHandler,
                new DefaultConnectionReuseStrategy(),
                params);
        
        //handler.setEventListener(new EventLogger());
        
        ioReactor = new HttpIOReactor(params);
        ioEventDispatch = new DefaultClientIOEventDispatch(handler, params);
        ioReactor.execute(ioEventDispatch);
    }
    
    public HttpSessionRequest connect(final InetSocketAddress address, final Object attachment) {
        HttpSessionRequest session = ioReactor.createSession(address, null, attachment);
        ioReactor.connect(session);
        return session;
    }
    
    public HttpParams getParams() {
        return params;
    }
    
}
