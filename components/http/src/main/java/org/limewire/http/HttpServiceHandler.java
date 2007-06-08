/*
 * $HeadURL: http://svn.apache.org/repos/asf/jakarta/httpcomponents/httpcore/trunk/module-nio/src/main/java/org/apache/http/nio/protocol/BufferingHttpServiceHandler.java $
 * $Revision: 1.4 $
 * $Date: 2007-06-08 00:28:21 $
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
import java.io.OutputStream;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolException;
import org.apache.http.UnsupportedHttpVersionException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.entity.ContentOutputStream;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.ContentInputBuffer;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.apache.http.nio.util.SimpleOutputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsLinker;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerResolver;
import org.apache.http.util.EncodingUtils;

/**
 * A HTTP service handler implementation that processes requests and responses
 * consecutively. Pipelining is not supported.
 * <p>
 * This class supports {@link HttpNIOEntity} objects and provides event
 * notifications when requests are received and responses have been sent.
 * <p>
 * The processing order for incoming requests is the following:
 * <pre>
 *    {@link HttpRequestInterceptor#process(HttpRequest, HttpContext)}
 *    {@link HttpServiceEventListener#requestReceived(NHttpConnection)}
 *    {@link HttpRequestHandlerResolver#lookup(String)}
 * </pre> 
 * <p>
 * Based on {@link BufferingHttpServiceHandler} rev. 544120.
 * 
 * @author <a href="mailto:oleg at ural.ru">Oleg Kalnichevski</a>
 */
public class HttpServiceHandler implements NHttpServiceHandler {

    private static final String CONN_STATE = "http.nio.conn-state";
    
    private final HttpProcessor httpProcessor;
    private final HttpResponseFactory responseFactory;
    private final ConnectionReuseStrategy connStrategy;
    private final ByteBufferAllocator allocator;
    private final HttpParams params;

    private HttpRequestHandlerResolver handlerResolver;
    private HttpExpectationVerifier expectationVerifier;
    private HttpServiceEventListener eventListener;
    
    public HttpServiceHandler(
            final HttpProcessor httpProcessor, 
            final HttpResponseFactory responseFactory,
            final ConnectionReuseStrategy connStrategy,
            final HttpParams params) {
        super();
        if (httpProcessor == null) {
            throw new IllegalArgumentException("HTTP processor may not be null.");
        }
        if (connStrategy == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null");
        }
        if (responseFactory == null) {
            throw new IllegalArgumentException("Response factory may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.httpProcessor = httpProcessor;
        this.connStrategy = connStrategy;
        this.responseFactory = responseFactory;
        this.params = params;
        this.allocator = new HeapByteBufferAllocator();
    }

    public void setEventListener(final HttpServiceEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setHandlerResolver(final HttpRequestHandlerResolver handlerResolver) {
        this.handlerResolver = handlerResolver;
    }

    public void setExpectationVerifier(final HttpExpectationVerifier expectationVerifier) {
        this.expectationVerifier = expectationVerifier;
    }

    public HttpParams getParams() {
        return this.params;
    }
    
    public void connected(final NHttpServerConnection conn) {
        HttpContext context = conn.getContext();

        ServerConnState connState = new ServerConnState(allocator); 
        context.setAttribute(CONN_STATE, connState);

        if (this.eventListener != null) {
            this.eventListener.connectionOpen(conn);
        }
    }

    public void requestReceived(final NHttpServerConnection conn) {
        HttpContext context = conn.getContext();
        
        HttpRequest request = conn.getHttpRequest();
        HttpParamsLinker.link(request, this.params);
        
        ServerConnState connState = (ServerConnState) context.getAttribute(CONN_STATE);

        // Update connection state
        connState.resetInput();
        connState.setRequest(request);
        connState.setInputState(ServerConnState.REQUEST_RECEIVED);
        
        HttpVersion ver = request.getRequestLine().getHttpVersion();
        if (!ver.lessEquals(HttpVersion.HTTP_1_1)) {
            // Downgrade protocol version if greater than HTTP/1.1 
            ver = HttpVersion.HTTP_1_1;
        }

        HttpResponse response;
        
        try {

            if (request instanceof HttpEntityEnclosingRequest) {
                if (((HttpEntityEnclosingRequest) request).expectContinue()) {
                    response = this.responseFactory.newHttpResponse(
                            ver, HttpStatus.SC_CONTINUE, context);
                    HttpParamsLinker.link(response, this.params);
                    
                    if (this.expectationVerifier != null) {
                        try {
                            this.expectationVerifier.verify(request, response, context);
                        } catch (HttpException ex) {
                            response = this.responseFactory.newHttpResponse(
                                    HttpVersion.HTTP_1_0, 
                                    HttpStatus.SC_INTERNAL_SERVER_ERROR, 
                                    context);
                            HttpParamsLinker.link(response, this.params);
                            handleException(ex, response);
                        }
                    }
                    
                    if (response.getStatusLine().getStatusCode() < 200) {
                        // Send 1xx response indicating the server expections
                        // have been met
                        conn.submitResponse(response);
                    } else {
                        // The request does not meet the server expections
                        conn.resetInput();
                        connState.resetInput();
                        sendResponse(conn, response);
                    }
                }
                // Request content is expected. 
                // Wait until the request content is fully received
            } else {
                // No request content is expected. 
                // Process request right away
                conn.suspendInput();
                processRequest(conn, request);
            }
            
        } catch (IOException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalIOException(ex, conn);
            }
        } catch (HttpException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalProtocolException(ex, conn);
            }
        }
        
    }

    public void closed(final NHttpServerConnection conn) {
        notifyResponseSent(conn);
        if (this.eventListener != null) {
            this.eventListener.connectionClosed(conn);
        }
    }

    public void exception(final NHttpServerConnection conn, final HttpException httpex) {
        HttpContext context = conn.getContext();
        try {
            HttpResponse response = this.responseFactory.newHttpResponse(
                    HttpVersion.HTTP_1_0, HttpStatus.SC_INTERNAL_SERVER_ERROR, context);
            HttpParamsLinker.link(response, this.params);
            handleException(httpex, response);
            response.setEntity(null);
            sendResponse(conn, response);
            
        } catch (IOException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalIOException(ex, conn);
            }
        } catch (HttpException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalProtocolException(ex, conn);
            }
        }
    }

    public void exception(final NHttpServerConnection conn, final IOException ex) {
        shutdownConnection(conn);
        if (this.eventListener != null) {
            this.eventListener.fatalIOException(ex, conn);
        }
    }

    public void inputReady(final NHttpServerConnection conn, final ContentDecoder decoder) {
        HttpContext context = conn.getContext();
        HttpRequest request = conn.getHttpRequest();

        ServerConnState connState = (ServerConnState) context.getAttribute(CONN_STATE);
        ContentInputBuffer buffer = connState.getInbuffer();

        // Update connection state
        connState.setInputState(ServerConnState.REQUEST_BODY_STREAM);
        
        try {
            buffer.consumeContent(decoder);
            if (decoder.isCompleted()) {
                // Request entity has been fully received
                connState.setInputState(ServerConnState.REQUEST_BODY_DONE);

                // Create a wrapper entity instead of the original one
                HttpEntityEnclosingRequest entityReq = (HttpEntityEnclosingRequest) request;
                if (entityReq.getEntity() != null) {
                    entityReq.setEntity(new ContentBufferEntity(
                            entityReq.getEntity(), 
                            connState.getInbuffer()));
                }
                conn.suspendInput();
                processRequest(conn, request);
            }
            
        } catch (IOException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalIOException(ex, conn);
            }
        } catch (HttpException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalProtocolException(ex, conn);
            }
        }
    }

    public void responseReady(final NHttpServerConnection conn) {
        notifyResponseSent(conn);

        if (conn.isOpen()) {
            conn.requestInput();
            if (((DefaultNHttpServerConnection)conn).hasBufferedInput()) {
                ((DefaultNHttpServerConnection)conn).consumeInput(this);
            }
        }
    }

    public void outputReady(final NHttpServerConnection conn, final ContentEncoder encoder) {

        HttpContext context = conn.getContext();

        ServerConnState connState = (ServerConnState) context.getAttribute(CONN_STATE);
        ContentOutputBuffer buffer = connState.getOutbuffer();

        // Update connection state
        connState.setOutputState(ServerConnState.RESPONSE_BODY_STREAM);
        
        try {
            // LW
            HttpNIOEntity entity = connState.getEntity();
            if (entity != null) {
                entity.produceContent(encoder, conn);
            } else {
                buffer.produceContent(encoder);
            }

            if (encoder.isCompleted()) {
                connState.setOutputState(ServerConnState.RESPONSE_BODY_DONE);
                connState.resetOutput();
                
                if (!this.connStrategy.keepAlive(conn.getHttpResponse(), context)) {
                    conn.close();
                }
            }
        } catch (IOException ex) {
            shutdownConnection(conn);
            if (this.eventListener != null) {
                this.eventListener.fatalIOException(ex, conn);
            }
        }
    }

    public void timeout(final NHttpServerConnection conn) {
        shutdownConnection(conn);
        if (this.eventListener != null) {
            this.eventListener.connectionTimeout(conn);
        }
    }

    private void shutdownConnection(final NHttpConnection conn) {
        ServerConnState connState = (ServerConnState) conn.getContext().getAttribute(CONN_STATE);
        connState.resetOutput();

        try {
            conn.shutdown();
        } catch (IOException ignore) {
        }
    }
    
    private void handleException(final HttpException ex, final HttpResponse response) {
        int code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        if (ex instanceof MethodNotSupportedException) {
            code = HttpStatus.SC_NOT_IMPLEMENTED;
        } else if (ex instanceof UnsupportedHttpVersionException) {
            code = HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED;
        } else if (ex instanceof ProtocolException) {
            code = HttpStatus.SC_BAD_REQUEST;
        }
        response.setStatusCode(code);
        
        byte[] msg = EncodingUtils.getAsciiBytes(ex.getMessage());
        ByteArrayEntity entity = new ByteArrayEntity(msg);
        entity.setContentType("text/plain; charset=US-ASCII");
        response.setEntity(entity);
    }
    
    private void processRequest(
            final NHttpServerConnection conn,
            final HttpRequest request) throws IOException, HttpException {
        
        HttpContext context = conn.getContext();
        HttpVersion ver = request.getRequestLine().getHttpVersion();

        if (!ver.lessEquals(HttpVersion.HTTP_1_1)) {
            // Downgrade protocol version if greater than HTTP/1.1 
            ver = HttpVersion.HTTP_1_1;
        }

        HttpResponse response = this.responseFactory.newHttpResponse(
                ver, 
                HttpStatus.SC_OK, 
                conn.getContext());
        HttpParamsLinker.link(response, this.params);
        
        context.setAttribute(HttpExecutionContext.HTTP_REQUEST, request);
        context.setAttribute(HttpExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpExecutionContext.HTTP_RESPONSE, response);
        
        try {

            this.httpProcessor.process(request, context);

            notifyRequestReceived(conn);
            
            HttpRequestHandler handler = null;
            if (this.handlerResolver != null) {
                String requestURI = request.getRequestLine().getUri();
                handler = this.handlerResolver.lookup(requestURI);
            }
            if (handler != null) {
                handler.handle(request, response, context);
            } else {
                response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            }
        } catch (HttpException ex) {
            response = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_0, 
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, context);
            HttpParamsLinker.link(response, this.params);
            handleException(ex, response);
        }

        sendResponse(conn, response);
    }

    private void sendResponse(
            final NHttpServerConnection conn,
            final HttpResponse response) throws IOException, HttpException {

        HttpContext context = conn.getContext();

        ServerConnState connState = (ServerConnState) context.getAttribute(CONN_STATE);
        ContentOutputBuffer buffer = connState.getOutbuffer();

        this.httpProcessor.process(response, context);

        if (!canResponseHaveBody(connState.getRequest(), response)) {
            response.setEntity(null);
        }
        
        connState.setResponse(response);
        conn.submitResponse(response);

        // Update connection state
        connState.setOutputState(ServerConnState.RESPONSE_SENT);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            // LW
            if (entity instanceof HttpNIOEntity) {
                connState.setEntity((HttpNIOEntity) entity);
            } else {
                OutputStream outstream = new ContentOutputStream(buffer);
                entity.writeTo(outstream);
                outstream.flush();
                outstream.close();
            }
        } else {
            connState.resetOutput();            
            if (!this.connStrategy.keepAlive(response, context)) {
                conn.close();
            }
        }
    }

    private boolean canResponseHaveBody(
            final HttpRequest request, final HttpResponse response) {

        if (request != null && "HEAD".equalsIgnoreCase(request.getRequestLine().getMethod())) {
            return false;
        }
        
        int status = response.getStatusLine().getStatusCode(); 
        return status >= HttpStatus.SC_OK 
            && status != HttpStatus.SC_NO_CONTENT 
            && status != HttpStatus.SC_NOT_MODIFIED
            && status != HttpStatus.SC_RESET_CONTENT; 
    }

    static class ServerConnState {
        
        public static final int READY                      = 0;
        public static final int REQUEST_RECEIVED           = 1;
        public static final int REQUEST_BODY_STREAM        = 2;
        public static final int REQUEST_BODY_DONE          = 4;
        public static final int RESPONSE_SENT              = 8;
        public static final int RESPONSE_BODY_STREAM       = 16;
        public static final int RESPONSE_BODY_DONE         = 32;
        
        private SimpleInputBuffer inbuffer; 
        private ContentOutputBuffer outbuffer;

        private int inputState;
        private int outputState;
        
        private HttpRequest request;
        private final ByteBufferAllocator allocator;
        
        public ServerConnState(final ByteBufferAllocator allocator) {
            super();
            this.inputState = READY;
            this.outputState = READY;
            this.allocator = allocator;
        }

        public ContentInputBuffer getInbuffer() {
            if (this.inbuffer == null) {
                this.inbuffer = new SimpleInputBuffer(2048, allocator);
            }
            return this.inbuffer;
        }

        public ContentOutputBuffer getOutbuffer() {
            if (this.outbuffer == null) {
                this.outbuffer = new SimpleOutputBuffer(2048, allocator);
            }
            return this.outbuffer;
        }
        
        public int getInputState() {
            return this.inputState;
        }

        public void setInputState(int inputState) {
            this.inputState = inputState;
        }

        public int getOutputState() {
            return this.outputState;
        }

        public void setOutputState(int outputState) {
            this.outputState = outputState;
        }

        public HttpRequest getRequest() {
            return this.request;
        }

        public void setRequest(final HttpRequest request) {
            this.request = request;
        }

        public void resetInput() {
            this.inbuffer = null;
            this.request = null;
            this.inputState = READY;
        }
        
        public void resetOutput() {
            this.outbuffer = null;
            this.outputState = READY;
            // LW
            if (this.entity != null) {
                this.entity.finished();
                this.entity = null;
            }
        }

        // LW 
        
        private HttpResponse response;

        private volatile HttpNIOEntity entity;

        public HttpResponse getResponse() {
            return response;
        }
        
        public void setResponse(HttpResponse response) {
            this.response = response;
        }
        
        public HttpNIOEntity getEntity() {
            return entity;
        }

        public void setEntity(HttpNIOEntity entity) {
            this.entity = entity;
        }

    }

    // LW

    public void notifyRequestReceived(NHttpServerConnection conn) {
        if (eventListener != null) {
            eventListener.requestReceived(conn, conn.getHttpRequest());
        }
    }

    public void notifyResponseSent(NHttpServerConnection conn) {
        ServerConnState connState = (ServerConnState) conn.getContext().getAttribute(CONN_STATE);
        if (eventListener != null) {
            HttpResponse response = connState.getResponse();
            if (response != null) {
                eventListener.responseSent(conn, response);
            }
        }
        connState.setResponse(null);
    }
    
}
