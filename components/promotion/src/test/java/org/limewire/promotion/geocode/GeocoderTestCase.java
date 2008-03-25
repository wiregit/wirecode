package org.limewire.promotion.geocode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.promotion.geocode.GeocodeInformation;
import org.limewire.promotion.geocode.Geocoder;
import org.limewire.util.BaseTestCase;

public class GeocoderTestCase extends BaseTestCase {

    public GeocoderTestCase(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(GeocoderTestCase.class);
    }
    
    private Geocoder geo;
    private Map<String,String> props;
        
    @Override
    protected void setUp() throws Exception {
        
        // Build the properties
        props = new HashMap<String,String>();
        props.put("CountryCode","US");
        props.put("CountryCode3","USA");
        props.put("CountryName","United States");
        props.put("Region","NY");
        props.put("Region2","New York");
        props.put("City","New York");
        props.put("PostalCode","10004");
        props.put("Latitude","40.6888");
        props.put("Longitude","74.0203");
        props.put("DmaCode","501");
        props.put("AreaCode","212");
        
        // Build the String
        final String SEPARATOR = "\t";
        final String NEWLINE = "\n";
        String testString = SEPARATOR + NEWLINE;
        for (Map.Entry<String,String> en : props.entrySet()) {
            testString += en.getKey() + SEPARATOR + en.getValue() + NEWLINE;
        }
        geo = new DefaultGeocoder(new DefaultStreamSuccessOrFailureCallbackConsumer(new StringInputStream(testString)));
    }
    
    public void testSimple() throws InterruptedException {
        
        // Test some state information
        assertFalse(geo.isReady());
        geo.initialize();
        assertTrue(geo.isReady());
        assertFalse(geo.hasFailed());
        GeocodeInformation info = null;
        
        // Let the geo get ready
        while (info == null || !geo.isReady()) {
            info = geo.getGeocodeInformation();
            if (info == null) Thread.sleep(1000);            
        }
        
        // Make sure we have all the correct properties
        System.out.println(info);
        for (GeocodeInformation.Property p : GeocodeInformation.getStrings2Properties().values()) {
            assertEquals(p.getValue() + " should be in " + info, props.get(p.getValue()), info.getProperty(p));
        }
    }
    
    public void testWantNull() {
        GeocodeInformation info = geo.getGeocodeInformation();
        assertNull(String.valueOf(info), info);
        assertFalse("geo.isReady()", geo.isReady());
        assertFalse("geo.hasFailed()", geo.hasFailed());
    }
    
    private final class StringInputStream extends InputStream {
        private final String buf;
        private int cur;
        StringInputStream(String buf) {
            this.buf = buf;
        }
        @Override
        public int read() throws IOException {
            if (cur < buf.length()) return buf.charAt(cur++);
            return -1;
        }
    }
    
}
