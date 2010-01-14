package org.limewire.activation.impl;

import java.text.SimpleDateFormat;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationID;
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

    // Test a successful activation string with 3 modules, 2 of which
    // are active, and 1 of which is unavailable.
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
                "        \"id\":1,\n" +
                "        \"name\":\"Optimized Search Results\",\n" +
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
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20101001", format.format(item.getDateExpired()));

        item = response.getActivationItems().get(1);
        assertEquals(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE, item.getModuleID());
        assertEquals("Optimized Search Results", item.getLicenseName());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20101231", format.format(item.getDateExpired()));

        item = response.getActivationItems().get(2);
        assertEquals(ActivationID.TECH_SUPPORT_MODULE, item.getModuleID());
        assertEquals("Tech support", item.getLicenseName());
        assertEquals(ActivationItem.Status.UNAVAILABLE, item.getStatus());
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20501001", format.format(item.getDateExpired()));
    }

    // test a JSON response which contains no modules
    //
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
    
    // test with an unknown module.  An unknown module should return
    // a status of "unuseable_lw", regardless of the status of the 
    // module returned in the JSON
    public void testUnknownModule() throws Exception {

        String json = "{\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"response\":\"valid\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "      {\n" +
                "        \"id\":12,\n" +
                "        \"name\":\"Turbo-charged downloads\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20501001\",\n" +
                "        \"status\":active\n" +
                "      }\n" +
                "    ]\n" +
                "}";

        ActivationResponse response = parser.createFromJson(json);
        assertEquals("DAVV-XXME-BWU3", response.getLid());
        assertTrue(response.isValidResponse());
        assertEquals("0pd15.1xM6.2xM6.3xM6", response.getMCode());
        assertEquals(1440, response.getRefreshInterval());
        assertEquals(1, response.getActivationItems().size());
        
        ActivationItem item = response.getActivationItems().get(0);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        assertEquals(ActivationID.UNKNOWN_MODULE, item.getModuleID());
        assertEquals("Turbo-charged downloads", item.getLicenseName());
        assertEquals(ActivationItem.Status.UNUSEABLE_LW, item.getStatus());
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20501001", format.format(item.getDateExpired()));
    }
    
    // test a json which has an invalid status in one of its modules.
    //
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
            parser.createFromJson(json);
            fail("Expected InvalidDataException");
        } catch (InvalidDataException e) {
            // Received InvalidDataException as expected 
        }
    }
    
    // test Strings which are not JSON.
    //
    public void testNonJsonInputError() throws Exception {
        String json = "non-json-string-test";

        try {
            parser.createFromJson(json);
            fail("Expected InvalidDataException");
        } catch (InvalidDataException e) {
            // Received InvalidDataException as expected 
        }
    }
}
