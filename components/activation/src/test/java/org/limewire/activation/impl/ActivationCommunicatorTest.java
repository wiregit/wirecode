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
import java.util.concurrent.ScheduledExecutorService;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;

import junit.framework.Test;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.activation.impl.ActivationCommunicator.RequestType;
import org.limewire.http.httpclient.LimeWireHttpClientModule;
import org.limewire.http.LimeWireHttpModule;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivateAccessor;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.net.LimeWireNetTestModule;
import org.limewire.common.LimeWireCommonModule;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.CipherProviderImpl;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.apache.commons.codec.binary.Base64;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

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
   
    private static final String PUBLIC_KEY_A =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDS7whaG5tPvYMe0HYRDOFjAi3T2vQ0C/rrYaDyP7kt5LdB" +
        "+f8XG0/NThIiH7VN604UGMgQj7gV4ZvUysNtsQHJeOLR06QIpBnzYzEUzV6IfW7wcGferGIBOW6NU696d+2N" +
        "EJMJGxPbXSKzk0+4mkCkiO6PhpTPMWVgDoxQ4KyV7wIDAQAB";
    
    private static final String PRIVATE_KEY_A = 
        "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBANLvCFobm0+9gx7QdhEM4WMCLdPa9DQL+uth" +
        "oPI/uS3kt0H5/xcbT81OEiIftU3rThQYyBCPuBXhm9TKw22xAcl44tHTpAikGfNjMRTNXoh9bvBwZ96sYgE5" +
        "bo1Tr3p37Y0QkwkbE9tdIrOTT7iaQKSI7o+GlM8xZWAOjFDgrJXvAgMBAAECgYEAkty21fYutuBeMNA3xDtR" +
        "mhvkSINEUBCfTc+VvdU8W4XJSniDcVUkxO88lOG63Fue60Mt2MoYA7QnSYs7cl4xvQ9Usf1YsBdjkGWx4PC5" +
        "KMPibuID6aopZ3dM37jBuALOI+309XBR69ZBzhyxgAC+xS6WNX0sQuPynmv7q3ns6sECQQDwdxTM44DdA5AJ" +
        "G+MwfD+bZIBUJ/W1i04NsjxZskq+ANJ8x1KW+OTF0tpzvsuMPKcTkMABYbihOWEl0osLCI+xAkEA4I+MIznz" +
        "3buUjCue0gZIldtXJE04NEyy4An4DfzHNr4oqbdLcOkKoVWAVMm7gLhobcvAIQXNE+Tu3DEQHiWHnwJBAMkW" +
        "fBF+6uNoOEo1xP5l2PdEy0AVDpfbv9EaTPehbnmHzH3GXZ2c1AtOcZo7YpKKohltgfNl2fURO9laQSZf6XEC" +
        "QQCm3aQ5zOeM3cWdfxBuaqLnUGzpmcPpARFub5n28t4prJZUvtJ9XX47smhBGQKOvPlElUH4h/IDFXv0/TRH" +
        "4oVrAkEA6A0lKFX80ZS5+f5v3L6C0Lu78XOMHZGxDlM5u+7wF5+QUFfn1R3fQu04xwDnbEmuRAtbzHYq82BX" +
        "XvV2nfnqig==";
    
    
    private ActivationSettingStub settingsStub;
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
        settingsStub = (ActivationSettingStub)injector.getInstance(ActivationSettingsController.class);
        settingsStub.setActivationHost("http://127.0.0.1:8123/activate");
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
        modules.add(new AbstractModule() {
            @Override
            public void configure() {
                bind(ActivationSettingsController.class).toInstance(new ActivationSettingStub());
                bind(ActivationCommunicator.class).to(ActivationCommunicatorImpl.class);
                bind(ActivationResponseFactory.class).to(ActivationResponseFactoryImpl.class);
                bind(ActivationItemFactory.class).to(ActivationItemFactoryImpl.class);
                bind(CipherProvider.class).to(CipherProviderImpl.class);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(new SimpleTimer(true));
            }
        });
        modules.add(new LimeWireHttpModule());
        modules.add(new LimeWireCommonModule());
        modules.add(new LimeWireNetTestModule());
        return modules.toArray(new Module[modules.size()]);
    }
    
    // test successful server response
    //
    public void testSuccessfulServerResponse() throws Exception {
        settingsStub.serverPublicKey = PUBLIC_KEY_A;
        String base64PrivateKey = PRIVATE_KEY_A;
        
        String json = "{\n" +
                "  \"response\":\"valid\",\n" +
                "@LID@" +
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
        
        serverController.setPrivateKey(base64PrivateKey);
        serverController.setReturnJsonTemplate(json);
        serverController.startServer();
        
        ActivationCommunicator comm = injector.getInstance(ActivationCommunicator.class);
        ActivationResponse resp = comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
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
    
    // test the case when the activation server is unable to decrypt the "lidtoken"
    // which, when decrypted, would normally contain the String "[customer_lid],[random_number]"
    //
    public void testKeyMismatchServerUnsuccessfullyDecrypts() throws Exception {
        settingsStub.serverPublicKey =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDZCXZYjgP" +
            "Zw2OAyIuP7v8innxvatGO3xCKvWBftGq8LhJvwoTaRXOeGs" +
            "SxROFx6pdzfJDi1ODN6OjXzMmSzcJP+SkgvTnl6ZJO3YVY7V" +
            "sVVFrHl3TYzlPwhOcG+mk8867rPYyparOBtjau2mNnzHenFA" +
            "GCFXMKGI5DNRE65hQFmQIDAQAB";
        
            
        String json = "{\n" +
                "  \"response\":\"valid\",\n" +
                "@LID@" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "    ]\n" +
                "}";
        
        serverController.setReturnJsonTemplate(json);
        serverController.startServer();
        
        ActivationCommunicator comm = injector.getInstance(ActivationCommunicator.class);
        try {
            comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
            fail("Expected InvalidDataException");
        } catch (InvalidTokenException e) {
            assertEquals("random number security check failed", e.getMessage());
        }
    }
    
    // lw client sends encrypted lidtoken, but server sends no token in response
    // (as would happen in the case of a spoofed activation server)
    //
    public void testServerReturnsNoToken() throws Exception {
        String forceJsonReturn = "{\n" +
                "  \"response\":\"valid\",\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "    ]\n" +
                "}";
        
        serverController.setForceJsonReturn(forceJsonReturn);
        serverController.startServer();
        
        ActivationCommunicator comm = injector.getInstance(ActivationCommunicator.class);
        try {
            comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
            fail("Expected InvalidDataException");
        } catch (InvalidTokenException e) {
            assertEquals("random number security check failed", e.getMessage());
        }
    }
    
    // test server is down / connection refused
    //
    public void testConnectionRefused() throws Exception {
        try {
            comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
            fail("Expected IOException when server not listening.");
        } catch(IOException e) {
            // expected ioexception
        }
    }
    
    // test 404 file not found exception
    //
    public void test404ErrorResponse() throws Exception {
        settingsStub.setActivationHost("http://127.0.0.1:8123/invalid");
        serverController.setReturnJsonTemplate("dfgdfgd");
        serverController.startServer();
        try {
            comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
            fail("Expected IOException for 404 error.");
        } catch(IOException e) {
            // expected ioexception
            assertTrue(e.getMessage().startsWith("invalid http response, status: 404"));
        }
    }
    
    // test to make sure the LW client times out when the server can't be reached
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
            comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
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
        
        settingsStub.setActivationHost("http://" + unreachableIpAddress + ":8123/sfsdfs");
        Callable<ActivationResponse> contactUnreachableServer = new Callable<ActivationResponse>() {
            @Override
            public ActivationResponse call() throws Exception {
                return comm.activate("DAVV-XXME-BWU3", RequestType.USER_ACTIVATE);
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
        private String base64PrivateKey;
        
        private final HttpServer server = new HttpServer();
        private String jsonTemplate;
        private String forceJson;

        void setReturnJsonTemplate(String serverReturn) {
            this.jsonTemplate = serverReturn;
        }
        
        void setForceJsonReturn(String forceJson) {
            this.forceJson = forceJson;    
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
                String lidTokenParam = httpRequest.getParameter("lidtoken");
                String responseJson = getActivationJsonResponse(lidTokenParam);
                byte[] serverReturnBytes = responseJson.getBytes();
                httpResponse.getOutputStream().write(serverReturnBytes);
                httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
            } else {
                httpResponse.setStatus(org.mortbay.http.HttpResponse.__404_Not_Found);
            }
            httpResponse.setContentType("text/html");
            httpResponse.commit();
            httpRequest.setHandled(true);    
        }

        void setPrivateKey(String base64PrivateKey) {
            this.base64PrivateKey = base64PrivateKey;    
        }

        private String getActivationJsonResponse(String lidTokenParam) {
            if (forceJson != null) {
                return forceJson;
            }
            
            String lidAndTokenDecrypted = null;
            String errorResponseJson = "{\"response\":\"error\",\"lid\":\"" + lidTokenParam + "\",\"message\":" +
                    "\"Invalid 'lid'\"}";
            String responseJson = errorResponseJson;

            try {
                lidAndTokenDecrypted = decryptLidToken(lidTokenParam);
            } catch (GeneralSecurityException e) {
                assertEquals(errorResponseJson, responseJson);
            }

            if (lidAndTokenDecrypted != null) {
                String[] lidAndToken = lidAndTokenDecrypted.split(",");
                if (lidAndToken.length == 2) {
                    String lidReplace = "   \"lid\":\"" + lidAndToken[0] + "\",\n" +
                            "   \"token\":\"" + lidAndToken[1] + "\",\n";
                    responseJson = jsonTemplate.replace("@LID@", lidReplace);
                }
            }
            return responseJson;
        }

        private String decryptLidToken(String lidtoken) throws GeneralSecurityException {
            if (base64PrivateKey == null) {
                return "";
            }
            byte[] privateKeyBytes = Base64.decodeBase64(base64PrivateKey.getBytes());
            byte[] encryptedLidToken = Base64.decodeBase64(lidtoken.getBytes());
            byte[] decryptedBytes;
                KeyFactory fac = KeyFactory.getInstance("RSA");
                EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
                PrivateKey privateKey = fac.generatePrivate(spec);

                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                decryptedBytes = cipher.doFinal(encryptedLidToken);

            return new String(decryptedBytes);
        }
    }
}
