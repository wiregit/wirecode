package org.limewire.core.settings;

import java.util.Map;
import java.util.Properties;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.core.api.malware.VirusUpdatesURL;
import org.limewire.core.api.search.store.StoreEnabled;
import org.limewire.core.api.search.store.StoreSearchEnabled;
import org.limewire.geocode.GeoLocation;
import org.limewire.geocode.GeocodeUrl;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.BroadcastingMutableProvider;
import org.limewire.inject.MutableProvider;
import org.limewire.inspection.InspectionsServerUrls;
import org.limewire.listener.EventListener;
import org.limewire.promotion.search.StoreAPIURL;
import org.limewire.promotion.search.StoreCookieDomain;
import org.limewire.promotion.search.StoreLoginPopupURL;
import org.limewire.setting.StringSetting;
import org.mozilla.browser.MozillaInitialization;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class LimeWireCoreSettingsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(GeocodeUrl.class).toProvider(GeocodeSettings.GEOCODE_URL);
        bind(new TypeLiteral<MutableProvider<Properties>>(){}).annotatedWith(GeoLocation.class).toInstance(GeocodeSettings.GEO_LOCATION);
        bind(new TypeLiteral<Map<String, StringSetting>>(){}).annotatedWith(InspectionsServerUrls.class).toProvider(InspectionsURLsMapProvider.class);
        bind(new TypeLiteral<String>(){}).annotatedWith(StoreAPIURL.class).toProvider(SearchSettings.STORE_API_URL);
        final BooleanProviderDecorator storeEnabled = new BooleanProviderDecorator(LWSSettings.SHOW_STORE_COMPONENTS) {
            @Override
            public Boolean get() {
                return super.get() && MozillaInitialization.isInitialized();
            }
        };
        bind(new TypeLiteral<BroadcastingMutableProvider<Boolean>>(){}).annotatedWith(StoreEnabled.class).toInstance(storeEnabled);
        bind(new TypeLiteral<MutableProvider<Boolean>>(){}).annotatedWith(StoreEnabled.class).toInstance(storeEnabled);
        bind(new TypeLiteral<Boolean>(){}).annotatedWith(StoreEnabled.class).toProvider(storeEnabled);
        
        final BooleanProviderDecorator storeSearchEnabled = new BooleanProviderDecorator(storeEnabled) {
            @Override
            public Boolean get() {
                return super.get() && SearchSettings.ENABLE_STORE_SEARCH.get();
            }
        };
        
        bind(new TypeLiteral<Boolean>(){}).annotatedWith(StoreSearchEnabled.class).toProvider(storeSearchEnabled);
        bind(new TypeLiteral<String>(){}).annotatedWith(StoreLoginPopupURL.class).toProvider(LWSSettings.LWS_CLIENT_LOGIN_POPUP_URL);
        bind(new TypeLiteral<String>(){}).annotatedWith(StoreCookieDomain.class).toProvider(LWSSettings.LWS_COOKIE_DOMAIN);
        bind(new TypeLiteral<String>(){}).annotatedWith(VirusUpdatesURL.class).toProvider(DownloadSettings.VIRUS_UPDATES_SERVER);
    }

    @Singleton
    private static class InspectionsURLsMapProvider extends AbstractLazySingletonProvider<Map<String, StringSetting>> {
        @Override
        protected Map<String, StringSetting> createObject() {
            return ImmutableMap.of(
                InspectionsServerUrls.INSPECTION_SPEC_REQUEST_URL, InspectionsSettings.INSPECTION_SPEC_REQUEST_URL, 
                InspectionsServerUrls.INSPECTION_SPEC_SUBMIT_URL, InspectionsSettings.INSPECTION_SPEC_SUBMIT_URL);        
        }
    }
    
    private abstract class BooleanProviderDecorator implements BroadcastingMutableProvider<Boolean> {
        
        final BroadcastingMutableProvider<Boolean> delegate;

        public BooleanProviderDecorator(BroadcastingMutableProvider<Boolean> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void set(Boolean newValue) {
            delegate.set(newValue);
        }

        @Override
        public Boolean get() {
            return delegate.get();
        }

        @Override
        public void addListener(EventListener<Boolean> booleanEventListener) {
            delegate.addListener(booleanEventListener);
        }

        @Override
        public boolean removeListener(EventListener<Boolean> booleanEventListener) {
            return delegate.removeListener(booleanEventListener);
        }
    }
}