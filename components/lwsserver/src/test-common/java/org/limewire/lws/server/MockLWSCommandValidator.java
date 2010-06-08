package org.limewire.lws.server;

public class MockLWSCommandValidator implements LWSCommandValidator {

    @Override
    public boolean verifyBrowserIPAddresswithClientIP(String browserIPString) {
        return true;
    }

    @Override
    public boolean verifySignedParameter(String param, String signatureBits) {
        return true;
    }
}
