package com.limegroup.gnutella;

import java.io.Serializable;

/**
 * A generic type of media, i.e., "video" or "audio".
 * Many different file formats can be of the same media type.
 * MediaType's are immutable.   Serializable for 
 * downloads.dat file; be careful when modifying!
 */
public class MediaType implements Serializable {
    static final long serialVersionUID = 3999062781289258389L;

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
        // See http://www.mrc-cbu.cam.ac.uk/Help/mimedefault.html
        MediaType text = new MediaType(SCHEMA_DOCUMENTS, DOCUMENTS,
            new String[] {
                "html", "htm", "xhtml", "mht", "mhtml", "xml",
                "txt", "ans", "asc", "diz", "eml",
                "pdf", "ps", "eps", "epsf",
                "rtf", "wri", "doc", "mcw", "wps",
                "xls", "wk1", "dif", "csv", "ppt",
                "hlp", "chm",
                "tex", "texi", "latex", "info", "man"
            });
        MediaType programs = new MediaType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "exe", "bin", "mdb",
                "sh", "csh", "awk", "pl",
                "zip", "jar", "arj", "rar", "ace", "lzh", "lha",
                "cab", "rpm", "deb", "msi", "msp",
                "gz", "gzip", "z", "bz2", "zoo",
                "tar", "tgz", "taz", "shar",
                "hqx", "sit", "dmg", "7z"
            });
        MediaType audio = new MediaType(SCHEMA_AUDIO, AUDIO,
            new String[] {
                "mp3", "mpa", "mp1", "mpga",
                "ra", "rm", "ram", "rmj",
                "wma", "wav",
                "lqt", "ogg", "med",
                "aif", "aiff", "aifc",
                "au", "snd", "iso",
                "mid", "midi", "rmi", "mod"
            });
        MediaType video = new MediaType(SCHEMA_VIDEO, VIDEO,
            new String[] {
                "mpg", "mpeg", "mpe", "mng", "mpv", "m1v",
                "vob", "mp2", "mpv2", "mp2v", "m2p", "m2v",
                "vcd", "mp4", "dv", "dvd", "div", "divx", "dvx",
                "smi", "smil", "rm", "ram", "rv",
                "avi", "asf", "wmv", "qt", "mov",
                "fli", "flc", "flx",
                "wml", "vrml", "swf", "dcr", "jve", "nsv"
            });
        MediaType images = new MediaType(SCHEMA_IMAGES, IMAGES,
            new String[] {
                "gif", "png",
                "jpg", "jpeg", "jpe", "jif", "jiff", "jfif",
                "tif", "tiff", "iff", "lbm", "ilbm",
                "mac", "drw", "pct", "img",
                "bmp", "dib", "rle", "ico", "ani", "icl", "cur",
                "emf", "wmf", "pcx",
                "pcd", "tga", "pic",
                "psd", "wpg", "dcx", "cpt", "mic",
                "pbm", "pnm", "ppm", "xbm", "xpm", "xwd",
                "sgi", "fax", "rgb", "ras"
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
