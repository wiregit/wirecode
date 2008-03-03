package org.limewire.promotion;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class SearcherDatabaseImplTest extends BaseTestCase {
    public SearcherDatabaseImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SearcherDatabaseImplTest.class);
    }

    public void testImplInit() {
        SearcherDatabaseImpl searcherDatabase = new SearcherDatabaseImpl();
        
    }
}
