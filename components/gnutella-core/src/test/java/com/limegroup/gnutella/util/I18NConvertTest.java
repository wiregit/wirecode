package com.limegroup.gnutella.util;

import junit.framework.*;
import java.lang.reflect.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class I18NConvertTest extends BaseTestCase {

    private BufferedReader _buf;
    private boolean _last = false;
    private final String DELIM = ";";
    
    private final String CASE = "CASE";
    private final String ACCENTS = "ACCENTS";
    private final String OTHER = "OTHER";
    private final String SPLIT = "KEYWORD_SPLIT";
    private I18NConvert _instance;

    public I18NConvertTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(I18NConvertTest.class);
    }

    //runs before the test.
    public void setUp() throws Exception {
        //read in a file and do the tests from there.
        FileInputStream fin = 
            new FileInputStream
            (new File("com/limegroup/gnutella/util/i18ntest.txt"));
        _buf = new BufferedReader(new InputStreamReader(fin, "UTF-8"));

        _instance = I18NConvert.instance();
    }
    
    public void testConversions() {
        readLines(CASE,0);
        readLines(ACCENTS,0);
        _last = true;
        readLines(SPLIT,1);
    }

    private void readLines(String what, int which) {
        String line = "";
        try {
            while((line = _buf.readLine()) != null &&
                  !line.equals("# END " + what)) {
                doNorm(line, what, which);
            }
        }
        catch(IOException ioe) {
            fail("problem with i18ntest.txt file", ioe);
        }
        catch(Throwable t) {
            fail("proba at line : " + line, t);
        }

    }

    private void doNorm(String line, final String what, int which) 
        throws Throwable {

        if(line.indexOf("# ") == -1) {
            String[] split = StringUtils.split(line, DELIM);
            String x = getKC(getDK(split[1]));
            if(which == 1)
                x = _instance.getNorm(split[1]);

            assertEquals(what + " " + line + ":", 
                         split[0], 
                         x);
        }
    }
    
    public void tearDown() throws Exception {
        System.out.println("tear down");
        if(_last && _buf != null)
            _buf.close();
    }

    
    private final String getDK(String s) 
        throws Throwable { 
        
        try {
            return ((String)
                    PrivilegedAccessor.invokeMethod(_instance,
                                                    "getDK",
                                                    new Object[] {s},
                                                    new Class[] {String.class}));
        }
        catch(Exception e) {
            if(e.getCause() != null)
                throw e.getCause();
            throw e;
        }
    }

    private final String getKC(String s) 
        throws Throwable { 

        return ((String)
                PrivilegedAccessor.invokeMethod(_instance,
                                               "getKC",
                                               new Object[] {s},
                                               new Class[] {String.class}));
    }

}





