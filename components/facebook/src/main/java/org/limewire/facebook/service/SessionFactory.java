package org.limewire.facebook.service;

public interface SessionFactory {
    String getSession(String authToken);
    String getSecret(String session);
}
