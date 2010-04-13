package com.limegroup.gnutella.spam;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.SHA1;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

public class TemplateHashTokenFactory {

    private static final Log LOG = LogFactory.getLog(TemplateHashTokenFactory.class);

    /**
     * The string that is used to replace the query when creating a template
     * from a filename.
     */
    private static final String REPLACEMENT_STRING = "****";

    private final SHA1 sha1 = new SHA1();

    /**
     * If the filename contains the query (trimmed and ignoring case), replaces
     * the query in the filename with a fixed string, strips digits and
     * whitespace from the start, and returns a token representing the hash of
     * the resulting template. Otherwise returns null.
     */
    public TemplateHashToken create(String query, String filename) {
        query = query.trim().toLowerCase(Locale.US);
        filename = filename.trim().toLowerCase(Locale.US);
        if(filename.contains(query)) {
            String template = filename.replace(query, REPLACEMENT_STRING);
            template = template.replaceFirst("^[0-9\\s]*", "");
            if(LOG.isDebugEnabled())
                LOG.debug("Created template: " + template);
            return createFromTemplate(template);
        }
        LOG.debug("Did not create template");
        return null;
    }

    /**
     * Creates a template hash token from a template token and returns it.
     */
    public TemplateHashToken convert(TemplateToken tt) {
        TemplateHashToken tht = createFromTemplate(tt.keyword);
        tht.setRating(tt.getRating());
        return tht;
    }

    private TemplateHashToken createFromTemplate(String template) {
        byte[] utf8 = StringUtils.toUTF8Bytes(template);
        synchronized(this) {
            sha1.reset();
            sha1.engineUpdate(utf8, 0, utf8.length);
            byte[] hash = sha1.digest();
            if(LOG.isDebugEnabled())
                LOG.debug("Template hash: " + Base32.encode(hash));
            return new TemplateHashToken(hash);
        }
    }
}
