package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl.DOCUMENT_END;
import static org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl.DOCUMENT_START;
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
             DOCUMENT_START + "<span class=\"title\">Night life</span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.NOT_STARTED, false));
    }
    
    public void testMouseOverHeadingDocument() {
        String expected = 
            DOCUMENT_START + "<span class=\"title\"><a href=\"#download\">Night life</a></span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.NOT_STARTED, true));
    }
    
    public void testDownloadingHeadingDocument() {
        String expected = 
            DOCUMENT_START + "You are <a href=\"#downloading\">downloading</a> <span class=\"title\">Night life</span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.DOWNLOADING, false));
    }
    
    public void testFileInLibraryHeadingDocument() {
        String expected = 
            DOCUMENT_START + "<span class=\"title\">Night life</span> is in <a href=\"#library\">Your Library</a>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.LIBRARY, false));
        assertEquals(expected, bldr.getHeadingDocument(NIGHT_LIFE, BasicDownloadState.DOWNLOADED, false));
    }

    public void testFileInLibraryHeadingWhenInputStringNeedsExistingHTMLTagsStrippedDocument() {
        String expected = 
            DOCUMENT_START + "<span class=\"title\">Night life</span> is in <a href=\"#library\">Your Library</a>" + DOCUMENT_END;
        String input = "<html>" + NIGHT_LIFE + "</html>";
        assertEquals(expected, bldr.getHeadingDocument(input, BasicDownloadState.LIBRARY, false));
        assertEquals(expected, bldr.getHeadingDocument(input, BasicDownloadState.DOWNLOADED, false));
    }
}
