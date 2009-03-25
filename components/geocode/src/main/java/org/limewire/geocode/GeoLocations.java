package org.limewire.geocode;

import java.lang.annotation.Annotation;

public class GeoLocations {
    public static GeoLocation geoLocation() {
        return new GeoLocationImpl();    
    }

    private static class GeoLocationImpl implements GeoLocation{
        @Override
        public Class<? extends Annotation> annotationType() {
            return GeoLocation.class;
        }
    }
}
