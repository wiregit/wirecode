package com.limegroup.gnutella;

/**
 * A generic type of media, i.e., "video" or "audio".
 * Many different file formats can be of the same media type.
 * MediaType's are immutable.
 */
public class MediaType {
    private String schema;
    private String description;
    private String[] extensions;
    
    // These values should match standard MIME content-type
    // categories and/or XSD schema names.
    private static final String SCHEMA_ANY_TYPE = "*";
    private static final String SCHEMA_DOCUMENTS = "text";
    private static final String SCHEMA_PROGRAMS = "application";
    private static final String SCHEMA_AUDIO = "audio";
    private static final String SCHEMA_VIDEO = "video";
    private static final String SCHEMA_IMAGES = "image";
    
    // These are used as resource keys to retreive descriptions in the GUI
    private static final String ANY_TYPE = "MEDIA_ANY_TYPE";
    private static final String DOCUMENTS = "MEDIA_DOCUMENTS";
    private static final String PROGRAMS = "MEDIA_PROGRAMS";
    private static final String AUDIO = "MEDIA_AUDIO";
    private static final String VIDEO = "MEDIA_VIDEO";
    private static final String IMAGES = "MEDIA_IMAGES";
    
    /**
     * @param schema a MIME compliant non-localizable identifier,
     *  that matches file categories (and XSD schema names).
     * @param descriptionKey a media identifier that can be used
     *  to retreive a localizable descriptive text.
     * @param extensions a list of all file extensions of this
     *  type.  Must be all lowercase.  If null, this matches
     *  any file.
     */
    public MediaType(String schema, String description,
                     String[] extensions) {
        this.schema = schema;
        this.description = description;
        this.extensions = extensions;
    }
        
    /** 
     * Returns true if a file with the given name is of this
     * media type, i.e., the suffix of the filename matches
     * one of this' extensions. 
     */
    public boolean matches(String filename) {
        if (extensions == null)
            return true;

        //Get suffix of filename.
        int j = filename.lastIndexOf(".");
        if (j == -1 || j == filename.length())
            return false;
        String suffix = filename.substring(j+1).toLowerCase();

        // Match with extensions.
        for (int i = 0; i < extensions.length; i++) {
            if (suffix.equals(extensions[i]))
                return true;
        }
        return false;
    }
    
    /** 
     * Returns this' media-type (a MIME content-type category)
     * (previously returned a description key)
     */
    public String toString() {
        return schema;
    }
    
    /** 
     * Returns this' description key in localizable resources
     * (now distinct from the result of the toString method)
     */
    public String getDescription() {
        return description;
    }
    
    // Do we need lazzy instanciation ?
    private static MediaType[] allMediaTypes = null;
    public static final MediaType[] getDefaultMediaTypes() {
        if (allMediaTypes == null)
            allMediaTypes = getTypes();
        return allMediaTypes;
    }
    
    public static boolean isDefaultType(String schema) {
        final MediaType[] types = getDefaultMediaTypes();
        for (int i = types.length; --i >= 0;)
            if (schema.equals(types[i].schema))
                return true;
            return false;
    }
    
    // do we really need this static method ?
    public static MediaType getAudioMediaType() {
        // index should match the above constructor
        return (getDefaultMediaTypes())[3]; /* AUDIO */
    }
    
    // do we really need this static method ?
    public static MediaType getVideoMediaType() {
        // index should match the above constructor
        return (getDefaultMediaTypes())[4]; /* VIDEO */
    }
    
    /** Returns an array of default media types. */
    private static MediaType[] getTypes() {
        MediaType any = new MediaType(SCHEMA_ANY_TYPE, ANY_TYPE,
                                      null);
            // This list isn't exhaustive, but it's not clear that we want it
            // to.  We may only want the most common file types here.
            // See http://www.mrc-cbu.cam.ac.uk/Help/mimedefault.html
        MediaType text = new MediaType(SCHEMA_DOCUMENTS, DOCUMENTS,
            new String[] {
                "html", "htm", "xml", "xhtml", "txt",
                "pdf", "ps", "eps", "rtf", "doc",
                "tex", "texi", "latex", "man"
            });
        MediaType programs = new MediaType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "exe", "bin", "sh", "csh",
                "zip", "arj", "rar", "lha",
                "cab", "rpm", "deb", "msi", "msp",
                "gz", "gzip", "z",
                "tar", "tgz", "taz", "shar",
                "hqx", "sit", "dmg"
            });
        MediaType audio = new MediaType(SCHEMA_AUDIO, AUDIO,
            new String[] {
                "mp3", "mpa", "ogg",
                "ra", "rm", "ram", "rmj", "lqt",
                "wav", "au", "snd",
                "aif", "aiff", "aifc"
            });
        MediaType video = new MediaType(SCHEMA_VIDEO, VIDEO,
            new String[] {
                "mpg", "mpeg", "mpe", "mng",
                "rm", "ram", "asf", "qt", "mov", "avi",
                "swf", "dcr", "jve"
            });
        MediaType images = new MediaType(SCHEMA_IMAGES, IMAGES,
            new String[] {
                "gif", "jpg", "jpeg", "jpe", "png",
                "tif", "tiff", "bmp", "ico", "pcx",
                "pic", "pbm", "pnm", "ppm", "xwd"
            });
        // Added by Sumeet Thadani to allow a rich search window to be popped up.
        return new MediaType[] {any, text, programs, audio, video, images};
    }
    
    //public static void main(String args[]) {
    //    MediaType[] types = getDefaultMediaTypes();
    //    MediaType mt;
    //
    //    mt = types[0]; /* SCHEMA_ANY_TYPE */
    //    Assert.that(mt.matches("foo.jpg"));
    //    Assert.that(mt.matches("foo"));
    //    Assert.that(mt.matches(""));
    //
    //    mt = types[1]; /* SCHEMA_DOCUMENTS */
    //    Assert.that(mt.toString().equals(SCHEMA_DOCUMENTS));
    //    Assert.that(mt.matches("foo.html"));
    //    Assert.that(mt.matches("foo.HTML"));
    //    Assert.that(mt.matches("foo.ps"));
    //    Assert.that(mt.matches("foo.PS"));
    //    Assert.that(! mt.matches("foo.jpg"));
    //    Assert.that(! mt.matches("foo"));
    //    Assert.that(! mt.matches("foo."));
    //}
}
