package org.limewire.ui.swing.filter;

import org.limewire.friend.api.Friend;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for SourceMatcher.
 */
public class SourceMatcherTest extends BaseTestCase {
    /** Instance of class to be tested. */
    private SourceMatcher<MockFilterableItem> sourceMatcher;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public SourceMatcherTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sourceMatcher = new SourceMatcher<MockFilterableItem>(SourceItem.ANONYMOUS_SOURCE);
    }

    @Override
    protected void tearDown() throws Exception {
        sourceMatcher = null;
        super.tearDown();
    }

    /** Tests method to match item with anonymous source. */
    public void testMatchesAnonymous() {
        // Create test item.
        MockFilterableItem item = new MockFilterableItem("Test Item");
        
        // Verify anonymous item matches.
        boolean actualReturn = sourceMatcher.matches(item);
        assertTrue("matches anonymous", actualReturn);
    }

    /** Tests method to match item with any friend. */
    public void testMatchesAnyFriend() {
        // Create matcher for any friend.
        sourceMatcher = new SourceMatcher<MockFilterableItem>(SourceItem.ANY_FRIEND_SOURCE);
        
        // Create test item.
        MockFilterableItem item = new MockFilterableItem("Test Item");
        item.addFriend(new TestFriend("Vulcan"));
        
        // Verify any friend matches.
        boolean actualReturn = sourceMatcher.matches(item);
        assertTrue("matches any friend", actualReturn);
    }

    /** Tests method to match item with specific friend. */
    public void testMatchesFriend() {
        // Create matcher for specific friend.
        SourceItem friendSource = new SourceItem(SourceItem.Type.FRIEND, "Vulcan");
        sourceMatcher = new SourceMatcher<MockFilterableItem>(friendSource);
        
        // Create test item.
        MockFilterableItem item = new MockFilterableItem("Test Item");
        item.addFriend(new TestFriend("Vulcan"));
        
        // Verify same friend matches.
        boolean actualReturn = sourceMatcher.matches(item);
        assertTrue("matches same friend", actualReturn);
        
        // Create 2nd test item.
        MockFilterableItem item2 = new MockFilterableItem("Test Item");
        item2.addFriend(new TestFriend("Klingon"));
        
        // Verify different friend does not match.
        actualReturn = sourceMatcher.matches(item2);
        assertFalse("matches different friend", actualReturn);
    }
    
    /**
     * Test friend.
     */
    private static class TestFriend implements Friend {
        private String name;

        public TestFriend(String name) {
            this.name = name;
        }
        
        @Override
        public String getId() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRenderName() {
            return name;
        }
    }
}
