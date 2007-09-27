package com.limegroup.gnutella.util;


import java.io.File;
import java.io.PrintStream;
import java.util.StringTokenizer;

import junit.framework.Test;

import org.limewire.inspection.Inspector;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;

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
        
        PrintStream out = System.out;
        PrintStream err = System.err;
        
        File props = new File("props");
        System.setOut(new PrintStream(props));
        System.setErr(System.out);
        try {
            while (st.hasMoreTokens()) {
                InspectionTool.main(new String[]{st.nextToken()});
            }
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
        
        Injector i = LimeTestUtils.createInjector();
        Inspector inspector = i.getInstance(Inspector.class);
        inspector.load(props);
        assertTrue(inspector.loaded());
    }
}
