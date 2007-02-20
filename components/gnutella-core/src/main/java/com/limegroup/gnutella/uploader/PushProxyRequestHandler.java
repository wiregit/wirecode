/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.io.NetworkUtils;
import org.limewire.util.Base32;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.statistics.UploadStat;

public class PushProxyRequestHandler implements HttpRequestHandler {

    public PushProxyRequestHandler() {
    }

    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        PushProxyRequest pushProxyRequest = getPushProxyRequest(request);
        UploadStat.PUSH_PROXY.incrementStat();
        if (pushProxyRequest == null) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            UploadStat.PUSH_PROXY_REQ_BAD.incrementStat();
        } else {
            if (!sendRequest(pushProxyRequest)) {
                response.setStatusCode(HttpStatus.SC_GONE);
                response.setReasonPhrase("Servent not connected");
                UploadStat.PUSH_PROXY_REQ_FAILED.incrementStat();
            } else {
                response.setStatusCode(HttpStatus.SC_ACCEPTED);
                response.setReasonPhrase("Message sent");
                UploadStat.PUSH_PROXY_REQ_SUCCESS.incrementStat();
            }

        }
    }

    private PushProxyRequest getPushProxyRequest(HttpRequest request) {
        String uri = request.getRequestLine().getUri();
        // start after the '?'
        int i = uri.indexOf('?');
        if (i == -1) {
            return null;
        }

        String queryString = uri.substring(i + 1);

        StringTokenizer t = new StringTokenizer(queryString, "=&");
        if (t.countTokens() < 2 || t.countTokens() % 2 != 0) {
            return null;
        }

        String clientGUID = null;
        int fileIndex = 0;

        while (t.hasMoreTokens()) {
            final String key = t.nextToken();
            final String val = t.nextToken();
            if (key.equalsIgnoreCase(PushProxyUploadState.P_SERVER_ID)) {
                if (clientGUID != null) // already have a name?
                    return null;
                // must convert from base32 to base 16.
                byte[] base16 = Base32.decode(val);
                if (base16.length != 16)
                    return null;
                clientGUID = new GUID(base16).toHexString();
            } else if (key.equalsIgnoreCase(PushProxyUploadState.P_GUID)) {
                if (clientGUID != null || val.length() != 32)
                    return null;
                // already in base16
                clientGUID = val;
            } else if (key.equalsIgnoreCase(PushProxyUploadState.P_FILE)) {
                if (fileIndex != 0)
                    return null;
                try {
                    fileIndex = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (fileIndex < 0)
                    return null;

            }
        }
        
        if (clientGUID == null) {
            return null;
        }

        Header header = request.getLastHeader(HTTPHeaderName.NODE
                .httpStringValue());
        InetSocketAddress address = getNodeAddress(header.getValue());
        if (address == null) {
            return null;
        }

        return new PushProxyRequest(clientGUID, fileIndex, address);
    }

    private InetSocketAddress getNodeAddress(String value) {
        StringTokenizer t = new StringTokenizer(value, ":");
        if (t.countTokens() == 2) {
            try {
                InetAddress address = InetAddress.getByName(t.nextToken()
                        .trim());
                int port = Integer.parseInt(t.nextToken().trim());
                if (NetworkUtils.isValidAddress(address)
                        && NetworkUtils.isValidPort(port)) {
                    return new InetSocketAddress(address, port);
                }
            } catch (UnknownHostException badHost) {
            } catch (NumberFormatException nfe) {
            }
        }
        return null;
    }

    private boolean sendRequest(PushProxyRequest request) {
        byte[] clientGUID = GUID.fromHexString(request.getClientGUID());
        PushRequest push = new PushRequest(GUID.makeGuid(), (byte) 0,
                clientGUID, request.getFileIndex(), request.getAddress()
                        .getAddress().getAddress(), request.getAddress()
                        .getPort());
        try {
            RouterService.getMessageRouter().sendPushRequest(push);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private class PushProxyRequest {

        private String clientGUID;

        private int fileIndex;

        private InetSocketAddress address;

        public PushProxyRequest(String clientGUID, int fileIndex,
                InetSocketAddress address) {
            this.clientGUID = clientGUID;
            this.fileIndex = fileIndex;
            this.address = address;
        }

        public String getClientGUID() {
            return clientGUID;
        }

        public int getFileIndex() {
            return fileIndex;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

    }

}