package org.limewire.geocode;

import org.limewire.inject.AbstractModule;

import com.limegroup.gnutella.geocode.CachedGeoLocation;
import com.limegroup.gnutella.geocode.CachedGeoLocationImpl;

/**
 * Main module for the geocoder component.
 */
public class LimewireGeocodeModule extends AbstractModule {
    
    private final Class<? extends Geocoder> geocoderClass;
    
    public LimewireGeocodeModule(Class<? extends Geocoder> geocoderClass) {
        this.geocoderClass = geocoderClass;
    }

    @Override
    protected void configure() {
        bind(Geocoder.class).to(geocoderClass);
        bind(CachedGeoLocation.class).to(CachedGeoLocationImpl.class);
    }
}
