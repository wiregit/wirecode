package org.limewire.ui.swing.search.resultpanel;

import java.util.Comparator;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class compares VisualSearchResult objects by their descriptions.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ResultComparator implements Comparator<VisualSearchResult> {

    @Override
    public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
        return vsr1.getDescription().compareTo(vsr2.getDescription());
    }
}
