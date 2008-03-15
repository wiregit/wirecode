package org.limewire.promotion.impressions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Instances of this will use the httpclient classes directly to execute HTTP
 * requests.
 */
public class TestContainerRequester extends ContainerRequester {

    @Override
    protected void handle(PostMethod request) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        int ret = client.executeMethod(request);

        if (ret == HttpStatus.SC_NOT_IMPLEMENTED) {
            System.err.println("The Post method is not implemented by this URI");
            // still consume the response body
            request.getResponseBodyAsString();
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getResponseBodyAsStream()));
            String line;
            while (((line = in.readLine()) != null)) {
                System.out.println(line);
            }
        }
    }

    @Override
    protected void error(Exception e) {
        e.printStackTrace();
    }

    @Override
    protected String getUserAgent() {
        return LimeWireUtils.getHttpServer(); // todo
    }

}
