package org.limewire.core.impl.integration;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.settings.GeocodeSettings;
import org.limewire.geocode.GeocodeInformation;
import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

public class CachedGeoLocationIntegrationTest extends LimeTestCase {

    
    public void testFetchesInfoFromServerAtStartup() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        GeocodeSettings.GEO_LOCATION.set(new Properties());
        GeocodeSettings.GEO_LOCATION.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                latch.countDown();
            }
        });
        CoreGlueTestUtils.createInjectorAndStart();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        GeocodeInformation info = GeocodeInformation.fromProperties(GeocodeSettings.GEO_LOCATION.get());
        assertEquals("US", info.getProperty(Property.CountryCode));
        assertNotNull(info.getProperty(Property.Ip));
        assertNotNull(info.getProperty(Property.Longitude));
        assertNotNull(info.getProperty(Property.Latitude));
    }
    

}
