package org.limewire.activation;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class PemCodecTest extends BaseTestCase {
    public PemCodecTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PemCodecTest.class);
    }

    public void testEncodeEmpty() {
        assertEquals("", PemCodec.encode("".getBytes()));
    }

    public void testEncodeSmall() {
        assertEquals("Zm9v\n", PemCodec.encode("foo".getBytes()));
    }

    public void testEncodeBig() {
        String thing = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Pellentesque in ipsum. Morbi semper massa ac magna. Aliquam lorem sapien, fringilla sed, sagittis id, iaculis in, arcu. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Quisque sagittis nulla interdum elit. Praesent iaculis. Mauris egestas posuere tortor. Maecenas nibh. Duis nec est eu risus interdum luctus. Fusce nec eros quis sapien tempus scelerisque. Morbi facilisis sodales mi. Duis semper semper nulla. Suspendisse potenti. Proin vitae nisl ut arcu molestie luctus. Nunc tristique vestibulum est. Duis ac enim.";
        String expected = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVlciBhZGlwaXNj\n"
                + "aW5nIGVsaXQuIFBlbGxlbnRlc3F1ZSBpbiBpcHN1bS4gTW9yYmkgc2VtcGVyIG1h\n"
                + "c3NhIGFjIG1hZ25hLiBBbGlxdWFtIGxvcmVtIHNhcGllbiwgZnJpbmdpbGxhIHNl\n"
                + "ZCwgc2FnaXR0aXMgaWQsIGlhY3VsaXMgaW4sIGFyY3UuIFBlbGxlbnRlc3F1ZSBo\n"
                + "YWJpdGFudCBtb3JiaSB0cmlzdGlxdWUgc2VuZWN0dXMgZXQgbmV0dXMgZXQgbWFs\n"
                + "ZXN1YWRhIGZhbWVzIGFjIHR1cnBpcyBlZ2VzdGFzLiBRdWlzcXVlIHNhZ2l0dGlz\n"
                + "IG51bGxhIGludGVyZHVtIGVsaXQuIFByYWVzZW50IGlhY3VsaXMuIE1hdXJpcyBl\n"
                + "Z2VzdGFzIHBvc3VlcmUgdG9ydG9yLiBNYWVjZW5hcyBuaWJoLiBEdWlzIG5lYyBl\n"
                + "c3QgZXUgcmlzdXMgaW50ZXJkdW0gbHVjdHVzLiBGdXNjZSBuZWMgZXJvcyBxdWlz\n"
                + "IHNhcGllbiB0ZW1wdXMgc2NlbGVyaXNxdWUuIE1vcmJpIGZhY2lsaXNpcyBzb2Rh\n"
                + "bGVzIG1pLiBEdWlzIHNlbXBlciBzZW1wZXIgbnVsbGEuIFN1c3BlbmRpc3NlIHBv\n"
                + "dGVudGkuIFByb2luIHZpdGFlIG5pc2wgdXQgYXJjdSBtb2xlc3RpZSBsdWN0dXMu\n"
                + "IE51bmMgdHJpc3RpcXVlIHZlc3RpYnVsdW0gZXN0LiBEdWlzIGFjIGVuaW0u\n";
        assertEquals(expected, PemCodec.encode(thing.getBytes()));
    }

    public void testDecodeSmall() {
        assertEquals("foo".getBytes(), PemCodec.decode("Zm9v"));
        assertEquals("foo".getBytes(), PemCodec.decode("Zm9v\n"));
    }

    public void testDecodeBig() {
        String expected = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Pellentesque in ipsum. Morbi semper massa ac magna. Aliquam lorem sapien, fringilla sed, sagittis id, iaculis in, arcu. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Quisque sagittis nulla interdum elit. Praesent iaculis. Mauris egestas posuere tortor. Maecenas nibh. Duis nec est eu risus interdum luctus. Fusce nec eros quis sapien tempus scelerisque. Morbi facilisis sodales mi. Duis semper semper nulla. Suspendisse potenti. Proin vitae nisl ut arcu molestie luctus. Nunc tristique vestibulum est. Duis ac enim.";
        String encoded = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVlciBhZGlwaXNj\n"
                + "aW5nIGVsaXQuIFBlbGxlbnRlc3F1ZSBpbiBpcHN1bS4gTW9yYmkgc2VtcGVyIG1h\n"
                + "c3NhIGFjIG1hZ25hLiBBbGlxdWFtIGxvcmVtIHNhcGllbiwgZnJpbmdpbGxhIHNl\n"
                + "ZCwgc2FnaXR0aXMgaWQsIGlhY3VsaXMgaW4sIGFyY3UuIFBlbGxlbnRlc3F1ZSBo\n"
                + "YWJpdGFudCBtb3JiaSB0cmlzdGlxdWUgc2VuZWN0dXMgZXQgbmV0dXMgZXQgbWFs\n"
                + "ZXN1YWRhIGZhbWVzIGFjIHR1cnBpcyBlZ2VzdGFzLiBRdWlzcXVlIHNhZ2l0dGlz\n"
                + "IG51bGxhIGludGVyZHVtIGVsaXQuIFByYWVzZW50IGlhY3VsaXMuIE1hdXJpcyBl\n"
                + "Z2VzdGFzIHBvc3VlcmUgdG9ydG9yLiBNYWVjZW5hcyBuaWJoLiBEdWlzIG5lYyBl\n"
                + "c3QgZXUgcmlzdXMgaW50ZXJkdW0gbHVjdHVzLiBGdXNjZSBuZWMgZXJvcyBxdWlz\n"
                + "IHNhcGllbiB0ZW1wdXMgc2NlbGVyaXNxdWUuIE1vcmJpIGZhY2lsaXNpcyBzb2Rh\n"
                + "bGVzIG1pLiBEdWlzIHNlbXBlciBzZW1wZXIgbnVsbGEuIFN1c3BlbmRpc3NlIHBv\n"
                + "dGVudGkuIFByb2luIHZpdGFlIG5pc2wgdXQgYXJjdSBtb2xlc3RpZSBsdWN0dXMu\n"
                + "IE51bmMgdHJpc3RpcXVlIHZlc3RpYnVsdW0gZXN0LiBEdWlzIGFjIGVuaW0u\n";
        assertEquals(expected, new String(PemCodec.decode(encoded)));
    }

    public void testEncodeDecodeCycle() {
        String source = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Pellentesque in ipsum. Morbi semper massa ac magna. Aliquam lorem sapien, fringilla sed, sagittis id, iaculis in, arcu. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Quisque sagittis nulla interdum elit. Praesent iaculis. Mauris egestas posuere tortor. Maecenas nibh. Duis nec est eu risus interdum luctus. Fusce nec eros quis sapien tempus scelerisque. Morbi facilisis sodales mi. Duis semper semper nulla. Suspendisse potenti. Proin vitae nisl ut arcu molestie luctus. Nunc tristique vestibulum est. Duis ac enim.";
        for (int i = 0; i < source.length(); i++) {
            String original = source.substring(0, i);
            String encoded = PemCodec.encode(original.getBytes());
            String decoded = new String(PemCodec.decode(encoded));
            assertEquals(original, decoded);
        }
    }
}
