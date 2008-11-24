package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl.DOCUMENT_END;
import junit.framework.TestCase;

import org.limewire.ui.swing.search.model.BasicDownloadState;

public class SearchHeadingDocumentBuilderImplTest extends TestCase {
    private static final String NIGHT_LIFE = "Night life";
    private SearchHeadingDocumentBuilderImpl bldr;
    
    public void setUp() {
        bldr = new SearchHeadingDocumentBuilderImpl();
    }

    public void testNonMouseOverHeadingDocument() {
        String expected = 
             bldr.documentStartHTML + "<span class=\"title\">Night life</span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.NOT_STARTED, false, false));
    }
    
    public void testMouseOverHeadingDocument() {
        String expected = 
            bldr.documentStartHTML + "<span class=\"title\"><a href=\"#download\">Night life</a></span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.NOT_STARTED, true, false));
    }
    
    public void testDownloadingHeadingDocument() {
        String expected = 
            bldr.documentStartHTML + "You are <a href=\"#downloading\">downloading</a> <span class=\"title\">Night life</span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.DOWNLOADING, false, false));

    }
    
    public void testFileInLibraryHeadingDocument() {
        String expected = 
            bldr.documentStartHTML + "<span class=\"title\">Night life</span> is in <a href=\"#library\">Your Library</a>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.LIBRARY, false, false));
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.DOWNLOADED, false, false));
    }

    public void testHandleSpam() {
        String expected = 
            bldr.documentStartHTML + "<span class=\"title\">Night life</span> is Spam" + DOCUMENT_END;
       assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.NOT_STARTED, false, true));
       assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.DOWNLOADING, false, true));
       assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.DOWNLOADED, false, true));
       assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.LIBRARY, false, true));
    }
}
