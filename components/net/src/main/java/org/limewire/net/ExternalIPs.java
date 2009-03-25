package org.limewire.net;

import java.lang.annotation.Annotation;

public class ExternalIPs {
    public static ExternalIP externalIP() {
        return new ExternalIPImpl();    
    }
    
    private static class ExternalIPImpl implements ExternalIP{
        @Override
        public Class<? extends Annotation> annotationType() {
            return ExternalIP.class;
        }
    }
}
