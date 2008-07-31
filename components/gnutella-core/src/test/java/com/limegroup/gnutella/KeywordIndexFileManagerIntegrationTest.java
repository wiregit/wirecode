package com.limegroup.gnutella;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.util.FileManagerTestUtils;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SearchSettings;

/**
 * Integration tests for query handling
 */
public class KeywordIndexFileManagerIntegrationTest extends FileManagerTestCase {



    public KeywordIndexFileManagerIntegrationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(KeywordIndexFileManagerIntegrationTest.class);
    }

    /**
     * Test plaintext searching to ensure that shared files' metadata is searched
     *
     */
    public void testMetadataResultsForPlaintextQuery() throws Exception {
        waitForLoad();

        // test a query where the filename is meaningless but XML matches.
        File f1 = createNewNamedTestFile(10, "meaningless");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
                "artist=\"Sammy B\" album=\"Jazz in A minor\" genre=\"mean\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileManagerEvent result = addIfShared(f1, l1);
        assertTrue(result.toString(), result.isAddEvent());
        assertEquals(d1, result.getNewFileDesc().getLimeXMLDocuments().get(0));

        // test exact match of keywords in metadata
        Response[] responses = keywordIndex.query(queryRequestFactory.createRequery("Sammy B"));

        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // test exact match on album in metadata
        responses = keywordIndex.query(queryRequestFactory.createQuery("Jazz in A minor"));

        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // test searching metadata fields with prefixes
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sam"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        responses = keywordIndex.query(queryRequestFactory.createQuery("Ja"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // test searching metadata fields with prefixes of varying case
        responses = keywordIndex.query(queryRequestFactory.createQuery("sAm"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        responses = keywordIndex.query(queryRequestFactory.createQuery("jaZZ In"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // not matching: prefix
        responses = keywordIndex.query(queryRequestFactory.createQuery("sammm"));
        assertEquals(0, responses.length);

        // not matching: correct part but in string
        responses = keywordIndex.query(queryRequestFactory.createQuery("in"));
        assertEquals(0, responses.length);

        // not matching: String contains full metadata but has additional text
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy B XYZ"));
        assertEquals(0, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("minor"));
        assertEquals(0, responses.length);

        // search something that is in just the file name prefix
        responses = keywordIndex.query(queryRequestFactory.createQuery("meaningless"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // search something that is in both the file name prefix AND the metadata
        // Make sure there is only 1 search result
        responses = keywordIndex.query(queryRequestFactory.createQuery("mean"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // search something that is neither in the file name NOR the metadata
        responses = keywordIndex.query(queryRequestFactory.createQuery("No Matches"));
        assertEquals(0, responses.length);

        // remove file
        fman.removeFile(f1);

        // no more matches
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy B"));
        assertEquals(0, responses.length);

        // test on album
        responses = keywordIndex.query(queryRequestFactory.createQuery("Jazz in A minor"));

        assertEquals(0, responses.length);

        // prefixes
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sam"));
        assertEquals(0, responses.length);
    }

    /**
     * Given the following shared files:
     *
     * #1. An audio file with title "one two three four"
     * #2. A video file with title "one two three four"
     *
     * A query request with query string "one two three four" and
     * media type "audio" should only match file #1.
     *
     */
    public void testMetadataResultsForPlaintextQueryWithSpecificMimeType() throws Exception {
        waitForLoad();

        File f1 = createNewNamedTestFile(10, "audioFile");
        LimeXMLDocument audioMetaData = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
                "artist=\"one two tree\" album=\"Jazz in A minor\" genre=\"mean\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();

        File f2 = createNewNamedTestFile(10, "videoFile");
        LimeXMLDocument videoMetaData = limeXMLDocumentFactory.createLimeXMLDocument(
                FileManagerTestUtils.buildVideoXMLString("director=\"one two tree\" title=\"Four five Six\""));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();

        l1.add(audioMetaData);
        l2.add(videoMetaData);

        addIfShared(f1, l1);
        addIfShared(f2, l2);

        // A query request with query string "one two three four" and
        // media type "audio" should only match the file named "audioFile".
        String queryString = "one two tree";
        QueryRequest queryAudio =
                queryRequestFactory.createQuery(GUID.makeGuid(), queryString, "",
                        MediaType.getAudioMediaType());
        Response[] responses = keywordIndex.query(queryAudio);
        assertEquals(1, responses.length);
        assertEquals(audioMetaData.getXMLString(), responses[0].getDocument().getXMLString());

        // A query request with query string "one two three four" and
        // media type "video" should only match the file named "videoFile".
        QueryRequest queryVideo =
                queryRequestFactory.createQuery(GUID.makeGuid(), queryString, "",
                        MediaType.getVideoMediaType());
        responses = keywordIndex.query(queryVideo);
        assertEquals(1, responses.length);
        assertEquals(videoMetaData.getXMLString(), responses[0].getDocument().getXMLString());

        // A query request with query string "one two three four" and
        // media type "video" should only match the file named "videoFile".
        QueryRequest queryPrograms =
                queryRequestFactory.createQuery(GUID.makeGuid(), queryString, "",
                        MediaType.getProgramMediaType());
        responses = keywordIndex.query(queryPrograms);
        assertEquals(0, responses.length);
    }

    /**
     * Test that if the search setting "INCLUDE_METADATA_IN_PLAINTEXT_SEARCH" is set to false,
     * plaintext queries that match the metadata of shared files will not show up as results.
     *
     */
    public void testMetadataResultsForPlaintextQueryWithSearchSettingTurnedOff() throws Exception {
        waitForLoad();

        // test a query where the filename is meaningless but XML matches.
        File f1 = createNewNamedTestFile(10, "meaningless");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
                "artist=\"Sammy B\" album=\"Jazz in A minor\" genre=\"mean\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        addIfShared(f1, l1);


        assertTrue(SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.getValue());

        SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.setValue(false);

        // test that with the setting set to false, plaintext queries on the metadata
        // do not succeed, but file name searches still succeed

        // file search should succeed
        responses = keywordIndex.query(queryRequestFactory.createQuery("meaningle"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // plaintext search of metadata should fail
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy"));
        assertEquals(0, responses.length);

        // turn the setting back on, and plaintext searches of metadata should succeed.
        SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.setValue(true);

        // file search should still succeed
        responses = keywordIndex.query(queryRequestFactory.createQuery("meaning"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // plaintext search of metadata should succeed
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

    }

    /**
     * Make sure a "What is New" query does not search the meta data,
     * nor should it search file names.  It should only get the 3 files that were most
     * recently shared.
     */
    public void testWhatIsNewQueryDoesNotSearchMetaData() throws Exception {
        waitForLoad();

        // create and add files to file manager with associated metadata
        //

        // This file contains the "whatisnewxoxo" string in the metadata
        addFileWithMetaDataToFileManager("fileShouldNotShowUpInWhat-Is-New.txt", FileManagerTestUtils.buildAudioXMLString(
                                "artist=\"WhatIsNewXOXO\" album=\"Badd in A minor\" genre=\"mean\" "));
        
        addFileWithMetaDataToFileManager("145random", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"should not show up\" album=\"Radd on A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("987abctrees", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"should not show up\" album=\"Faze in A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("search123.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"lll two tree\" album=\"Raze in A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("456search.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"nnn two tree\" album=\"Daze in A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("678attempt.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"ooo two tree\" album=\"Paze in A minor\" genre=\"mean\" "));
        
        QueryRequest query = queryRequestFactory.createWhatIsNewQuery(GUID.makeGuid(), (byte)3);

        // sanity check!
        assertTrue(query.isWhatIsNewRequest());

        responses = keywordIndex.query(query);
        assertEquals(3, responses.length);

        // make sure that none of the "what is new" responses correspond to the
        // older file which contained "whatisnewxoxo" in its metadata
        for (Response response : responses) {
            assertFalse(response.getName().startsWith("fileShouldNotShowUpInWhat-Is-New.txt"));
        }

        // create query with "whatisnewxoxo" as query string, and
        // make sure "fileShouldNotShowUpInWhat-Is-New.txt" shows up as a result.
        responses = keywordIndex.query(queryRequestFactory.createQuery("WhatIsNewXOXO"));
        assertEquals(1, responses.length);
        assertTrue(responses[0].getName().startsWith("fileShouldNotShowUpInWhat-Is-New.txt"));
    }

    /**
     * Make sure the response to a "What is New" query contains metadata.
     */
    public void testWhatsNewSearchResponseContainsMetaData() throws Exception {

        waitForLoad();

        addFileWithMetaDataToFileManager("678attempt.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"ooo two tree\" album=\"Paze in A minor\" genre=\"mean\" "));

        QueryRequest query = queryRequestFactory.createWhatIsNewQuery(GUID.makeGuid(), (byte)3);

        responses = keywordIndex.query(query);
        assertEquals(1, responses.length);

        assertNotNull(responses[0].getDocument());
    }


    private void addFileWithMetaDataToFileManager(String name, String xml) throws Exception {
        File f1 = createNewNamedTestFile(10, name);

        LimeXMLDocument metaData = limeXMLDocumentFactory.createLimeXMLDocument(xml);
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();

        l1.add(metaData);
        addIfShared(f1, l1);
        Thread.sleep(500);
    }

}
