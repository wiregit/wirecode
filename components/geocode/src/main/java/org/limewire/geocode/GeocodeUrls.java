package org.limewire.geocode;

import java.lang.annotation.Annotation;

public class GeocodeUrls {
    public static GeocodeUrl geocodeUrl() {
        return new GeocodeUrlImpl();    
    }
    
    private static class GeocodeUrlImpl implements GeocodeUrl{
        @Override
        public Class<? extends Annotation> annotationType() {
            return GeocodeUrl.class;
        }
    }
    
}
