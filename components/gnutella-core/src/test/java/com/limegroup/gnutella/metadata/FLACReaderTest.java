package com.limegroup.gnutella.metadata;

import java.io.File;

import junit.framework.Test;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Tests FLAC meta-data reader
 */
public class FLACReaderTest extends AudioTest {
    
    public FLACReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FLACReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFlac() throws Exception {
        File file = CommonUtils.getResourceFile(dir+"FLAC.flac");
        assertTrue("file should exist", file.exists());
        
        MetaReader data = MetaDataFactory.parse(file);
        testTag((AudioMetaData) data.getMetaData());
    }
}