package org.limewire.ui.swing.search.model;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CleanStringCacheTest extends TestCase {
    public void testEqualStrings() {
        Assert.assertTrue(new CleanStringCache().matches("123", "123"));
        Assert.assertTrue(new CleanStringCache().matches("abc", "abc"));
    }
    
    public void testEqualsIgnoreCase() {
        Assert.assertTrue(new CleanStringCache().matches("aBc", "AbC"));
    }
    
    public void testEqualsIgnoreSymbols() {
        Assert.assertTrue(new CleanStringCache().matches("aBC.-", "AbC_()"));
    }
    
    public void testEqualsIgnoreLeafNodes() {
        Assert.assertTrue(new CleanStringCache().matches("aBC(1)", "AbC"));
    }
}
