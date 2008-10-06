package org.limewire.ui.swing.search.model;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NameMatcherTest extends TestCase {
    public void testEqualStrings() {
        Assert.assertTrue(new NameMatcher().matches("123", "123"));
        Assert.assertTrue(new NameMatcher().matches("abc", "abc"));
    }
    
    public void testEqualsIgnoreCase() {
        Assert.assertTrue(new NameMatcher().matches("aBc", "AbC"));
    }
    
    public void testEqualsIgnoreSymbols() {
        Assert.assertTrue(new NameMatcher().matches("aBC.-", "AbC_()"));
    }
    
    public void testEqualsIgnoreLeafNodes() {
        Assert.assertTrue(new NameMatcher().matches("aBC(1)", "AbC"));
    }
}
