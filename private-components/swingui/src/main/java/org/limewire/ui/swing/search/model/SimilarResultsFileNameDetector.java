package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * For every file name found in the search the parent that matches that filename
 * is put into the matchCache. A single parent might have more than 1 key. As a
 * new search result comes in, its fileNAmes are found, the parent matching him
 * is taken from the cache, and then a new parent is chosen between the two. The
 * new parent is then put in the cache for all the relevant filenames. Because
 * of ordering issues when processing items, a child might end up in the cache.
 * But when selecting a new parent, the findParent method checks items parents
 * as well. This prevents the data from being wrong when setting parents on
 * other visual search results.
 */
public class SimilarResultsFileNameDetector implements SimilarResultsDetector {
    private final Log LOG = LogFactory.getLog(getClass());

    private final CleanStringCache nameCache;

    private final Map<String, VisualSearchResult> matchCache;

    public SimilarResultsFileNameDetector() {
        this.nameCache = new CleanStringCache();
        this.matchCache = new HashMap<String, VisualSearchResult>();
    }

    @Override
    public void detectSimilarResult(VisualSearchResult visualSearchResult) {
        if (!visualSearchResult.isSpam()) {
            Set<String> names = getCleanFileNames(visualSearchResult);
            VisualSearchResult parent = null;
            for (String name : names) {
                parent = matchCache.get(name);
                if (parent == null) {
                    matchCache.put(name, visualSearchResult);
                } else {
                    parent = update(parent, visualSearchResult);
                    matchCache.put(name, parent);
                }
            }
        }
    }

    public Set<String> getCleanFileNames(VisualSearchResult visualSearchResult) {
        List<SearchResult> coreResults = visualSearchResult.getCoreSearchResults();
        Set<String> cleanFileNames = new HashSet<String>();
        for (SearchResult searchResult : coreResults) {
            String cleanFileName = nameCache.cleanString(searchResult.getFileName());
            cleanFileNames.add(cleanFileName);
        }
        return cleanFileNames;
    }

    /**
     * Finds the parent correlating the two searchResults and then moves these
     * search results under that parent. Then updates the visibility of these
     * items based on their parents visibilities.
     * 
     * Returns the parent chosen for these search results.
     */
    private VisualSearchResult update(VisualSearchResult o1, VisualSearchResult o2) {

        VisualSearchResult parent = findParent(o1, o2);

        boolean childrenVisible = o1.isChildrenVisible() || o2.isChildrenVisible()
                || parent.isChildrenVisible() || o1.getSimilarityParent() != null
                && o1.getSimilarityParent().isChildrenVisible() || o2.getSimilarityParent() != null
                && o2.getSimilarityParent().isChildrenVisible()
                || parent.getSimilarityParent() != null
                && parent.getSimilarityParent().isChildrenVisible();

        updateParent(o1.getSimilarityParent(), parent);
        updateParent(o1, parent);
        updateParent(o2.getSimilarityParent(), parent);
        updateParent(o2, parent);

        updateVisibility(parent, childrenVisible);

        return parent;
    }

    /**
     * Update visibilities of newly changed parents.
     */
    private void updateVisibility(VisualSearchResult parent, final boolean childrenVisible) {
        LOG.debugf("Setting child visibility for {0} to {1}", parent.getCoreSearchResults().get(0)
                .getUrn(), childrenVisible);
        parent.setVisible(true);
        parent.setChildrenVisible(childrenVisible);
    }

    /**
     * Updates the child to use the given parent. The parent is set, the
     * children are moved, and the visibility is copied. Also the given child is
     * checked to see if it already has a parent, if so its parent is also
     * updated to be a child of the given parent.
     */
    private void updateParent(VisualSearchResult child, VisualSearchResult parent) {
        parent.setSimilarityParent(null);
        if (child != null && child != parent) {
            child.setSimilarityParent(parent);
            parent.addSimilarSearchResult(child);
            moveChildren(child, parent);
        }
    }

    /**
     * Moves the children from the child to the parent.
     */
    private void moveChildren(VisualSearchResult child, VisualSearchResult parent) {
        child.removeSimilarSearchResult(parent);
        for (VisualSearchResult item : child.getSimilarResults()) {
            updateParent(item, parent);
            child.removeSimilarSearchResult(item);
            parent.addSimilarSearchResult(item);
        }
    }

    /**
     * Returns which item should be the parent between the two similar search
     * results. Currently the item with the most sources, is considered the
     * parent.
     */
    private VisualSearchResult findParent(VisualSearchResult o1, VisualSearchResult o2) {
        VisualSearchResult parent = null;

        VisualSearchResult parent1 = o1;
        VisualSearchResult parent2 = o2;
        VisualSearchResult parent3 = o1.getSimilarityParent();
        VisualSearchResult parent4 = o2.getSimilarityParent();
        int parent1Count = parent1 == null ? 0 : parent1.getSources().size();
        int parent2Count = parent2 == null ? 0 : parent2.getSources().size();
        int parent3Count = parent3 == null ? 0 : parent3.getSources().size();
        int parent4Count = parent4 == null ? 0 : parent4.getSources().size();

        if (parent4Count > parent3Count && parent4Count > parent2Count
                && parent4Count > parent1Count) {
            parent = parent4;
        } else if (parent3Count > parent2Count && parent3Count > parent1Count) {
            parent = parent3;
        } else if (parent2Count > parent1Count) {
            parent = parent2;
        } else {
            parent = parent1;
        }

        return parent;
    }

}
