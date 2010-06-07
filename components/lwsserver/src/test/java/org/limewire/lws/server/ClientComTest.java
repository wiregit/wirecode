package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LocalServerDelegate.URLConstructor;

public class ClientComTest extends AbstractCommunicationSupport {
    
    public ClientComTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(ClientComTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
    
    public void testPing() {
        getCode().sendPing(new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(String res) {
                //
                // This will be encoded strangely, but just check the length
                //
                String s = new String(LWSDispatcherSupport.PING_BYTES);
                assertTrue("'" + s + "' should contain PNG", s.indexOf("PNG") != -1);
            }
        });
    } 
    
    public void testGetDownloadProgress() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        
        String browserIP = MockLWSNetworkAdapterImpl.getIPAddress();
        args.put("browserIP", browserIP);
        String browserIPSignature = getSignedBytes(browserIP);
        args.put("signedBrowserIP", browserIPSignature);       
        args.put("callback", "DUMMY");
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.GET_DOWNLOAD_PROGRESS, args,
                new FakeJavascriptCodeInTheWebpage.Handler() {
                    public void handle(String res) {
                        assertTrue("'" + res + "' should not contain error", res.indexOf("ERROR") == -1);
                    }
                });
    }
    
    public void testDownload() throws Exception {
        String hash = "af8b74763ace4069df7020821bfb9c175af1208a";
        String browserIP = MockLWSNetworkAdapterImpl.getIPAddress();
        Map<String, String> downloadArgs = getDownloadArgs(hash, browserIP);
        String url = getDownloadURL(downloadArgs);
        
        Map<String, String> args = new HashMap<String, String>();
        args.put("url", url);
        args.put("callback", "_DUMMY");
        getCode().sendLocalMsg(LWSDispatcher.PREFIX + LWSDispatcherSupport.Commands.DOWNLOAD, args,
                new FakeJavascriptCodeInTheWebpage.Handler() {
                    public void handle(String res) {
                        assertTrue("OK == " + res, LWSDispatcherSupport.Responses.OK.equals(LWSServerUtil.removeCallback(res)));
                    }
                });
    }
    
    public void testEmptyDownloadRequest() {
        Map<String, String> args = new HashMap<String, String>();
        args.put("url", "");
        args.put("callback", "_DUMMY");
        getCode().sendLocalMsg(LWSDispatcher.PREFIX + LWSDispatcherSupport.Commands.DOWNLOAD, args, 
                errorHandler(LWSDispatcherSupport.ErrorCodes.MISSING_PARAMETER));
    }
    
    public void testDownloadInvalidHashSignature() throws Exception {
        String hash = "af8b74763ace4069df7020821bfb9c175af1208a";
        String browserIP = MockLWSNetworkAdapterImpl.getIPAddress();
        Map<String, String> downloadArgs = getDownloadArgs(hash, browserIP);
        downloadArgs.put("signedHash", "SDFJFDGKL34543JFKLDJGJGKLDFJGKLGDFJFLKJRTKLWEKLJ4KRTJ34KRTJDFKVJKJR3KL4JKLJ34KRJ34KLRJKQL");        
        String url = getDownloadURL(downloadArgs);
        
        Map<String, String> args = new HashMap<String, String>();
        args.put("url", url);
        args.put("callback", "_DUMMY");
        getCode().sendLocalMsg(LWSDispatcher.PREFIX + LWSDispatcherSupport.Commands.DOWNLOAD, args, 
            new FakeJavascriptCodeInTheWebpage.Handler() {
                public void handle(String res) {
                    assertTrue("OK != " + res, LWSDispatcherSupport.Responses.INVALID_HASH_SIGNATURE.equals(LWSServerUtil.removeCallback(res)));
                }
            });
    }
    
    public void testDownloadInvalidBrowserIPSignature() throws Exception {
        String hash = "af8b74763ace4069df7020821bfb9c175af1208a";
        String browserIP = MockLWSNetworkAdapterImpl.getIPAddress();
        Map<String, String> downloadArgs = getDownloadArgs(hash, browserIP);
        downloadArgs.put("signedBrowserIP", "SDFJFDGKL34543JFKLDJGJGKLDFJGKLGDFJFLKJRTKLWEKLJ4KRTJ34KRTJDFKVJKJR3KL4JKLJ34KRJ34KLRJKQL");
        String url = getDownloadURL(downloadArgs);
        
        Map<String, String> args = new HashMap<String, String>();
        args.put("url", url);
        args.put("callback", "_DUMMY");
        getCode().sendLocalMsg(LWSDispatcher.PREFIX + LWSDispatcherSupport.Commands.DOWNLOAD, args, 
            new FakeJavascriptCodeInTheWebpage.Handler() {
                public void handle(String res) {
                    assertTrue("OK != " + res, LWSDispatcherSupport.Responses.INVALID_IP_SIGNATURE.equals(LWSServerUtil.removeCallback(res)));
                }
            });     
    }
    
    public void testDownloadInconsistentBrowserClientIP() throws Exception {
        String hash = "af8b74763ace4069df7020821bfb9c175af1208a";
        String browserIP = "10.0.0.1";
        Map<String, String> downloadArgs = getDownloadArgs(hash, browserIP);    
        String url = getDownloadURL(downloadArgs);

        Map<String, String> args = new HashMap<String, String>();
        args.put("url", url);
        args.put("callback", "_DUMMY");
        getCode().sendLocalMsg(LWSDispatcher.PREFIX + LWSDispatcherSupport.Commands.DOWNLOAD, args, 
            new FakeJavascriptCodeInTheWebpage.Handler() {
                public void handle(String res) {
                    assertTrue("OK != " + res, LWSDispatcherSupport.Responses.BROWSER_CLIENT_IP_DONOT_MATCH.equals(LWSServerUtil.removeCallback(res)));
                }
            });
    }
    
    //
    // Utility Methods.
    //  
    private Map<String, String> getDownloadArgs(String hash, String browserIP) throws Exception {
        Map<String, String> downloadArgs = new HashMap<String, String>();
        downloadArgs.put("hash", hash);
        downloadArgs.put("browserIP", browserIP);
        
        String signedHash = getSignedBytes(hash);
        String signedBrowserIP = getSignedBytes(browserIP);
        downloadArgs.put("signedHash", signedHash);
        downloadArgs.put("signedBrowserIP", signedBrowserIP);
        
        return downloadArgs;
    }
    
    private String getDownloadURL(Map<String, String> downloadArgs) {
        URLConstructor ctor = LocalServerDelegate.NormalStyleURLConstructor.INSTANCE;
        String url = ctor.constructURL("/store/download", downloadArgs);
        return url;
    }
}
