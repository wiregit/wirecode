package org.limewire.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.Test;

import org.limewire.inspection.InspectionTool;
import org.limewire.ui.swing.LimeWireModule;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;

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

        Injector injector = Guice.createInjector(new LimeWireModule()); 
        Map<String, String> results = new HashMap<String, String>();
        int checked = 0;
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if(!next.endsWith("build" + File.separator + "classes")) {
                continue;
            }
            checked++;
            results.putAll(InspectionTool.generateMappings(new File(next), injector, new String[0]));
        }
        assertFalse(results.isEmpty());
        assertGreaterThan(10, checked);
    }
}

