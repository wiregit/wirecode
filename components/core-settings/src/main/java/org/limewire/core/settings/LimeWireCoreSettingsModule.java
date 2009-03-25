package org.limewire.core.settings;

import java.util.Properties;

import org.limewire.geocode.GeoLocations;
import org.limewire.geocode.GeocodeUrls;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.MutableProvider;

import com.google.inject.TypeLiteral;

public class LimeWireCoreSettingsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(GeocodeUrls.geocodeUrl()).toProvider(GeocodeSettings.GEOCODE_URL);
        bind(new TypeLiteral<MutableProvider<Properties>>(){}).annotatedWith(GeoLocations.geoLocation()).toInstance(GeocodeSettings.GEO_LOCATION);
    }
}
