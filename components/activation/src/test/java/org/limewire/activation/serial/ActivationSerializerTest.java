package org.limewire.activation.serial;

import java.io.File;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.CipherProviderImpl;

public class ActivationSerializerTest extends LimeTestCase {

    public ActivationSerializerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActivationSerializerTest.class);
    }
    
    public void testLoadAndSaveEmptyList() throws Exception {
        File tmp = getFile();
        
        ActivationSerializerSettings settings  = new ActivationSerializerSettingsStub(tmp, tmp);
        CipherProvider cipherProvider = new CipherProviderImpl();
        ActivationSerializer serializer = new ActivationSerializerImpl(settings, cipherProvider);
        serializer.writeToDisk("");
        
        String json = serializer.readFromDisk();
        assertEquals("", json);
    }
    
    public void testLoadAndSaveSingular() throws Exception {
        String jsonString = "{\"lid\":\"test\",\"response\":\"valid\",\"mcode\":\"4pd15\",\"guid\":\"44444444444444444444444444444444\",\"refresh\":1440,\"modulecount\":2,\"modules\":[{\"id\":0,\"name\":\"Turbo-charged downloads\",\"pur\":\"20091001\",\"exp\":\"20101001\",\"status\":\"active\"},{\"id\":0,\"name\":\"Optimized search results\",\"pur\":\"20091001\",\"exp\":\"20101001\",\"status\":\"expired\"}]}";
        File tmp = getFile();
        
        ActivationSerializerSettings settings  = new ActivationSerializerSettingsStub(tmp, tmp);
        CipherProvider cipherProvider = new CipherProviderImpl();
        ActivationSerializer serializer = new ActivationSerializerImpl(settings, cipherProvider);
        
        serializer.writeToDisk(jsonString);
        
        String json = serializer.readFromDisk();
        assertEquals(jsonString, json);
    }
        
    private File getFile() throws Exception {
        File tmp = File.createTempFile("lwc", "save");
        tmp.delete();
        tmp.deleteOnExit();
        
        return tmp;
    }
}
