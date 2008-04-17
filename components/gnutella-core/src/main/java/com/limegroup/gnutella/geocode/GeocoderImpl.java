package com.limegroup.gnutella.geocode;

import org.limewire.geocode.DefaultGeocoder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.settings.GeocodeSettings;

@Singleton
public final class GeocoderImpl extends DefaultGeocoder {
    
    @Inject
    public GeocoderImpl(Provider<HttpExecutor> exe) {
        super(new HttpExecutorSuccessOrFailureCallbackConsumer(
                exe, GeocodeSettings.GEOCODE_URL.getValue()));
    }

}
