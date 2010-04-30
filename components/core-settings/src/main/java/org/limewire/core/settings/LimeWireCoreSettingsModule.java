package org.limewire.core.settings;

import java.util.Map;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.core.api.malware.VirusUpdatesURL;
import org.limewire.inject.AbstractModule;
import org.limewire.inspection.InspectionsServerUrls;
import org.limewire.setting.StringSetting;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class LimeWireCoreSettingsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<Map<String, StringSetting>>(){}).annotatedWith(InspectionsServerUrls.class).toProvider(InspectionsURLsMapProvider.class);
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
}
