package org.limewire.activation;

import java.io.*;

import java.net.URISyntaxException;
import java.util.ArrayList;

import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.limewire.http.httpclient.LimeHttpClient;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.settings.PROActivationSettings;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Activator {

    private final LimeHttpClient limeHttpClient;
    
    // TODO: This really isn't state outside one run.  Something in this whole package needs redesign.
    private String proActivationID;
    private String clientGUID;
    private String proActivationIDDate;
    private String proActivationLookupURL;
    
    @Inject
    public Activator(LimeHttpClient limeHttpClient) {
        this.limeHttpClient = limeHttpClient;
    }
    
    private void setup() {
        proActivationIDDate = PROActivationSettings.PRO_ACTIVATION_ID_DATE.getValue();

        byte[] myClientGUID = GuiCoreMediator.getApplicationServices().getMyGUID();
        clientGUID = EncodingUtils.encode(new GUID(myClientGUID).toHexString());
        
        proActivationID = PROActivationSettings.PRO_ACTIVATION_ID.getValue();
        proActivationLookupURL = PROActivationSettings.PRO_ACTIVATION_LOOKUP_URL.getValue();
        
        if ( proActivationIDDate == null || "".equals(proActivationIDDate) ) { 
            fail("Your PRO activation date was missing.");
        }
        
        if ( proActivationID == null || "".equals(proActivationID) || 
             !clientGUID.substring(0,6).equals(proActivationID.substring(10,16)) ) {
            fail("Your PRO activation ID was missing or invalid.");
        }
    }
    
    private void fail(String msg) {
        // TODO: implement proper error conditions and normal failures.
        System.out.println(msg);
    }
    
    public void attemptActivation() {
        String strActivationKey = requestSalesVerification();
        System.out.println(strActivationKey);
        doActivation();
    }
    
    public void checkForExpiredActivation() {
        // TODO: implement.
        doDeactivation();
    }
    
    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    private String requestSalesVerification() {
        
        setup();  // TODO:  Yeah, yeah.  I know.
        
        //
        // Build a request for a PRO Activation lookup based on saved settings.
        //
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("id", proActivationID));
        nameValuePairs.add(new BasicNameValuePair("date", proActivationIDDate));
        nameValuePairs.add(new BasicNameValuePair("clientGUID", clientGUID));

        HttpPost post = null;
        try {
            post = new HttpPost(proActivationLookupURL);
        } catch (URISyntaxException e) {
            fail(e.toString());
            return null;
        }
        final HttpPost request = post;
        try {
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e) {
            fail(e.toString());
            return null;
        }
        HttpParams params = new BasicHttpParams();   // TODO:  Was this not needed really?
        request.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        
        try {
            HttpResponse response = limeHttpClient.execute(post);
            InputStream stream = response.getEntity().getContent();
       
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
            }

            br.close();
            return sb.toString();
            
        } catch (HttpException e) {
            fail(e.toString());
        } catch (IOException e) {
            fail(e.toString());
        }
        return null;
    }
    
    private void doActivation() {
        System.out.println("doActivation");
        // TODO: implement.
    }
    
    private void doDeactivation() {
        System.out.println("doDeactivation");
        // TODO: implement.
    }
    
       
}
