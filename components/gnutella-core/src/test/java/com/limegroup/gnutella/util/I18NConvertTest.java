package com.limegroup.gnutella.util;

import junit.framework.*;
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

    public void testCase() {
        readLines(CASE);
    }

    public void testACCENTS() {
        _last = true;
        readLines(ACCENTS);
    }

    
    private void readLines(String what) {
        String line;
        try {
            while((line = _buf.readLine()) != null &&
                  !line.equals("# END " + what)) {
                doNorm(line, what);
            }
        }
        catch(IOException ioe) {
            fail("problem with i18ntest.txt file", ioe);
        }
    }

    private void doNorm(String line, final String what) {
        if(line.indexOf("#") != 0) {
            String[] split = StringUtils.split(line, DELIM);
            assertEquals(what + " " + line + ":", 
                         split[0], 
                         _instance.getNorm(split[1]));
        }
    }
    


    public void tearDown() throws Exception {
        System.out.println("tear down");
        if(_last && _buf != null)
            _buf.close();
    }
}
