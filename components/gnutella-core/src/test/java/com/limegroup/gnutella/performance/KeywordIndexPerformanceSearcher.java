package com.limegroup.gnutella.performance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.SharingSettings;
import org.limewire.listener.EventListener;

import com.google.inject.Injector;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * Class to measure latency searching on shared files
 */
public class KeywordIndexPerformanceSearcher {

    private static final Log LOG = LogFactory.getLog(KeywordIndexPerformanceSearcher.class);

    private Injector injector;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private QueryRequestFactory queryRequestFactory;
    private SharedFilesKeywordIndex keywordIndex;

    private int numberOfFilesIndexed;
    private String rawDataFileName;
    private String sharedFilesDirectory;
    private int numSearchesPerSearchTerm;
    private String[] searchTerms;

    private List<QueryLatency> searchResults;


    KeywordIndexPerformanceSearcher(SearchConfig config) {
        this.numberOfFilesIndexed = config.getNumberOfFilesIndexed();
        this.rawDataFileName = config.getRawDataFileName();
        this.numSearchesPerSearchTerm = config.getNumberOfSearchesToPerform();
        this.sharedFilesDirectory = config.getSharedFilesDirectory();
        this.searchTerms = config.getSearchTerms();

        this.injector = LimeTestUtils.createInjector();
        this.keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        this.limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        this.queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }

    public void execute() throws KeywordIndexPerformanceException {

        setupSharedFiles();

        performSearches();
    }

    public List<QueryLatency> getSearchLatencies() {

        // TODO: accessor to get search latency results.  One day we might want the flexibility to create
        // KeywordIndexPerformanceAnalyzer (or to generalize, any  statistical analyzer) by
        // passing in a list of results instead of having to read from a file.
        return searchResults;
    }

    
    private void setupSharedFiles() throws KeywordIndexPerformanceException {

        SharingSettings.EXTENSIONS_TO_SHARE.setValue("abc");
        FileManager fman = injector.getInstance(FileManager.class);
        keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        CreationTimeCache creationTimeCache = injector.getInstance(CreationTimeCache.class);
        SchemaReplyCollectionMapper schemaMapper = injector.getInstance(SchemaReplyCollectionMapper.class);
        NumFilesAddedListener numFilesListener = new NumFilesAddedListener(numberOfFilesIndexed);

        fman.addFileEventListener(keywordIndex);
        fman.addFileEventListener(numFilesListener);
        fman.addFileEventListener(schemaMapper);
        fman.addFileEventListener(creationTimeCache);

        CommonWords commonWords;
        try {
            commonWords = new CommonWords();
        } catch (IOException e) {
            throw new KeywordIndexPerformanceException("Error initializing words for " +
                    "creating file metadata", e);
        }

        for (int i=0; i<numberOfFilesIndexed; i++) {

            String fileName = sharedFilesDirectory + "/" + i + " " + commonWords.getNextWord() +
                                " " + commonWords.getNextWord() + " " + commonWords.getNextWord() + ".abc";

            LimeXMLDocument limeXmlDoc = createLimeXmlDocument(commonWords);
            File sharedFile = createTempSharedFileWithContent(fileName, limeXmlDoc.getXMLString());

            fman.addSharedFileAlways(sharedFile, Collections.singletonList(limeXmlDoc));
        }
        boolean success = numFilesListener.awaitFinish(8);

        if (!success) {
            throw new KeywordIndexPerformanceException("Error adding " + numberOfFilesIndexed + " files to FileManager: ");
        }

        FileList list = fman.getGnutellaSharedFileList();
        LOG.info(list.size() + " files have been successfully added to the File Manager.");
    }

    private static File createTempSharedFileWithContent(String fileName,
                                                        String content) throws KeywordIndexPerformanceException {
        File tmp;
        try {
            tmp = new File(fileName);
            tmp.deleteOnExit();
            BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new KeywordIndexPerformanceException("Error creating file for sharing with File Manager", e);
        }
        return tmp;
    }


    private LimeXMLDocument createLimeXmlDocument(CommonWords commonWords)
    throws KeywordIndexPerformanceException {

        // pick template
        String limeXmlStr = renderTemplate(TemplateManager.getNextTemplate(), commonWords);
        LOG.debug("XML DOC STRING: " + limeXmlStr);

        LimeXMLDocument doc;
        try {
            doc = limeXMLDocumentFactory.createLimeXMLDocument(limeXmlStr);
        } catch (Exception e) {
            throw new KeywordIndexPerformanceException("Error creating metadata / Lime XML Document");
        }
        return doc;
    }



    private static String renderTemplate(String template, CommonWords common) {

        StringBuffer tpl = new StringBuffer(template);
        int indexOfStrToReplace;
        while ((indexOfStrToReplace = tpl.lastIndexOf("REPLACE")) != -1) {
            tpl.replace(indexOfStrToReplace, indexOfStrToReplace+7, common.getNextWord());
        }
        return tpl.toString();
    }

    private void performSearches() throws KeywordIndexPerformanceException {
        searchResults = new ArrayList<QueryLatency>();
        for (int currentSearch=0; currentSearch < numSearchesPerSearchTerm; currentSearch++) {
            for (String searchTerm : searchTerms) {
                QueryLatency searchLatency = performTimedSearch(searchTerm);
                searchResults.add(searchLatency);
            }
        }

        try {
            writeSearchResultsToFile();
        } catch (IOException e) {
            throw new KeywordIndexPerformanceException("Error writing search results to file", e);
        }
    }

    private QueryLatency performTimedSearch(String searchTerm) {
        QueryRequest queryRequest = queryRequestFactory.createQuery(searchTerm);

        long before = System.nanoTime();
        keywordIndex.query(queryRequest);
        long after = System.nanoTime();

        return new QueryLatency(searchTerm, after - before);
    }

    private void writeSearchResultsToFile() throws IOException {
        Writer writer = new FileWriter(rawDataFileName, false);

        for (QueryLatency latency : searchResults) {
            writer.write("query:" + latency.queryString +
                         ":" + Long.toString(latency.searchLatency) + "\n");
        }
        writer.close();
    }

    /**
     * latency for each query. Responses of the query are not recorded.
     */
    static class QueryLatency {

        String queryString;
        long searchLatency;

        QueryLatency(String queryString, long searchLatency) {
            this.queryString = queryString;
            this.searchLatency = searchLatency;
        }
    }


    /**
     * Round robin lime xml template fetching.  This might be better off read from a file.
     */
    private static class TemplateManager {


        private static final String[] templates = {
                "<?xml version=\"1.0\"?>" +
                        "<images xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/image.xsd\">" +
                            "<image title=\"REPLACE REPLACE\" description=\"REPLACE REPLACE REPLACE REPLACE\"" +
                                " artist=\"REPLACE REPLACE REPLACE\"" +
                                " licenseType=\"REPLACE REPLACE\"" +
                                " license=\"REPLACE REPLACE\"/>" +
                        "</images>",

                "<?xml version=\"1.0\"?>" +
                        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">" +
                            "<audio title=\"REPLACE REPLACE REPLACE\" artist=\"REPLACE REPLACE\" " +
                                "album=\"REPLACE REPLACE REPLACE\" " +
                                "track=\"REPLACE\" " +
                                "genre=\"REPLACE REPLACE\" " +
                                "price=\"REPLACE\" " +
                                "type=\"REPLACE\" " +
                                "action=\"REPLACE REPLACE REPLACE\" " +
                                "seconds=\"REPLACE REPLACE\" " +
                                "bitrate=\"REPLACE\" " +
                                "comments=\"REPLACE REPLACE REPLACE REPLACE REPLACE\" />" +
                        "</audios>",

                "<?xml version=\"1.0\"?>" +
                        "<documents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/document.xsd\">" +
                            "<document title=\"REPLACE REPLACE REPLACE\" author=\"REPLACE REPLACE\" " +
                                "topic=\"REPLACE REPLACE REPLACE\" " +
                                "licenseType=\"REPLACE REPLACE REPLACE\" " +
                                "license=\"REPLACE REPLACE\" " +
                        "/>" +
                        "</documents>",

                "<?xml version=\"1.0\"?>" +
                        "<applications " +
                            "xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/application.xsd\">" +
                            "<application name=\"REPLACE REPLACE\" publisher=\"REPLACE REPLACE\" " +
                                "platform=\"REPLACE REPLACE\" " +
                                "licenseType=\"REPLACE REPLACE REPLACE\" " +
                                "license=\"REPLACE REPLACE\"/>" +
                        "</applications>",

                "<?xml version=\"1.0\"?>" +
                        "<videos xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/video.xsd\">" +
                            "<video title=\"REPLACE REPLACE REPLACE\" director=\"REPLACE REPLACE\" " +
                                "producer=\"REPLACE REPLACE\" " +
                                "studio=\"REPLACE REPLACE REPLACE\" " +
                                "stars=\"REPLACE REPLACE REPLACE\" " +
                                "type=\"REPLACE REPLACE REPLACE\" " +
                                "minutes=\"REPLACE REPLACE REPLACE\" " +
                                "size=\"REPLACE REPLACE\" " +
                                "year=\"REPLACE\" " +
                                "language=\"REPLACE REPLACE\" " +
                                "subtitles=\"REPLACE REPLACE REPLACE\" " +
                                "rating=\"REPLACE REPLACE\" " +
                                "price=\"REPLACE REPLACE REPLACE\" " +
                                "shipping=\"REPLACE REPLACE REPLACE\" " +
                                " availability=\"REPLACE\"/>" +
                        "</videos>"};


        private static int count=0;

        public static String getNextTemplate() {
            return templates[count++ % templates.length];
        }
    }

    /**
     * Goal of class is to wait until numFilesIndexed files have been added
     * to the file manager.  Implemented by listening for ADD_FILE events
     * and decrementing a {@link java.util.concurrent.CountDownLatch}.
     */
    private static class NumFilesAddedListener implements EventListener<FileManagerEvent> {

        private final CountDownLatch latch;

        public NumFilesAddedListener(int numFilesIndexed) {
            latch = new CountDownLatch(numFilesIndexed);
        }

        public void handleEvent(FileManagerEvent evt) {

            if (evt.getType().equals(FileManagerEvent.Type.ADD_FILE)) {
                latch.countDown();
            }
        }

        /**
         * Blocks until numFilesIndexed ADD_FILE events have occurred.
         *
         * @param seconds timeout period, will return false if timeout exceeded
         * @return true if numFilesIndexed ADD_FILE events have occurred
         *         false otherwise (such as if timeout occurs)
         */
        public boolean awaitFinish(int seconds) {
            boolean finishedSuccessfully = false;
            try {
                finishedSuccessfully = latch.await(seconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignored, return unsuccessful
            }
            return finishedSuccessfully;
        }
    }
}