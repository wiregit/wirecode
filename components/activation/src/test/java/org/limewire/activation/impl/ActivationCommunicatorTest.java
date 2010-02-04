package org.limewire.activation.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.http.httpclient.LimeWireHttpClientModule;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivateAccessor;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/**
 * Test for {@link ActivationCommunicatorImpl}
 * 
 * Some scenarios tested:
 * 
 * - server down, ioexception expected
 * - server up
 *   - invalid json data sent back
 *     - string, or junk bytes --> invaliddataexception expected
 *   - valid json data sent back
 *   - server times out, never sends anything back 
 * 
 */
public class ActivationCommunicatorTest extends BaseTestCase {
    
    private ServerController serverController;
    private Injector injector;
    private ActivationCommunicator comm;
    
    public ActivationCommunicatorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActivationCommunicatorTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        injector = createInjector(getModules());
        serverController = new ServerController();
        comm = injector.getInstance(ActivationCommunicator.class);
//        ActivationSettings.ACTIVATION_HOST.set("http://localhost:8123/activate");
    }
    
    @Override
    protected void tearDown() throws Exception {
        serverController.stopServer();
    }
    
    private Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.DEVELOPMENT, modules);
    }
    
    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
//        modules.add(new LimeWireCoreModule());
//        modules.add(new CoreGlueModule());
        return modules.toArray(new Module[modules.size()]);
    }
    
    // test successful server response
    //
    public void testSuccessfulServerResponse() throws Exception {
        
        String json = "{\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"response\":\"valid\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "      {\n" +
                "        \"id\":1,\n" +
                "        \"name\":\"Turbo-charged downloads\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20191001\",\n" +
                "        \"status\":active\n" +
                "      }\n" +
                "    ]\n" +
                "}";
        
        serverController.setSetServerReturn(json);
        serverController.startServer();
        
        ActivationCommunicator comm = injector.getInstance(ActivationCommunicator.class);
        ActivationResponse resp = comm.activate("DAVV-XXME-BWU3");
        List<ActivationItem> items = resp.getActivationItems();
        assertEquals(1, items.size());        
        ActivationItem item = items.get(0);
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, item.getModuleID());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("Turbo-charged downloads", item.getLicenseName());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20191001", format.format(item.getDateExpired()));
    }
    
    // test server is down / connection refused
    //
    public void testConnectionRefused() throws Exception {
        try {
            comm.activate("DAVV-XXME-BWU3");
            fail("Expected IOException when server not listening.");
        } catch(IOException e) {
            // expected ioexception
        }
    }
    
    // test 404 file not found exception
    //
    public void test404ErrorResponse() throws Exception {
//        ActivationSettings.ACTIVATION_HOST.set("http://localhost:8123/invalid");
        serverController.setSetServerReturn("dfgdfgd");
        serverController.startServer();
        try {
            comm.activate("DAVV-XXME-BWU3");
            fail("Expected IOException for 404 error.");
        } catch(IOException e) {
            // expected ioexception
            assertTrue(e.getMessage().startsWith("invalid http response, status: 404"));
        }
    }
    
    // test to make sure the LW client times out if server does not respond.
    //
    public void testNoResponseFromServerClientTimesOut() throws Exception {
        LimeWireHttpClientModule.class.getClass();
        PrivateAccessor accessor = new PrivateAccessor(
            Class.forName(LimeWireHttpClientModule.class.getName()), null, "TIMEOUT");
        final int timeout = ((Integer)accessor.getOriginalValue());
        
        serverController.startServer(new AbstractHttpHandler() {
            @Override
            public void handle(String s, String s1, 
                               HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
                // wait a long time to simulate swallowing packets
                // client should timeout LONG before this interval
                try {
                    Thread.sleep(timeout+2000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });
        try {
            comm.activate("DAVV-XXME-BWU3");
            fail("Should have received IOException: read timed out");
        } catch(SocketTimeoutException e) {
            // expected ioexception
            assertTrue(e.getMessage().startsWith("read timed out"));
        }    
    }
    
    // test to make sure the LW client times out server cannot be reached 
    // (such as if the address is unrouteable)
    //
    public void testUnreachableServerClientTimesOut() throws Exception {
        String unreachableIpAddress = "172.16.0.253";
        LimeWireHttpClientModule.class.getClass();
        PrivateAccessor accessor = new PrivateAccessor(
            Class.forName(LimeWireHttpClientModule.class.getName()), null, "CONNECTION_TIMEOUT");
        final int timeout = ((Integer)accessor.getOriginalValue()) + 2000;
        
//        ActivationSettings.ACTIVATION_HOST.set("http://" + unreachableIpAddress + ":8123/sfsdfs");

        Callable<ActivationResponse> contactUnreachableServer = new Callable<ActivationResponse>() {
            @Override
            public ActivationResponse call() throws Exception {
                return comm.activate("DAVV-XXME-BWU3");
            }
        };
        ExecutorService poolForReachingServer = Executors.newSingleThreadExecutor();
        Future<ActivationResponse> reachServerResult = poolForReachingServer.submit(contactUnreachableServer);
        
        try {
            reachServerResult.get(timeout, TimeUnit.MILLISECONDS);
            fail("Expected a SocketTimeoutException");
        } catch (ExecutionException e) {
            assertInstanceof(SocketTimeoutException.class, e.getCause());
        }
    }
    


    private class ServerController extends ResourceHandler {
        
        private final String SERVER_ROOT_DIR = "";//_baseDir.getAbsolutePath();
        
        private final HttpServer server = new HttpServer();
        private String serverReturn;
        
        void setSetServerReturn(String serverReturn) {
            this.serverReturn = serverReturn;
        }
        
        void startServer() throws Exception {
            startServer(this);    
        }

        void startServer(HttpHandler handler) throws Exception {
            SocketListener listener = new SocketListener();
            listener.setPort(8123);
            listener.setMinThreads(1);
            server.addListener(listener);
            HttpContext context = server.addContext("");

            context.setResourceBase(SERVER_ROOT_DIR);
            setAcceptRanges(true);
            setDirAllowed(true);

            context.addHandler(handler);
            context.addHandler(new NotFoundHandler());
            server.start();
        }
        
        void stopServer() {
            try {
                server.stop();
            } catch (InterruptedException e) {
                // ignoring
            }
        }
        
        @Override
        public void handle(String s, String s1, HttpRequest httpRequest, HttpResponse httpResponse)
        throws IOException {
            String path = httpRequest.getURI().getPath();
            if (path.equals("/activate")) {
                httpResponse.getOutputStream().write(serverReturn.getBytes());
                httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
            } else {
                httpResponse.setStatus(org.mortbay.http.HttpResponse.__404_Not_Found);
            }
            httpResponse.setContentType("text/html");
            httpResponse.commit();
            httpRequest.setHandled(true);    
        }
        
    }
}