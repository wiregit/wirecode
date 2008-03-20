package com.limegroup.gnutella.metadata.audio.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLNames;


/**
 * Sets WMA metadata using the ASF parser.
 */
public class WMAMetaData implements MetaReader {
    
    //private static final Log LOG = LogFactory.getLog(WMAMetaData.class);
    protected final AudioMetaData audioData;
    
    /** Sets WMA data. */
    public WMAMetaData(File f) throws IOException {
        audioData = new AudioMetaData();
        parseFile(f);
    }
    
    /** Constructs a WMAMetadata from a parser. */
    public WMAMetaData(ASFParser p) throws IOException {
        audioData = new AudioMetaData();
        set(p);
    }
    
    /** Parse using the ASF Parser. */
    private void parseFile(File f) throws IOException {
        ASFParser data = new ASFParser(f);
        set(data);
    }
    
    /** Sets data based on an ASF Parser. */
    private void set(ASFParser data) throws IOException {
        if(data.hasVideo())
            throw new IOException("use WMV instead!");
        if(!data.hasAudio())
            throw new IOException("no audio data!");
            
        audioData.setTitle(data.getTitle());
        audioData.setAlbum(data.getAlbum());
        audioData.setArtist(data.getArtist());
        audioData.setYear(data.getYear());
        audioData.setComment(data.getComment());
        audioData.setTrack(String.valueOf(data.getTrack()));
        audioData.setBitrate(data.getBitrate());
        audioData.setLength(data.getLength());
        audioData.setGenre(data.getGenre());
        audioData.setLicense(data.getCopyright());
        
        if(data.getLicenseInfo() != null)
            audioData.setLicenseType(data.getLicenseInfo());
    }

    public MetaData getMetaData() {
        return audioData;
    }

    /**
     * @return the XML schema for this file
     */
    public String getSchemaURI() {
        return LimeXMLNames.AUDIO_SCHEMA;
    }

    public List<NameValue<String>> toNameValueList() {
        return audioData.toNameValueList();
    }
}
