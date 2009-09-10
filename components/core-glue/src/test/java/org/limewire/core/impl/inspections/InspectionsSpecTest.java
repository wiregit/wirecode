package org.limewire.core.impl.inspections;


import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import org.limewire.core.settings.InspectionsSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.InvalidDataException;

/**
 * Test {@link InspectionsSpec} encoding/decoding, wire format to/from object.
 */
public class InspectionsSpecTest extends LimeTestCase {
    

    public InspectionsSpecTest(String name) {
        super(name);
    }
    
    public void testInvalidWireFormatToInspectionsSpecFailure() throws Exception {
        byte[] garbage = "sdjklfhsdkfsdkjfsdgsdjgldfjgkldfjgldfjgdfkgdfkgldjfgdfjgkdfjg".getBytes();

        InspectionsParser parser = new InspectionsParser();
        
        try {
            parser.parseInspectionSpecs(garbage);
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Not in GZIP format", e.getMessage());       
        }
    }
    
    public void testEmptyWireFormatToInspectionsSpecFailure() throws Exception {
        byte[] garbage = new byte[0];
        InspectionsParser parser = new InspectionsParser();
        
        try {
            parser.parseInspectionSpecs(garbage);
            fail("Expected IOException");
        } catch (IOException e) {
            assertInstanceof(EOFException.class, e);       
        }
    }

    public void testWireFormatToInspectionsSpecSuccess() throws Exception {
        InspectionsSettings.INSPECTION_SPEC_MINIMUM_INTERVAL.set(0);
        InspectionsSpec spec = new InspectionsSpec(Arrays.asList("one", "two", "three"), 5, 3);
        InspectionsSpec spec2 = new InspectionsSpec(Arrays.asList("four", "five", "six"), 6, 4);
        byte[] encoded = InspectionsTestUtils.getGzippedAndBencoded(Arrays.asList(spec, spec2));
        
        InspectionsParser parser = new InspectionsParser();
        List<InspectionsSpec> parsedSpecs = parser.parseInspectionSpecs(encoded);
            
        assertEquals(spec.getInitialDelay(), parsedSpecs.get(0).getInitialDelay());
        assertEquals(spec2.getInitialDelay(), parsedSpecs.get(1).getInitialDelay());
        assertEquals(spec.getInterval(), parsedSpecs.get(0).getInterval());
        assertEquals(spec2.getInterval(), parsedSpecs.get(1).getInterval());
        assertEquals(spec.getInspectionPoints(), parsedSpecs.get(0).getInspectionPoints());
        assertEquals(spec2.getInspectionPoints(), parsedSpecs.get(1).getInspectionPoints());
    }
    
    public void testWireFormatContainsRepeatInspectionPoints() throws Exception {
        InspectionsSpec spec = new InspectionsSpec(
                Arrays.asList("one", "three", "two", "three"), 500, 78);
        byte[] encoded = InspectionsTestUtils.getGzippedAndBencoded(Collections.singletonList(spec));
        
        InspectionsParser parser = new InspectionsParser();
        List<InspectionsSpec> parsedSpecs = parser.parseInspectionSpecs(encoded);
        
        assertEquals(1, parsedSpecs.size());
        assertEquals(500, parsedSpecs.get(0).getInitialDelay());
        assertEquals(78, parsedSpecs.get(0).getInterval());
        assertEquals(Arrays.asList("one", "three", "two"), parsedSpecs.get(0).getInspectionPoints());
    }
    
    public void testWireFormatToInspectionsSpecInvalidInterval() throws Exception {
        InspectionsSpec spec = new InspectionsSpec(Arrays.asList("one", "two", "three"), 5, 3);
        byte[] encoded = InspectionsTestUtils.getGzippedAndBencoded(Arrays.asList(spec));
        
        InspectionsParser parser = new InspectionsParser();
        try {
            parser.parseInspectionSpecs(encoded);
            fail();
        } catch (InvalidDataException e) {
            // expected result
        }
    }
}
