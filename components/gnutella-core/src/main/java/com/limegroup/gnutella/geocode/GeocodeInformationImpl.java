package com.limegroup.gnutella.geocode;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple implemenation of {@link GeocodeInformation}.
 */
final class GeocodeInformationImpl implements GeocodeInformation {

    private final Map<Property, String> names2values = new HashMap<Property, String>();

    static {
        // We have to make reference to one so they resolve, in case we're
        // creating them only from GeocodeInformation.STRINGS2PROPERTIES
        Property.AreaCode.getValue();
    }

    public String getProperty(Property p) {
        return names2values.get(p);
    }

    void setProperty(String name, String value) {
        setProperty(GeocodeInformation.STRINGS2PROPERTIES.get(name), value);
    }

    void setProperty(Property p, String value) {
        names2values.put(p, value);
    }

    public String toString() {
        return String.valueOf(names2values);
    }
}
