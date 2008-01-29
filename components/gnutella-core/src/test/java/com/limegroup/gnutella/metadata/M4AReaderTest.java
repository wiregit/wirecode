package com.limegroup.gnutella.metadata;

import java.io.File;

import junit.framework.Test;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Tests M4A meta-data reader
 */
public class M4AReaderTest extends AudioTest {

    public M4AReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(M4AReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testM4A() throws Exception {
        File file = CommonUtils.getResourceFile(dir+"M4A.m4a");
        assertTrue("file should exist", file.exists());
        
        MetaReader data = MetaDataFactory.parse(file);
        testTag((AudioMetaData) data.getMetaData());
    }
}

