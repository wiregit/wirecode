package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.metadata.audio.reader.ASFParser;
import com.limegroup.gnutella.metadata.audio.reader.AudioDataReader;
import com.limegroup.gnutella.metadata.audio.reader.MP3MetaData;
import com.limegroup.gnutella.metadata.audio.reader.WMAMetaData;
import com.limegroup.gnutella.metadata.audio.writer.FlacDataEditor;
import com.limegroup.gnutella.metadata.audio.writer.M4ADataEditor;
import com.limegroup.gnutella.metadata.audio.writer.MP3DataEditor;
import com.limegroup.gnutella.metadata.audio.writer.OGGDataEditor;
import com.limegroup.gnutella.metadata.video.reader.MOVMetaData;
import com.limegroup.gnutella.metadata.video.reader.MPEGMetaData;
import com.limegroup.gnutella.metadata.video.reader.OGMMetaData;
import com.limegroup.gnutella.metadata.video.reader.RIFFMetaData;
import com.limegroup.gnutella.metadata.video.reader.WMVMetaData;
import com.limegroup.gnutella.xml.LimeXMLUtils;

public class MetaDataFactory {
    
    private MetaDataFactory(){}

    private static final Log LOG = LogFactory.getLog(MetaDataFactory.class); 
    
    /**
     * factory method which returns an instance of MetaDataEditor which
     * should be used with the specific file
     * @param name the name of the file to be annotated
     * @return the MetaDataEditor that will do the annotation.  null if the
     * lime xml repository should be used.
     */
    public static MetaWriter getEditorForFile(String name) {
        if (LimeXMLUtils.isSupportedAudioFormat(name))
            return getAudioEditorForFile(name);
        //add video types here
        return null;
        
    }
    
    /** Creates MetaData for the file, if possible. */  
    public static MetaReader parse(File f) throws IOException {
        try {
            if (LimeXMLUtils.isSupportedAudioFormat(f))
                return parseAudioFile(f);
            else if (LimeXMLUtils.isSupportedVideoFormat(f))
                return parseVideoMetaData(f);
            //TODO: add other media formats here            
            else if (LimeXMLUtils.isSupportedMultipleFormat(f))
                return parseMultipleFormat(f);
        } catch (OutOfMemoryError e) {
            LOG.warn("Ran out of memory while parsing.",e);
        }
        return null;
    }
    
    /** Figures out what kind of MetaData should exist for this file. */
    private static MetaReader parseMultipleFormat(File f) throws IOException {
        if(LimeXMLUtils.isASFFile(f)) {
            ASFParser p = new ASFParser(f);
            if(p.hasVideo())
                return new WMVMetaData(p);
            else if(p.hasAudio())
                return new WMAMetaData(p);
        }
        return null;
    }
    
    /**
     * Factory method for retrieving the correct editor
     * for a given file.
     */
    public static MetaWriter getAudioEditorForFile(String name) {
        if (LimeXMLUtils.isMP3File(name))
            return new MP3DataEditor();
        if (LimeXMLUtils.isOGGFile(name))
            return new OGGDataEditor();
        if (LimeXMLUtils.isM4AFile(name))
            return new M4ADataEditor();
        if (LimeXMLUtils.isFLACFile(name))
            return new FlacDataEditor();
        return null;
    }
    
    public static MetaReader parseAudioFile(File f) throws IOException { 
        if (LimeXMLUtils.isMP3File(f))
            return new MP3MetaData(f);
        if (LimeXMLUtils.isOGGFile(f))
            return new AudioDataReader(f);
        if (LimeXMLUtils.isFLACFile(f))
            return new AudioDataReader(f);
        if (LimeXMLUtils.isM4AFile(f))
            return new AudioDataReader(f);
        if (LimeXMLUtils.isWMAFile(f))
            return new WMAMetaData(f);
        
        return null;
    }
    
    /**
     * Parses video metadata out of the file if this is a known video file.
     * Otherwise returns null.
     */
    public static MetaReader parseVideoMetaData(File file)
            throws IOException {
        if (LimeXMLUtils.isRIFFFile(file))
            return new RIFFMetaData(file);
        else if (LimeXMLUtils.isOGMFile(file))
            return new OGMMetaData(file);
        else if(LimeXMLUtils.isWMVFile(file))
            return new WMVMetaData(file);
        else if(LimeXMLUtils.isMPEGFile(file))
            return new MPEGMetaData(file);
        else if (LimeXMLUtils.isQuickTimeFile(file))
            return new MOVMetaData(file);
            
        return null;
    }
}
