package com.limegroup.gnutella.mp3;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import com.limegroup.gnutella.gui.mp3.M3UFileFormat;
import com.limegroup.gnutella.util.BaseTestCase;

public class M3UFileFormatTest extends BaseTestCase {

    public M3UFileFormatTest(String name) {
        super(name);
    }

    public void test() throws java.lang.Exception {
        File file = File.createTempFile("test",".m3u");
        List records = new ArrayList();
        records.add(record1());
        records.add(record2());
        M3UFileFormat.store(records,file);			
        List recordsRead = M3UFileFormat.load(file);
        assertEquals(records,recordsRead);
    }

    M3UFileFormat.Record record1() {
        return new M3UFileFormat.Record(123,"Name 1, with embedded comma",new File("/home/tjones/file1.mp3"));
    }

    M3UFileFormat.Record record2() {
        return new M3UFileFormat.Record(234,"Name 2",new File("/home/tjones/file2.mp3"));
    }
}
