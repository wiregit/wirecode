package org.limewire.store.server;

/*
 * $HeadURL$
 * $Revision: 1.1.4.2 $
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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

/**
 * Requires a port and dispatcher for the requests.
 */
abstract class AbstractHttpCoreServer {

    /**
     * Returns the handler for the requests.
     * 
     * @return the handler for the requests
     */
    protected abstract HttpRequestHandler createDispatcher();
    
    /**
     * Returns he port to use.
     * 
     * @return port to use
     */
    protected abstract int getPort();

    public final void start() {
        try {
            Thread t = new Thread(new RequestListenerThread(getPort()));
            t.setDaemon(false);
            t.start();
        } catch (IOException e) {
            System.err.println(getClass().getName() + " on port " + getPort());
            e.printStackTrace();
        }

    }

     private class RequestListenerThread implements Runnable {

        private final ServerSocket serversocket;
        private final HttpParams params;

        public RequestListenerThread(int port) throws IOException {
            this.serversocket = new ServerSocket(port);
            this.params = new BasicHttpParams(null);
            this.params
                    .setIntParameter(HttpConnectionParams.SO_TIMEOUT, 5000)
                    .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                            8 * 1024)
                    .setBooleanParameter(
                            HttpConnectionParams.STALE_CONNECTION_CHECK, false)
                    .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
                    .setParameter(HttpProtocolParams.ORIGIN_SERVER,
                            "Jakarta-HttpComponents/1.1");
        }
        
        public final void run() {
            System.out.println("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    System.out.println("Incoming connection from "
                            + socket.getInetAddress());
                    conn.bind(socket, this.params);

                    // Set up the HTTP protocol processor
                    BasicHttpProcessor httpproc = new BasicHttpProcessor();
                    httpproc.addInterceptor(new ResponseDate());
                    httpproc.addInterceptor(new ResponseServer());
                    httpproc.addInterceptor(new ResponseContent());
                    httpproc.addInterceptor(new ResponseConnControl());

                    // Set up request handlers
                    HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
                    reqistry.register("*", createDispatcher());

                    // Set up the HTTP service
                    HttpService httpService = new HttpService(httpproc,
                            new DefaultConnectionReuseStrategy(),
                            new DefaultHttpResponseFactory());
                    httpService.setParams(this.params);
                    httpService.setHandlerResolver(reqistry);

                    // Start worker thread
                    Thread t = new Thread(new WorkerThread(httpService, conn));
                    t.setDaemon(true);
                    t.start();
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    System.err
                            .println("I/O error initialising connection thread: "
                                    + e.getMessage());
                    break;
                }
            }
        }

    }

    private class WorkerThread implements Runnable {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(HttpService httpservice, HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        public void run() {
            System.out.println("New connection thread");
            HttpContext context = new HttpExecutionContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                System.err.println("Client closed connection");
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                System.err.println("Unrecoverable HTTP protocol violation: "
                        + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {
                }
            }
        }

    }

}
