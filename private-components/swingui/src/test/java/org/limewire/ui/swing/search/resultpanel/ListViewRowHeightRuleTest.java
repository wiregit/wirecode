package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.*;
import junit.framework.TestCase;

import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.MockVisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayResult;

public class ListViewRowHeightRuleTest extends TestCase {

    private ListViewRowHeightRuleImpl rule;
    private MockVisualSearchResult mock;
    
    @Override
    protected void setUp() throws Exception {
        rule = new ListViewRowHeightRuleImpl();
        mock = new MockVisualSearchResult("Foo");
    }

    public void testHeadingOnly() {
        assertStuff(BasicDownloadState.DOWNLOADING, "bar", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADED, "bar", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.LIBRARY, "bar", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADING, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADED, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.LIBRARY, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.NOT_STARTED, "", HeadingOnly, true, true);
        mock.setSpam(true);
        assertStuff(BasicDownloadState.NOT_STARTED, "bar", HeadingOnly, true, true);
    }

    private void assertStuff(BasicDownloadState state, String search, RowDisplayConfig config, boolean subHeadingEmpty, boolean metadataNull) {
        mock.setDownloadState(state);
        RowDisplayResult result = rule.getDisplayResult(mock, search);
        assertEquals(config, result.getConfig());
        assertEquals(subHeadingEmpty , result.getSubheading() == null || "".equals(result.getSubheading()));
        assertEquals(metadataNull, result.getMetadata() == null);
    }
    
    public void testHeadingAndSubHeading() {
        mock.setDownloadState(BasicDownloadState.NOT_STARTED);
        mock.setSubHeading("bar");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingAndSubheading, false, true);
        
        mock.setHeading("bar");
        mock.setSubHeading("foo");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingAndSubheading, false, true);
    }

    public void testHeadingSubHeadingAndMetadata() {
        mock.setHeading("bar");
        mock.setSubHeading("baz");
        mock.getProperties().put(PropertyKey.NAME, "foo");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingSubHeadingAndMetadata, false, false);
    }
}
