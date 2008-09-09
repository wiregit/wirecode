package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.metadata.audio.reader.AudioDataReader;
import com.limegroup.gnutella.metadata.audio.reader.MP3MetaData;
import com.limegroup.gnutella.metadata.audio.reader.OGGMetaData;
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

/**
 * Implementation of MetaDataFactory. Returns the appropriate reader/writer for
 * the file type if one exists, null if one does not exist 
 */
@Singleton
public class MetaDataFactoryImpl implements MetaDataFactory {

    private static final Log LOG = LogFactory.getLog(MetaDataFactory.class);
    
    private final ConcurrentMap<String, MetaReader> readerByExtension = new ConcurrentHashMap<String, MetaReader>();
    
    public MetaDataFactoryImpl() {
        WMMetaReader metaReader = new WMMetaReader();
        registerReader(metaReader, metaReader.getSupportedExtensions());
    }
    
    /**
     * factory method which returns an instance of MetaDataEditor which
     * should be used with the specific file
     * @param name the name of the file to be annotated
     * @return the MetaDataEditor that will do the annotation.  null if the
     * lime xml repository should be used.
     */
    public MetaWriter getEditorForFile(String name) {
        if (LimeXMLUtils.isSupportedAudioFormat(name))
            return getAudioEditorForFile(name);
        //add video types here
        return null;
    }

    @Override
    public void registerReader(MetaReader reader, String...fileExtensions) {
        for (String extension : fileExtensions) {
            MetaReader existingReader= readerByExtension.put(extension, reader);
            if (existingReader != null) {
                throw new IllegalArgumentException("factory: " + existingReader.getClass() + " already resistered for: " + extension);
            }
        }
    }
    
    public MetaData parse(File file) throws IOException {
        try {
            MetaReader reader = getMetaReader(file);
            if (reader != null) {
                return reader.parse(file);
            }
        } catch (OutOfMemoryError e) {
            LOG.warn("Ran out of memory while parsing.",e);
            throw (IOException)new IOException().initCause(e);
        } catch (Exception e) {
            LOG.warn("Exception parsing file.",e);
            throw (IOException)new IOException().initCause(e);
        }
        return null;
    }
    
    /** Creates MetaData for the file, if possible. */  
    public MetaReader getMetaReader(File f) {
        if (LimeXMLUtils.isSupportedAudioFormat(f))
            return getAudioReader(f);
        else if (LimeXMLUtils.isSupportedVideoFormat(f))
            return getVideoReader(f);         
        else {
            String extension = FileUtils.getFileExtension(f);
            if (extension != null) {
                MetaReader reader = readerByExtension.get(extension.toLowerCase(Locale.US));
                return reader;
            }
        }
        return null;
    }
    
    /**
     * Returns the audio editor for the file if an editor exists
     * for that file type. Returns null if no editor exists
     */
    private MetaWriter getAudioEditorForFile(String name) {
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
    
    /**
     * Reads the meta data for the audio file if LimeWire can parse
     * it, otherwise return null if file type is not supported
     */
    private MetaReader getAudioReader(File f) {
        if (LimeXMLUtils.isMP3File(f))
            return new MP3MetaData();
        if (LimeXMLUtils.isOGGFile(f))
            return new OGGMetaData();
        if (LimeXMLUtils.isFLACFile(f))
            return new AudioDataReader();
        if (LimeXMLUtils.isM4AFile(f))
            return new AudioDataReader();
        if (LimeXMLUtils.isWMAFile(f))
            return new WMAMetaData();
        
        return null;
    }
    
    /**
     * Reads the meta data for the video file if LimeWire can parse
     * it, otherwise return null if the file type is not suported
     */
    private MetaReader getVideoReader(File file) {
        if (LimeXMLUtils.isRIFFFile(file))
            return new RIFFMetaData();
        else if (LimeXMLUtils.isOGMFile(file))
            return new OGMMetaData();
        else if(LimeXMLUtils.isWMVFile(file))
            return new WMVMetaData();
        else if(LimeXMLUtils.isMPEGFile(file))
            return new MPEGMetaData();
        else if (LimeXMLUtils.isQuickTimeFile(file))
            return new MOVMetaData();
            
        return null;
    }
}
