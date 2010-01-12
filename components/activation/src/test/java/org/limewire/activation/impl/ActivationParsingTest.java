package org.limewire.activation.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationResponseFactory;
import org.limewire.io.InvalidDataException;

/**
 * Unit test for {@link org.limewire.activation.impl.ActivationItemFactory}
 */
public class ActivationParsingTest extends LimeTestCase {

    private ActivationResponseFactory parser;

    @Override
    protected void setUp() throws Exception {
        parser = new ActivationResponseFactoryImpl(
            new ActivationItemFactoryImpl());
    }

    // Test a successful activation string 
    //
    public void testSuccessfulActivationString() throws Exception {

        String json = "{\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"response\":\"valid\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "      {\n" +
                "        \"id\":0,\n" +
                "        \"name\":\"Turbo-charged downloads\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20101001\",\n" +
                "        \"status\":active\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\":3,\n" +
                "        \"name\":\"AVG Anti Virus Protection\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20101231\",\n" +
                "        \"status\":active\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\":2,\n" +
                "        \"name\":\"Tech support\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20501001\",\n" +
                "        \"status\":unavailable\n" +
                "      }\n" +
                "    ]\n" +
                "}";

        ActivationResponse response = parser.createFromJson(json);

        assertEquals("DAVV-XXME-BWU3", response.getLid());
        assertTrue(response.isValidResponse());
        assertEquals("0pd15.1xM6.2xM6.3xM6", response.getMCode());
        assertEquals(1440, response.getRefreshInterval());
        assertEquals(3, response.getActivationItems().size());

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        ActivationItem item = response.getActivationItems().get(0);
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, item.getModuleID());
        assertEquals("Turbo-charged downloads", item.getLicenseName());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20091001", format.format(new Date(item.getDatePurchased())));
        assertEquals("20101001", format.format(new Date(item.getDateExpired())));

        item = response.getActivationItems().get(1);
        assertEquals(ActivationID.AVG_MODULE, item.getModuleID());
        assertEquals("AVG Anti Virus Protection", item.getLicenseName());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20091001", format.format(new Date(item.getDatePurchased())));
        assertEquals("20101231", format.format(new Date(item.getDateExpired())));

        item = response.getActivationItems().get(2);
        assertEquals(ActivationID.TECH_SUPPORT_MODULE, item.getModuleID());
        assertEquals("Tech support", item.getLicenseName());
        assertEquals(ActivationItem.Status.UNAVAILABLE, item.getStatus());
        assertEquals("20091001", format.format(new Date(item.getDatePurchased())));
        assertEquals("20501001", format.format(new Date(item.getDateExpired())));
    }

    public void testActivationResponseNoModules() throws Exception {
        
        String json = "{\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"response\":\"valid\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "    ]\n" +
                "}";
        ActivationResponse response = parser.createFromJson(json);

        assertEquals("DAVV-XXME-BWU3", response.getLid());
        assertTrue(response.isValidResponse());
        assertEquals("0pd15.1xM6.2xM6.3xM6", response.getMCode());
        assertEquals(1440, response.getRefreshInterval());
        assertEquals(0, response.getActivationItems().size());
        
    }
    
    public void testBadActivationItemStatus() throws Exception {
        String json = "{\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"response\":\"valid\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "      {\n" +
                "        \"id\":0,\n" +
                "        \"name\":\"Turbo-charged downloads\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20130901\",\n" +
                "        \"status\":not-a-real-status\n" +
                "      }\n" +
                "    ]\n" +
                "}";

        try {
            ActivationResponse response = parser.createFromJson(json);
            fail("Expected InvalidDataException");
        } catch (InvalidDataException e) {
            // Received InvalidDataException as expected 
        }
    }
}
