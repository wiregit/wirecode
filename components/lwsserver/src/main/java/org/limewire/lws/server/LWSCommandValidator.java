package org.limewire.lws.server;


public interface LWSCommandValidator {
    
    public boolean verifySignedParameter(String param, String signatureBits);
    
    public boolean verifyBrowserIPAddresswithClientIP(String browserIPString);

}
