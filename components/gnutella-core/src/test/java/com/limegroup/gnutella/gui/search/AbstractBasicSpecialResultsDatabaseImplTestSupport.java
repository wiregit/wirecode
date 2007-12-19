package com.limegroup.gnutella.gui.search;

import java.util.List;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * This class allows subclasses to call
 * {@link #runTestWithString(Object, String, com.limegroup.gnutella.gui.search.AbstractBasicSpecialResultsDatabaseImplTest.Getter)}
 * and pass in an expected value to get from the first search result.All queries
 * are done for the String <code>cat</code> and all <code>buf</code> Strings
 * should generate at least one {@link SearchResult} for <code>cat</code>.  For example:
 * <pre>
 * "cat\tcat|size=1230\tname=the-name\tartist=dr. soos0\talbum=cat and a horse0\tcreation_time=1231230\tvendor=someone0\tgenre=childrens0\tlicense=free\n"
 * </pre>
 */
abstract class AbstractBasicSpecialResultsDatabaseImplTestSupport extends LimeTestCase {
    
    private final BasicSpecialResultsDatabaseImplTestHelper helper = new BasicSpecialResultsDatabaseImplTestHelper();
    
    public AbstractBasicSpecialResultsDatabaseImplTestSupport(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        helper.setUp();
    }  
  
    /**
     * Defines an interface to inspect a {@link SearchResult} and return one of
     * its attributes.
     */
    protected interface Getter {
        /**
         * Returns something from <code>sr</code>, e.g.
         * {@link SearchResult#getFileName()}.
         * 
         * @param sr subject of the inspection
         * @return something from <code>sr</code>, e.g.
         *         {@link SearchResult#getFileName()}.
         */
        Object get(SearchResult sr);
    }
    
    /**
     * Creates a search result from <code>buf</code> and assures that the
     * attributes specified by <code>attr</code> is null, but everything else
     * is created fine.
     * 
     * @param buf keywords and payloads
     * @param attr attributes that should be <code>null</code>
     */
    protected final void runTestWithString(final Object expected, String buf, final Getter getter) {       
        BasicSpecialResultsDatabaseImpl db = helper.newDatabase(buf);
        db.find("cat", null /* this is ok */, new ThirdPartyResultsDatabase.SearchResultsCallback() {
            public void process(List<SearchResult> results, SearchInformation info) {
                if(expected == NOTHING) {
                    assertTrue(results.isEmpty());
                } else {
                    assertFalse(results.isEmpty());
                    SearchResult sr = results.get(0);
                    assertNotNull(sr);
                    Object o = getter.get(sr);
                    assertTrue(expected + " != " + o,
                               expected == o || 
                               (expected instanceof Number&& o instanceof Number 
                                && ((Number)expected).intValue() ==  ((Number)o).intValue()));
                }
            }
        });
    }
    
    private static final Object NOTHING = new Object();
    
    protected final void runTestAndExpectNothing(String buf) {
        runTestWithString(NOTHING, buf, null);
    }
    
    /**
     * Calls
     * {@link #runTestWithString(Object, String, com.limegroup.gnutella.gui.search.AbstractBasicSpecialResultsDatabaseImplTest.Getter)}
     * expecting a <code>null</code> Object.
     */
    protected final void runTestWithStringNull(String buf, Getter getter) {
        runTestWithString(null, buf, getter);
    }
    
    /**
     * Calls
     * {@link #runTestWithString(Object, String, com.limegroup.gnutella.gui.search.AbstractBasicSpecialResultsDatabaseImplTest.Getter)}
     * expecting a <code>0</code> number.
     */    
    protected final void runTestWithStringZero(String buf, Getter getter) {
        runTestWithString(0, buf, getter);
    }
    
    /**
     * Calls
     * {@link #runTestWithString(Object, String, com.limegroup.gnutella.gui.search.AbstractBasicSpecialResultsDatabaseImplTest.Getter)}
     * expecting a <code>-1</code> number.
     */    
    protected final void runTestWithStringNegativeOne(String buf, Getter getter) {
        runTestWithString(-1, buf, getter);
    }
}
