package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.limegroup.gnutella.downloader.LWSIntegrationServices.Info;
import com.limegroup.gnutella.util.LimeWireUtils;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests the <code>GetInfo</code> command.
 */
public class GetInfoTest extends AbstractCommunicationSupportWithNoLocalServer {

    public GetInfoTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(GetInfoTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testGetInfo() {

        // This is going to be of the form
        // ( <name> '=' <value> '\t' )*
        String res = sendCommandToClient("GetInfo", EMPTY_ARGS);

        // Find all the key-value pairs in the response
        Map<String, String> propertiesInResponse = new HashMap<String, String>();
        for (String pair: res.split(Pattern.quote("\t"))) {
            String[] parts = pair.split("=");
            assert(parts.length == 2);
            propertiesInResponse.put(parts[0], parts[1]);
        }

        // Here are the key-value pairs we expect.
        Map<Info, String> expectedProperties = new HashMap<Info, String>() { {
                put(Info.IsAlphaRelease, String.valueOf(LimeWireUtils.isAlphaRelease()));
                put(Info.MinorVersionNumber, String.valueOf(LimeWireUtils.getMinorVersionNumber()));
                put(Info.IsPro, String.valueOf(LimeWireUtils.isPro()));
                put(Info.MajorVersionNumber, String.valueOf(LimeWireUtils.getMajorVersionNumber()));
                put(Info.ServiceVersionNumber, String.valueOf(LimeWireUtils.getServiceVersionNumber()));
                put(Info.IsBetaRelease, String.valueOf(LimeWireUtils.isBetaRelease()));
                put(Info.Vendor, String.valueOf(LimeWireUtils.getVendor()));
                put(Info.Version, String.valueOf(LimeWireUtils.getLimeWireVersion()));
                put(Info.IsTestingVersion, String.valueOf(LimeWireUtils.isTestingVersion()));
            }};

        assert (expectedProperties.size() == Info.values().length) :
            "not testing all properties";
        assert (propertiesInResponse.size() == Info.values().length) :
            "response does not contain all properties";

        for (Info key: expectedProperties.keySet()) {
            String expectedValue = expectedProperties.get(key);
            String actualValue = propertiesInResponse.get(key.name());
            assert (expectedValue.equals(actualValue)) : 
                "key: " + key.name() +
                " expected value: " + actualValue +
                " actual value: " + expectedValue;
        }
    }
}
