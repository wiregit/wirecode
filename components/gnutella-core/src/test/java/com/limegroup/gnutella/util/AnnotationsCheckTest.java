package com.limegroup.gnutella.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.Test;

import org.limewire.inspection.InspectionTool;
import org.limewire.util.BaseTestCase;

public class AnnotationsCheckTest extends BaseTestCase {

    public AnnotationsCheckTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AnnotationsCheckTest.class);
    }

    public void testAnnotations() throws Exception {
        String classPath = System.getProperty("java.class.path", ".");

        StringTokenizer st = new StringTokenizer(classPath, System.getProperty("path.separator"));

        Map<String, String> results = new HashMap<String, String>();
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            // Ignore test directories..
            if(next.endsWith(File.separator + "tests")) {
                continue;
            }
            results.putAll(InspectionTool.generateMappings(new File(next)));
        }
        assertFalse(results.isEmpty());
    }
}
