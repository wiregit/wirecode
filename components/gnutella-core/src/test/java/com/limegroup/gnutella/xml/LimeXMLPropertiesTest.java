package com.limegroup.gnutella.xml;

import junit.framework.*;
import java.lang.reflect.*;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import org.xml.sax.InputSource;
import java.io.*;

public class LimeXMLPropertiesTest extends BaseTestCase {

    private final String sch = "http://www.limewire.com/schemas/";

    public LimeXMLPropertiesTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeXMLPropertiesTest.class);
    }

    LimeXMLProperties _instance;
    
    public void setUp() throws Exception {
        _instance = LimeXMLProperties.instance();
    }


    public void testReadFromDir() {
        try {
            String resourcePath = "/" + _instance.getXMLSchemaDirResource().replace(File.separatorChar, '/');
            File dir = new File(getClass().getResource(resourcePath).getFile());
            InputSource[] inputsources =
                (InputSource[])PrivilegedAccessor.invokeMethod(_instance,
                                                "getFilesFromDir",
                                                new Object[]{dir},
                                                new Class[]{File.class});
            
            assertTrue(inputsources.length > 1);
            checkSchemaURI(inputsources);
        }
        catch(Throwable t) {
            fail(" reading from dir ", t);
        }
    }

    public void testReadFromJar() {
        try {
            InputSource[] inputsources = 
                (InputSource[])PrivilegedAccessor.invokeMethod(_instance,
                                                               "getFilesFromJar",
                                                               new Object[]{},
                                                               new Class[]{});
            
            assertTrue(inputsources.length > 1);
            checkSchemaURI(inputsources);
        }
        catch(Throwable t) {
            fail(" reading from jar ", t);
        }
    }


    private void checkSchemaURI(InputSource[] inputsources) {
        try {
            LimeXMLSchema lx = new LimeXMLSchema(inputsources[0]);
            assertEquals(lx.getSchemaURI(), sch + "audio.xsd");
            
            lx = new LimeXMLSchema(inputsources[1]);
            assertEquals(lx.getSchemaURI(), sch + "video.xsd");
        }
        catch(IOException e) {
            fail("exception in creating LimeXMLSchema from read in inputsources", e);
        }
    }

    
}
 
