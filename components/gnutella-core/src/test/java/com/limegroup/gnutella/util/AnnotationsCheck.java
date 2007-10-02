package com.limegroup.gnutella.util;


import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.Test;

import org.limewire.inspection.InspectionTool;
import org.limewire.util.BaseTestCase;

public class AnnotationsCheck extends BaseTestCase {

    public AnnotationsCheck(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AnnotationsCheck.class);
    }
    
    public void testAnnotations() throws Exception {
        String classPath = System.getProperty("java.class.path",".");

        StringTokenizer st = new StringTokenizer(classPath, 
          System.getProperty("path.separator"));
        
        
        Map<String, String> results = new HashMap<String, String>();
        while (st.hasMoreTokens()) 
            results.putAll(InspectionTool.generateMappings(new File(st.nextToken())));
        assertFalse(results.isEmpty());
    }
}
