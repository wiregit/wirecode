package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;

public class MockUtils {

    public static ConnectionManager createConnectionManagerWithPushProxies(Mockery context) throws UnknownHostException {
        Set<Connectable> proxies = new TreeSet<Connectable>(IpPort.COMPARATOR);
        proxies.add(new ConnectableImpl("192.168.0.1", 5555, false));
        proxies.add(new ConnectableImpl("192.168.0.2", 6666, true));
        return createConnectionManagerWithPushProxies(context, proxies);
    }
    
    public static ConnectionManager createConnectionManagerWithPushProxies(Mockery context, final Set<Connectable> pushProxies) {
        ConnectionManager connectionManager = context.mock(ConnectionManager.class);
        return mockWithPushProxies(context, connectionManager, pushProxies);
    }
    
    public static ConnectionManager mockWithPushProxies(Mockery context, final ConnectionManager connectionManager, final Set<Connectable> pushProxies) { 
        context.checking(new Expectations() {{
            allowing(connectionManager).getPushProxies();
            will(returnValue(pushProxies));
        }});
        return connectionManager;
    }

    /**
     * @return a {@link CustomAction} that fails an asynchronous http request executed by calling
     * {@link HttpExecutor#execute(HttpUriRequest, org.apache.http.params.HttpParams, HttpClientListener)}
     */
    public static CustomAction failUpload() {
        return new CustomAction("fail upload") {
            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                HttpClientListener httpClientListener = (HttpClientListener) invocation.getParameter(2);
                httpClientListener.requestFailed((HttpUriRequest) invocation.getParameter(0), null, new IOException());
                return null;
            }
        };
    }
    
    /**
     * @return a {@link Matcher} that matches an {@link HttpUriRequest} that contains <code>uriPart</code> 
     */
    public static Matcher<HttpUriRequest> createUriRequestMatcher(final String uriPart) {
        return new TypeSafeMatcher<HttpUriRequest>() {
            @Override
            public boolean matchesSafely(HttpUriRequest item) {
                return item.getURI().toString().contains(uriPart);
            }
            @Override
            public void describeTo(Description description) {
                description.appendText(uriPart);
            }
        };
    }
    
    /**
     * @return a {@link CustomAction} that will upload <code>data</code> in UTF-8 encoding to an aynchronous
     * request made by {@link HttpExecutor#execute(HttpUriRequest, org.apache.http.params.HttpParams, HttpClientListener)}
     */
    public static CustomAction upload(final byte[] data) {
        return new CustomAction("upload simpp file") {
            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                final HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
                ByteArrayEntity byteArrayEntity = new ByteArrayEntity(data);
                byteArrayEntity.setContentEncoding("UTF-8");
                httpResponse.setEntity(byteArrayEntity);
                HttpClientListener httpClientListener = (HttpClientListener) invocation.getParameter(2);
                httpClientListener.requestComplete((HttpUriRequest) invocation.getParameter(0), httpResponse);
                return null;
            }
        };
    }
}
