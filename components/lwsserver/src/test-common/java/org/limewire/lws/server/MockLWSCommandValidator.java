package org.limewire.lws.server;

public class MockLWSCommandValidator implements LWSCommandValidator {

    @Override
    public boolean verifyBrowserIPAddresswithClientIP(String browserIPString) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean verifySignedParameter(String param, String signatureBits) {
        // TODO Auto-generated method stub
        return true;
    }

}
