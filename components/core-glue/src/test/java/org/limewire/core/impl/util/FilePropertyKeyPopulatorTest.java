package org.limewire.core.impl.util;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public class FilePropertyKeyPopulatorTest extends BaseTestCase {
    public FilePropertyKeyPopulatorTest(String name) {
        super(name);
    }

    public void testAudioFilePopulation() throws Exception {
        String fileName = "testFileName.mp3";
        Long fileSize = new Long(1234);
        Long creationTime = new Long(5678);
        String title = "Hello World";
        String artist = "Me and you";
        String album = "Testing the waters";
        String genre = "Rock";
        String license = "me and you";
        String comments = "woah!";
        Long bitrate = new Long(128);
        Long seconds = new Long(956);
        Long year = new Long(1999);
        Long track = new Long(5);
        Long quality = new Long(2);

        String xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio title=\""
                + title
                + "\" artist=\""
                + artist
                + "\" album=\""
                + album
                + "\" genre=\""
                + genre
                + "\" track=\""
                + track
                + "\" year=\""
                + year
                + "\" seconds=\""
                + seconds
                + "\" bitrate=\""
                + bitrate
                + "\" comments=\""
                + comments
                + "\" license=\""
                + license + "\"/></audios>";

        Injector injector = LimeTestUtils.createInjector();
        LimeXMLDocumentFactory limeXMLDocumentFactory = injector
                .getInstance(LimeXMLDocumentFactory.class);
        LimeXMLDocument document = limeXMLDocumentFactory.createLimeXMLDocument(xml);

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator.populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(title, map.get(FilePropertyKey.TITLE));
        assertEquals(album, map.get(FilePropertyKey.ALBUM));
        assertEquals(genre, map.get(FilePropertyKey.GENRE));
        assertEquals(track, map.get(FilePropertyKey.TRACK_NUMBER));
        assertEquals(year, map.get(FilePropertyKey.YEAR));
        assertEquals(seconds, map.get(FilePropertyKey.LENGTH));
        assertEquals(bitrate, map.get(FilePropertyKey.BITRATE));
        assertEquals(comments, map.get(FilePropertyKey.DESCRIPTION));
        assertEquals(FileUtils.getFilenameNoExtension(fileName), map.get(FilePropertyKey.NAME));
        assertEquals(fileSize, map.get(FilePropertyKey.FILE_SIZE));
        assertEquals(creationTime, map.get(FilePropertyKey.DATE_CREATED));
        assertEquals(quality, map.get(FilePropertyKey.QUALITY));
    }

}
