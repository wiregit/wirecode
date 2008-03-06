package com.limegroup.gnutella.geocode;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines an interface for geographic information created from a
 * {@link Geocoder} about the client. This is basically a map from property
 * names to values. Access to these values is through the method
 * {@link #getProperty(com.limegroup.gnutella.geocode.GeocodeInformation.Property)}.
 */
public interface GeocodeInformation {
    
    /**
     * Maps {@link Property} names to values so that we can turn {@link String}s
     * into {@link Property Properties}. In order to use these without
     * specifically naming one you must make some reference to one of the enum
     * members. For example:
     * 
     * <pre>
     * Property.AreaCode.getValue();
     * </pre>
     * 
     * in {@link GeocodeInformationImpl}.
     */
    Map<String,Property> STRINGS2PROPERTIES = new HashMap<String,Property>();   

    /**
     * The various values
     *
     */
    enum Property {
        
        /** 
         * IP address -- e.g. <code>12.12.12.12</code>. 
         */
        Ip("Ip"),

        /** 
         * 2-letter country code -- e.g. <code>US</code>. 
         */
        CountryCode("CountryCode"),

        /**
         * 3-letter country code -- e.g. <code>USs</code>. 
         */
        CountryCode3("CountryCode3"),

        /**
         * Full country name -- e.g. <code>United States</code>. 
         */
        CountryName("CountryName"),

        /**
         * Short region name -- e.g. <code>NY</code>. 
         */
        Region("Region"),

        /**
         * Full region name -- e.g. <code>New York</code>. 
         */
        Region2("Region2"),

        /**
         * Full city name -- e.g. <code>New York</code>. 
         */
        City("City"),

        /**
         *  ountry-specific postal code -- e.g. <code>10004</code>. 
         */
        PostalCode("PostalCode"),

        /** 
         * Latitude in decimal degrees -- e.g. <code>40.6888</code>. 
         */
        Latitude("Latitude"),

        /** 
         * Longitude in decimal degrees -- e.g. <code>40.6888</code>. 
         */
        Longitude("Longitude"),

        /**
         * Dseignated market area code (<a
         * href="http://en.wikipedia.org/wiki/Media_market">wikipedia</a>) --
         * <code>e.g.</code>501</code>.
         */
        DmaCode("DmaCode"),

        /** 
         * Country-specific area code -- e.g. <code>212</code>. 
         */
        AreaCode("AreaCode");
        
        private final String s;
        
        Property(String s) {
            this.s = s;
            STRINGS2PROPERTIES.put(s,this);
        }
        
        String getValue() {
            return s;
        }
    }
    
    /**
     * Returns the String value for {@link Property} or <code>null</code>.
     * 
     * @param p key value
     * @return the String value for {@link Property} or <code>null</code>
     */
    String getProperty(Property p);

}