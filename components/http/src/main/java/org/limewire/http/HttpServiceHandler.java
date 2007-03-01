/*
 * $HeadURL: http://svn.apache.org/repos/asf/jakarta/httpcomponents/httpcore/trunk/module-nio/src/main/java/org/apache/http/nio/protocol/BufferingHttpServiceHandler.java $
 * $Revision: 1.1.2.2 $
 * $Date: 2007-03-01 03:14:09 $
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

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;

/**
 * Based on BufferingHttpServiceHandler.
 */
public class HttpServiceHandler extends BufferingHttpServiceHandler {

    private ServerConnectionEventListener connectionListener;

    public HttpServiceHandler(HttpProcessor httpProcessor,
            HttpResponseFactory responseFactory,
            ConnectionReuseStrategy connStrategy, HttpParams params) {
        super(httpProcessor, responseFactory, connStrategy, params);
    }  
    
    public void setConnectionListener(
            ServerConnectionEventListener connectionListener) {
        this.connectionListener = connectionListener;
    }
    
    public ServerConnectionEventListener getConnectionListener() {
        return connectionListener;
    }
    
    @Override
    public void connected(NHttpServerConnection conn) {
        super.connected(conn);
        
        if (connectionListener != null) {
            connectionListener.connectionOpen(conn);
        }
    }
    
    @Override
    public void closed(NHttpServerConnection conn) {
        super.closed(conn);

        if (connectionListener != null) {
            connectionListener.connectionClosed(conn);
        }
    }

    @Override
    public void outputReady(NHttpServerConnection conn, ContentEncoder encoder) {
        super.outputReady(conn, encoder);
        
        if (encoder.isCompleted()) {
            connectionListener.responseContentSent(conn, conn.getHttpResponse());
        }
    }
    
    public void responseSent(DefaultNHttpServerConnection conn, HttpResponse response) {
        if (connectionListener != null) {
            connectionListener.responseSent(conn, response);
        }        
    }

}
