package org.limewire.core.impl.util;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class FilePropertyKeyPopulatorTest extends BaseTestCase {
    public FilePropertyKeyPopulatorTest(String name) {
        super(name);
    }

    public void testAudioFilePopulation() throws Exception {
        final String fileName = "testFileName.mp3";
        final Long fileSize = new Long(1234);
        final Long creationTime = new Long(5678);
        final String title = "Hello World";
        final String artist = "Me and you";
        final String album = "Testing the waters";
        final String genre = "Rock";
        final String comments = "woah!";
        final Long bitrate = new Long(128);
        final Long seconds = new Long(956);
        final Long year = new Long(1999);
        final Long track = new Long(5);
        final Long quality = new Long(2);

        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.AUDIO_ALBUM);
                will(returnValue(album.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_ARTIST);
                will(returnValue(artist.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_BITRATE);
                will(returnValue(bitrate.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_COMMENTS);
                will(returnValue(comments.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_GENRE);
                will(returnValue(genre.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_TRACK);
                will(returnValue(track.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_YEAR);
                will(returnValue(year.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_SECONDS);
                will(returnValue(seconds.toString()));
            }
        });

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator
                .populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(artist, map.get(FilePropertyKey.AUTHOR));
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
