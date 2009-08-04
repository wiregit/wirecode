package com.limegroup.gnutella.filters.response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.KeywordFilter;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class XMLDocFilter extends KeywordFilter {

    private final Map<String, String> disallowedFieldValues;
    private final Map<String, List<String>> disallowedExactFieldValues;

    XMLDocFilter(boolean banAdult) {
        super(banAdult);
        if(banAdult) {
            ImmutableMap.Builder<String, String> fields =
                new ImmutableMap.Builder<String, String>();
            fields.put(LimeXMLNames.VIDEO_TYPE, "adult");
            fields.put(LimeXMLNames.VIDEO_RATING, "adult");
            disallowedFieldValues = fields.build();
            ImmutableMap.Builder<String, List<String>> exactFields =
                new ImmutableMap.Builder<String, List<String>>();
            exactFields.put(LimeXMLNames.VIDEO_RATING,
                    Arrays.asList("r", "nc-17"));
            disallowedExactFieldValues = exactFields.build();
        } else {
            disallowedFieldValues = Collections.emptyMap();
            disallowedExactFieldValues = Collections.emptyMap();
        }
    }

    @Override
    public boolean allow(QueryReply qr, Response response) {
        if(!super.allow(qr, response)) {
            return false;
        }
        LimeXMLDocument doc = response.getDocument();
        return doc == null || allowDoc(doc);
    }

    /**
     * Returns true if none of the filters matched. 
     */
    private boolean allowDoc(LimeXMLDocument doc) {
        for (Entry<String, String> entry : disallowedFieldValues.entrySet()) {
            String value = doc.getValue(entry.getKey());
            if (value != null) {
                value = value.toLowerCase(Locale.US);
                if (value.contains(entry.getValue())) {
                    return false;
                }
            }
        }
        for (Entry<String, List<String>> entry : disallowedExactFieldValues.entrySet()) {
            String value = doc.getValue(entry.getKey());
            if (value != null) {
                value = value.toLowerCase(Locale.US);
                for (String disallowed : entry.getValue()) {
                    if (value.equals(disallowed)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
