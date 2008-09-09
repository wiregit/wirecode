package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.metadata.audio.reader.ASFParser;
import com.limegroup.gnutella.metadata.audio.reader.WMAMetaData;
import com.limegroup.gnutella.metadata.video.reader.WMVMetaData;

public class WMMetaReader implements MetaReader {

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "asf", "wm" };
    }
    
    @Override
    public MetaData parse(File file) throws IOException {
        ASFParser p = new ASFParser(file);
        if (p.hasVideo())
            return new WMVMetaData().parse(p);
        else if(p.hasAudio())
            return new WMAMetaData().parse(p);
        else 
            throw new IOException("could not parse file");
    }
}
