package com.limegroup.gnutella.metadata;

import java.io.File;

import junit.framework.Test;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Tests Ogg meta-data reader
 */
public class OGGReaderTest extends AudioTest {

    public OGGReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OGGReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testOgg() throws Exception {
        File file = CommonUtils.getResourceFile(dir+"oggAll.ogg");
        assertTrue("file should exist", file.exists());
        
        MetaReader data = MetaDataFactory.parse(file);
        testTag((AudioMetaData) data.getMetaData());
    }
    
    public void testNoFieldsOgg() throws Exception {
        File file = CommonUtils.getResourceFile(dir+"oggNone.ogg");
        assertTrue("file should exist", file.exists());
        
        MetaReader data = MetaDataFactory.parse(file);
        AudioMetaData amd = (AudioMetaData) data.getMetaData();
        assertEquals("", amd.getTitle());
        assertEquals("", amd.getArtist());
        assertEquals("", amd.getAlbum());
        assertEquals("", amd.getComment());
        assertEquals("", amd.getTrack());
        assertEquals("", amd.getYear());
        assertEquals("", amd.getGenre());
    }
}