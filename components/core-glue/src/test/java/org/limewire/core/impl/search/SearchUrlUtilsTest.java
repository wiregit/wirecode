package org.limewire.core.impl.search;

import org.limewire.util.BaseTestCase;

/**
 * Tests the static methods provided by {@link SearchUrlUtils}.
 */
public class SearchUrlUtilsTest extends BaseTestCase {

    public SearchUrlUtilsTest(String name) {
        super(name);
    }

    /**
     * Tests {@link SearchUrlUtils#stripUrl(String)}.
     */
    public void testStripUrl() {
        assertEquals("asdsadsda.com", SearchUrlUtils.stripUrl("http://asdsadsda.com//asdsad/sad"));
        assertEquals("asfdsaddsasda.cffom", SearchUrlUtils.stripUrl("htasdsatp://asfdsaddsasda.cffom////"));
        assertEquals("a.c", SearchUrlUtils.stripUrl("htasdsatp://a.c"));
        assertEquals("a.c", SearchUrlUtils.stripUrl("a.c/asdsadsadsadsad"));
        assertEquals("asfd.s.a.d.d.sasda.c", SearchUrlUtils.stripUrl("ftp://asfd.s.a.d.d.sasda.c/a/.d/f/?f"));
        assertEquals("a b.c", SearchUrlUtils.stripUrl("hta&sds-\\atp://a b.c////"));
    }
}
