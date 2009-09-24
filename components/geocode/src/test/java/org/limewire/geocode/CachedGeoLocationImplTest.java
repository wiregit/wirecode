package org.limewire.geocode;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.inject.MutableProvider;
import org.limewire.inject.MutableProviderImpl;
import org.limewire.inject.Providers;
import org.limewire.io.ConnectableImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressEvent.Type;
import org.limewire.util.AssignParameterAction;

public class CachedGeoLocationImplTest extends TestCase {

    private Mockery context;
    private Geocoder geocoder;
    private CachedGeoLocationImpl geoLocation;
    private MutableProvider<Properties> geoLocationSetting;
    private MutableProvider<byte[]> addressProvider;

    public CachedGeoLocationImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        geocoder = context.mock(Geocoder.class);
        geoLocationSetting = new MutableProviderImpl<Properties>(new GeocodeInformation().toProperties());
        addressProvider = new MutableProviderImpl<byte[]>(new byte[0]);
        geoLocation = new CachedGeoLocationImpl(geoLocationSetting, Providers.of(geocoder), addressProvider);
    }
    
    public void testInitializeWillUseValueFromSetting() {
        addressProvider.set(new byte[] { (byte)129, 0, 0, 1 });
        GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.Ip, "129.0.0.1");
        geoLocationSetting.set(info.toProperties());
        context.checking(new Expectations() {{
            never(geocoder).getGeocodeInformation();
        }});
        
        geoLocation.initialize();
        GeocodeInformation result = geoLocation.get();
        assertEquals(info, result);
        
        context.assertIsSatisfied();
    }
    
    public void testInitializeWillDiscardValueFromSettingIfNoIp() {
        addressProvider.set(new byte[] { (byte)128, 0, 0, 1 });
        GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.CountryCode, "US");
        geoLocationSetting.set(info.toProperties());
        context.checking(new Expectations() {{
            never(geocoder).getGeocodeInformation();
        }});
        
        geoLocation.initialize();
        GeocodeInformation result = geoLocation.get();
        assertTrue(result.isEmpty());
        
        context.assertIsSatisfied();
    }
    

    public void testStartSavesNewValueToSetting() {
        final GeocodeInformation info = new GeocodeInformation();
        info.setProperty(Property.Ip, "129.0.0.1");
        context.checking(new Expectations() {{
            one(geocoder).getGeocodeInformation();
            will(returnValue(info));
        }});
        
        geoLocation.start();
        assertEquals(info.toProperties(), geoLocationSetting.get());
        context.assertIsSatisfied();
    }

    @SuppressWarnings("unchecked")
    public void testAddressEventTriggersGeoLocationToBeRefetched() {
        final GeocodeInformation newInfo = new GeocodeInformation();
        newInfo.setProperty(Property.Ip, "100.0.0.1");
        addressProvider.set(new byte[] { 100, 0, 0, 1 });
        final GeocodeInformation savedInfo = new GeocodeInformation();
        savedInfo.setProperty(Property.Ip, "129.0.0.1");
        geoLocationSetting.set(savedInfo.toProperties());
        final ListenerSupport<AddressEvent> listenerSupport = context.mock(ListenerSupport.class);
        final AtomicReference<EventListener<AddressEvent>> eventListener = new AtomicReference<EventListener<AddressEvent>>();
        
        context.checking(new Expectations() {{
            one(geocoder).getGeocodeInformation();
            will(returnValue(newInfo));
            one(listenerSupport).addListener(with(any(EventListener.class)));
            will(new AssignParameterAction<EventListener<AddressEvent>>(eventListener, 0));
        }});
        
        geoLocation.register(listenerSupport);
        geoLocation.initialize();
        eventListener.get().handleEvent(new AddressEvent(ConnectableImpl.INVALID_CONNECTABLE, Type.ADDRESS_CHANGED));
        
        assertEquals("100.0.0.1", geoLocation.get().getProperty(Property.Ip));
        context.assertIsSatisfied();
    }
}
