package com.limegroup.gnutella.licenses;

public class CCConstants {

    /** At verification URLs, this is where the license for the file (verified
     *  against the one described in the actual content) can be found.
     */
    public static final String LICENSE_RDF_OPEN = "<license rdf:resource=\"";
    
    /** This is the string that will 1) go out in CC queries and 2) should be
     *  QRP tables.
     */
    public static final String 
        CC_URI_PREFIX = "http://creativecommons.org/licenses/";

    /** This is the ID for the license field in xml schemas.
     */
    public static final String
        AUDIO_LICENSE_NAME = "audios__audio__license__";

    /** This is the shortened version of AUDIO_LICENSE_NAME.
     */
    public static final String AUDIO_LICENSE_NAME_ABBREV = "License";

    /** This is what we display to the user in the search screen drop down box.
     */
    public static final String CC_LICENSE_STRING = "Creative Commons";

}