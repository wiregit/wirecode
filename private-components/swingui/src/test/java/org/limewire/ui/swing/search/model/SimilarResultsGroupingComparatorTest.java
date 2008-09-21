package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.TestCase;

public class SimilarResultsGroupingComparatorTest extends TestCase {
    private static final String NON_PARENT = "nonParent";
    private static final String SIMILAR1 = "similar1";
    private static final String SIMILAR2 = "similar2";
    private static final String PARENT = "parent";
    private ArrayList<VisualSearchResult> results;
    private MockVisualSearchResult parent;
    private ArrayList<VisualSearchResult> similarResults;
    private VisualSearchResult simResult1;
    private VisualSearchResult simResult2;
    private MockVisualSearchResult nonParent;
    private SimilarResultsGroupingComparator comparator;

    @Override
    protected void setUp() {
        comparator = new SimilarResultsGroupingComparator() {
            @Override
            protected int doCompare(VisualSearchResult result1, VisualSearchResult result2) {
                return result1.getDescription().compareTo(result2.getDescription());
            }
        };

        results = new ArrayList<VisualSearchResult>();
        parent = new MockVisualSearchResult(PARENT);
        similarResults = new ArrayList<VisualSearchResult>();
        parent.setSimilarResults(similarResults);
        simResult1 = MockSimilarResultsFactory.newMockVisualSearchResult(parent, SIMILAR1);
        similarResults.add(simResult1);
        simResult2 = MockSimilarResultsFactory.newMockVisualSearchResult(parent, SIMILAR2);
        similarResults.add(simResult2);
        nonParent = new MockVisualSearchResult(NON_PARENT);
    }

    public void testSorting1() {
        populate(simResult2, parent, nonParent, simResult1);
        assertOrder(NON_PARENT, PARENT, SIMILAR2, SIMILAR1);
    }

    public void testSorting2() {
        populate(nonParent, simResult2, parent, simResult1);
        assertOrder(NON_PARENT, PARENT, SIMILAR2, SIMILAR1);
    }

    public void testSorting3() {
        populate(simResult2, simResult1, parent, nonParent);
        assertOrder(NON_PARENT, PARENT, SIMILAR2, SIMILAR1);
    }

    public void testSorting4() {
        populate(simResult1, simResult2, nonParent, parent);
        assertOrder(NON_PARENT, PARENT, SIMILAR1, SIMILAR2);
        
        results.clear();

        populate(parent, simResult2, nonParent, simResult1);
        assertOrder(NON_PARENT, PARENT, SIMILAR2, SIMILAR1);
    }
    
    private void populate(VisualSearchResult... results) {
        for(VisualSearchResult result : results) {
            this.results.add(result);
        }
    }
    
    private void assertOrder(String... order) {
        Collections.sort(results, comparator);
        /*for(VisualSearchResult result : results) {
            System.out.print(result.getDescription() + ", ");
        }
        System.out.println("");*/
        
        int index = 0;
        for (String result : order) {
            assertEquals(result, results.get(index++).getDescription());
        }
    }
}
