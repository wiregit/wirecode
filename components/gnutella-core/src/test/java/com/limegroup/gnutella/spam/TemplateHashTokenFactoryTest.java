package com.limegroup.gnutella.spam;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class TemplateHashTokenFactoryTest extends BaseTestCase {

    private TemplateHashTokenFactory factory;

    public TemplateHashTokenFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TemplateHashTokenFactoryTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        factory = new TemplateHashTokenFactory();
    }

    public void testCreatesTokenIfFilenameContainsQuery() {
        assertNotNull(factory.create("abc", "abcdef"));
    }

    public void testDoesNotCreateTokenIfFilenameDoesNotContainQuery() {
        assertNull(factory.create("abc", "defghi"));
    }

    public void testCaseOfQueryIsIgnored() {
        Token t1 = factory.create("ABC", "abcdef");
        Token t2 = factory.create("abc", "abcdef");
        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1, t2);
    }

    public void testCaseOfFilenameIsIgnored() {
        Token t1 = factory.create("abc", "ABCDEF");
        Token t2 = factory.create("abc", "abcdef");
        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1, t2);
    }

    public void testLeadingDigitsAndWhitespaceAreIgnored() {
        Token t1 = factory.create("abc", "abcdef");
        Token t2 = factory.create("abc", "0\t1 \n2abcdef");
        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1, t2);
    }

    public void testDifferentTemplatesCreateDifferentTokens() {
        Token t1 = factory.create("abc", "abcdef");
        Token t2 = factory.create("abc", "defabc");
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotEquals(t1, t2);
    }

    public void testWhitespaceInQueryIsCollapsed() {
        Token t1 = factory.create("abc \t  \n def", "abc def ghi");
        Token t2 = factory.create("abc def", "abc def ghi");
        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1, t2);
    }

    public void testWhitespaceInFilenameIsCollapsed() {
        Token t1 = factory.create("abc def", "abc  def \t\r\n ghi");
        Token t2 = factory.create("abc def", "abc def ghi");
        assertNotNull(t1);
        assertNotNull(t2);
        assertEquals(t1, t2);
    }
}
