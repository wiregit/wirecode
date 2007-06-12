package com.limegroup.gnutella.browser;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.AsyncHttpRequestHandler;

import com.limegroup.gnutella.HTTPAcceptor;

public class LocalHTTPAcceptor extends HTTPAcceptor {

    private static final Log LOG = LogFactory.getLog(LocalHTTPAcceptor.class);

    /** Magnet request for a default action on parameters */
//    private static final String MAGNET_DEFAULT = "/magnet10/default.js?";

    /** Magnet request for a paused response */
//    private static final String MAGNET_PAUSE = "/magnet10/pause";

    /** Start of Magnet URI */
    private static final String MAGNET = "magnet:?";

    /** Magnet detail command */
    private static final String MAGNET_DETAIL = "magcmd/detail?";

    private String lastCommand;

    private long lastCommandTime;

    private long MIN_REQUEST_INTERVAL = 1500;

    public LocalHTTPAcceptor() {
        registerHandler("magnet:", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/default.js", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/pause", new MagnetPauseRequestHandler());
        registerHandler("/magcmd/detail", new MagnetDetailRequestHandler());
        registerHandler("*", new FileRequestHandler());
    }

    private class MagnetCommandRequestHandler implements AsyncHttpRequestHandler {
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            triggerMagnetHandling(request.getRequestLine().getUri());
        }
    }

    private class MagnetPauseRequestHandler implements AsyncHttpRequestHandler {
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }
            response.setStatusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    private class MagnetDetailRequestHandler implements AsyncHttpRequestHandler {
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            String uri = request.getRequestLine().getUri();
            int i = uri.indexOf(MAGNET_DETAIL);
            String command = uri.substring(i + MAGNET_DETAIL.length());
            String page = MagnetHTML.buildMagnetDetailPage(command);
            // TODO set charset
            StringEntity entity = new StringEntity(page);
            entity.setContentType("text/html");
            response.setEntity(entity);
        }
    }

    private class FileRequestHandler implements HttpRequestHandler {
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        }
    }

    private synchronized void triggerMagnetHandling(String uri)
            throws IOException {
        int i = uri.indexOf("?");
        if (i == -1) {
            throw new IOException("Invalid command");
        }
        String command = uri.substring(i + 1);

        // suppress duplicate requests from some browsers
        long currentTime = System.currentTimeMillis();
        if (!(command.equals(lastCommand) && (currentTime - lastCommandTime) < MIN_REQUEST_INTERVAL)) {
            // trigger an operation
            ExternalControl.handleMagnetRequest(MAGNET + command);
            lastCommand = command;
            lastCommandTime = currentTime;

        } else {
            LOG.warn("Ignoring duplicate request: " + command);
        }
    }
    
}
