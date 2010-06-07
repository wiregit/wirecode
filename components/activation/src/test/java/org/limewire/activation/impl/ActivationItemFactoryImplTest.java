package org.limewire.activation.impl;

import java.text.SimpleDateFormat;

import junit.framework.Test;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.io.InvalidDataException;
import org.limewire.util.BaseTestCase;

/**
 * Unit test for {@link org.limewire.activation.impl.ActivationItemFactory}
 * TODO: test loading from disk with expired time
 */
public class ActivationItemFactoryImplTest extends BaseTestCase {

    private ActivationResponseFactory parser;
    
    public ActivationItemFactoryImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ActivationItemFactoryImplTest.class);
    }

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
                "        \"id\":1,\n" +
                "        \"name\":\"Turbo-charged downloads\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20101001\",\n" +
                "        \"status\":active\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\":2,\n" +
                "        \"name\":\"Optimized Search Results\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20101231\",\n" +
                "        \"status\":active\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\":3,\n" +
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
        assertEquals(1440, response.getRefreshIntervalInMinutes());
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
        assertEquals(1440, response.getRefreshIntervalInMinutes());
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
        assertEquals(1440, response.getRefreshIntervalInMinutes());
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
    
    // test "notfound" json response
    public void testIdNotInActivationServer() throws Exception {
        String json = "{\"lid\":\"DAVV-XXME-BWU3\",\"response\":\"notfound\",\"refresh\":0,\"message\":\"ID not found\"}";
        ActivationResponse response = parser.createFromJson(json);
        assertEquals(ActivationResponse.Type.NOTFOUND, response.getResponseType());
        assertEquals(0, response.getActivationItems().size());
        assertEquals("DAVV-XXME-BWU3", response.getLid());
    }
    
    // test "blocked" json response
    public void testIdBlockedForAbuse() throws Exception {
        String json = "{\"response\":\"blocked\",\"lid\":\"DA6QCXY96HN2\",\"guid\":\"587D340C9C51080E5C16BE65EBF60F92\",\"duration\":\"0.001526\",\"installations\":5}";   
        ActivationResponse response = parser.createFromJson(json);
        assertEquals(ActivationResponse.Type.BLOCKED, response.getResponseType());
        assertEquals(0, response.getActivationItems().size());
        assertEquals("DA6QCXY96HN2", response.getLid());
    }
    
    // test the "stop" json response, which signals to the LW client
    // to no longer contact the activation server upon startup.
    public void testStopResponse() throws Exception {
        String json = "{\"response\":\"stop\",\"lid\":\"XUFYT6Z6R292\"," +
                      "\"guid\":\"44444444444444444444444444444444\",\"refresh\":0," +
                      "\"mcode\":\"sdgdfgfg\",\"duration\":\"0.001717\"}";
        
        ActivationResponse response = parser.createFromJson(json);
        assertEquals(ActivationResponse.Type.STOP, response.getResponseType());
        assertEquals(0, response.getActivationItems().size());
        assertEquals("XUFYT6Z6R292", response.getLid());
    }
    
    // test the "error" json response
    public void testErrorResponse() throws Exception {
        String json = "{\"response\":\"error\",\"lid\":\"dfgdfgdfg\",\"message\":" +
                       "\"Invalid 'lid'\",\"duration\":\"0.001219\"}";
        
        ActivationResponse response = parser.createFromJson(json);
        assertEquals(ActivationResponse.Type.ERROR, response.getResponseType());
        assertEquals(0, response.getActivationItems().size());
        assertEquals("Invalid 'lid'", response.getMessage());
        assertEquals("dfgdfgdfg", response.getLid());
        assertNull(response.getMCode());
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
    
    // test parsing, loaded from disk, some items expired
    // expired items should have expired statuses
    //
    // Compare with parsing not-from-disk, from disk results
    // which are expired should differ with server statuses
    //
    public void testLoadedFromDiskNoItemsExpired() throws Exception {
        String json = "{\n" +
                "  \"lid\":\"DAVV-XXME-BWU3\",\n" +
                "  \"response\":\"valid\",\n" +
                "  \"mcode\":\"0pd15.1xM6.2xM6.3xM6\",\n" +
                "  \"refresh\":1440,\n" +
                "  \"modules\":\n" +
                "    [\n" +
                "      {\n" +
                "        \"id\":1,\n" +
                "        \"name\":\"Turbo-charged downloads\",\n" +
                "        \"pur\":\"19711001\",\n" +
                "        \"exp\":\"19750903\",\n" +
                "        \"status\":active\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\":2,\n" +
                "        \"name\":\"Optimized Search Results\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20491231\",\n" +
                "        \"status\":active\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\":3,\n" +
                "        \"name\":\"Tech support\",\n" +
                "        \"pur\":\"20091001\",\n" +
                "        \"exp\":\"20501001\",\n" +
                "        \"status\":unavailable\n" +
                "      }\n" +
                "    ]\n" +
                "}";

        ActivationResponse responseFromDisk = parser.createFromDiskJson(json);
        ActivationResponse responseNotFromDisk = parser.createFromJson(json);

        assertEquals("DAVV-XXME-BWU3", responseFromDisk.getLid());
        assertTrue(responseFromDisk.isValidResponse());
        assertEquals("0pd15.1xM6.2xM6.3xM6", responseFromDisk.getMCode());
        assertEquals(1440, responseFromDisk.getRefreshIntervalInMinutes());
        assertEquals(3, responseFromDisk.getActivationItems().size());

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        ActivationItem item = responseFromDisk.getActivationItems().get(0);
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, item.getModuleID());
        assertEquals("Turbo-charged downloads", item.getLicenseName());
        assertEquals(ActivationItem.Status.EXPIRED, item.getStatus());
        assertEquals("19711001", format.format(item.getDatePurchased()));
        assertEquals("19750903", format.format(item.getDateExpired()));
        
        // parsing not-from-disk should say this module (Turbo-charged downloads) is active
        ActivationItem itemNotFromDisk = responseNotFromDisk.getActivationItems().get(0);
        assertNotEquals(itemNotFromDisk, item);
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, itemNotFromDisk.getModuleID());
        assertEquals("Turbo-charged downloads", itemNotFromDisk.getLicenseName());
        assertEquals(ActivationItem.Status.ACTIVE, itemNotFromDisk.getStatus());
        assertEquals("19711001", format.format(itemNotFromDisk.getDatePurchased()));
        assertEquals("19750903", format.format(itemNotFromDisk.getDateExpired()));

        item = responseFromDisk.getActivationItems().get(1);
        assertEquals(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE, item.getModuleID());
        assertEquals("Optimized Search Results", item.getLicenseName());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20491231", format.format(item.getDateExpired()));
        
        // parsing not-from-disk should be the same as from disk
        itemNotFromDisk = responseNotFromDisk.getActivationItems().get(1);
        assertEquals(itemNotFromDisk, item);

        item = responseFromDisk.getActivationItems().get(2);
        assertEquals(ActivationID.TECH_SUPPORT_MODULE, item.getModuleID());
        assertEquals("Tech support", item.getLicenseName());
        assertEquals(ActivationItem.Status.UNAVAILABLE, item.getStatus());
        assertEquals("20091001", format.format(item.getDatePurchased()));
        assertEquals("20501001", format.format(item.getDateExpired()));
        
        // parsing not-from-disk should be the same as from disk
        itemNotFromDisk = responseNotFromDisk.getActivationItems().get(2);
        assertEquals(itemNotFromDisk, item);
    }
    
}
