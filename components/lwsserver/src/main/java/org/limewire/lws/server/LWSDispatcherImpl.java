/**
 * 
 */
package org.limewire.lws.server;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.limewire.util.URIUtils;


/**
 * Dispatches commands after going through an authentication phase explained
 * here.
 * 
 */
public final class LWSDispatcherImpl extends LWSDispatcherSupport {
    
    private LWSCommandValidator verifier;
    
    public LWSDispatcherImpl(LWSCommandValidator commandVerifier){
        this.verifier = commandVerifier;
    }
    
    
    @Override
    protected final Handler[] createHandlers() {
        return new Handler[] {
                new GetDownloadProgress(),
                new Download()
         };
    }
    
      /**
      * Returns the arguments to the right of the <code>?</code>. <br>
      * <code>static</code> for testing
      * 
      * @param request may be <code>null</code>
      * @return the arguments to the right of the <code>?</code>
      */
    @Override
    protected Map<String, String> getArgs(String request) {
         if (request == null || request.length() == 0) {
             return Collections.emptyMap();
         }
         int ihuh = request.indexOf('?');
         if (ihuh == -1) {
             return Collections.emptyMap();
         }
         final String rest = request.substring(ihuh + 1);
         return LWSServerUtil.parseArgs(rest);
     }   
     
     @Override
     protected String getCommand(String request) {
         int iprefix = request.indexOf(PREFIX);
         String res = iprefix == -1 ? request : request.substring(iprefix + PREFIX.length());
         final char[] cs = { '#', '?' };
         for (char c : cs) {
             final int id = res.indexOf(c);
             if (id != -1)
                 res = res.substring(0, id);
         }
         return res;
     }

    // ------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------
    
    class GetDownloadProgress extends HandlerWithCallback{
        
        @Override
        protected void handleRest(Map<String, String> args, StringCallback cb){
            if (getCommandReceiver() == null) {
                cb.process(LWSDispatcherSupport.Responses.NO_DISPATCHEE);
                return;
            }
            
            String signedBrowserIP = args.get("signedBrowserIP");
            String browserIP = args.get("browserIP");
            
            
            // verify browser and client are on same machine
            if(LWSServerUtil.isEmpty(browserIP) || !verifier.verifyBrowserIPAddresswithClientIP(browserIP)){  
                cb.process(LWSDispatcherSupport.Responses.BROWSER_CLIENT_IP_DONOT_MATCH);
                return; 
            }
            
            // verify browserIP signature
            if(LWSServerUtil.isEmpty(signedBrowserIP) || !verifier.verifySignedParameter(browserIP, signedBrowserIP)){
                cb.process(LWSDispatcherSupport.Responses.INVALID_IP_SIGNATURE);
                return; 
            }
            
            String res = getCommandReceiver().receiveCommand(name(), args);
            cb.process(res);         
        }
    }

    /**
     * Sent from code with parameter {@link Parameters#COMMAND}.
     */
    class Download extends HandlerWithCallback{
        
        @Override
        protected void handleRest(Map<String, String> args, StringCallback cb) {
            
            LWSReceivesCommandsFromDispatcher receiver = getCommandReceiver();
            
            if (receiver == null) {    
                cb.process(LWSDispatcherSupport.Responses.NO_DISPATCHEE);
                return;
            }
            
            String lwsDownloadRequest = null;
            String url = args.get(Parameters.URL);
            if(LWSServerUtil.isEmpty(url)){
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_PARAMETER));
                return;
            }
            
            
            try { lwsDownloadRequest = URIUtils.decodeToUtf8(url);
            } catch (URISyntaxException e) {
                cb.process(LWSDispatcherSupport.Responses.INVALID_DOWNLOAD);
                return;
            }
            Map<String, String> downloadArgs = getArgs(lwsDownloadRequest);
             
            // extract required parameters from download URL
            String signedHash = downloadArgs.get("signedHash");
            String hash = downloadArgs.get("hash");
            String signedBrowserIP = downloadArgs.get("signedBrowserIP");
            String browserIP = downloadArgs.get("browserIP");
            if(LWSServerUtil.isEmpty(signedHash) || LWSServerUtil.isEmpty(hash) ||
                    LWSServerUtil.isEmpty(signedBrowserIP) ||LWSServerUtil.isEmpty(browserIP) ){
                cb.process(LWSDispatcherSupport.Responses.INVALID_DOWNLOAD);
                return;
            }
             
            // verify hash signature 
            if(!verifier.verifySignedParameter(hash, signedHash)){
                cb.process(LWSDispatcherSupport.Responses.INVALID_HASH_SIGNATURE);
                return;
            }
            
            // verify browserIP signature
            if(!verifier.verifySignedParameter(browserIP, signedBrowserIP)){
                cb.process(LWSDispatcherSupport.Responses.INVALID_IP_SIGNATURE);
                return; 
            }
            
            
            // verify browser and client are on same machine
            if(!verifier.verifyBrowserIPAddresswithClientIP(browserIP)){  
                cb.process(LWSDispatcherSupport.Responses.BROWSER_CLIENT_IP_DONOT_MATCH);
                return; 
            }
            
            String res = receiver.receiveCommand(name(), args);
            if(res != null){
                cb.process(res);
                return;
            }
            
            cb.process(LWSDispatcherSupport.Responses.INVALID_DOWNLOAD);
        }
    }   

}