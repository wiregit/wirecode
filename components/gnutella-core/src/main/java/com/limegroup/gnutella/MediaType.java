package com.limegroup.gnutella;

import com.limegroup.gnutella.util.Comparators;
import com.limegroup.gnutella.util.DataUtils;

import com.sun.java.util.collections.Set;
import com.sun.java.util.collections.TreeSet;
import com.sun.java.util.collections.Arrays;

/**
 * A generic type of media, i.e., "video" or "audio".
 * Many different file formats can be of the same media type.
 * MediaType's are immutable.
 *
 * // See http://www.mrc-cbu.cam.ac.uk/Help/mimedefault.html
 */
public class MediaType {
    
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
    
    // An extension that signifies this MediaType, used
    // for looking up icons in the file system.
    private static final String EXT_ANY_TYPE = null;
    private static final String EXT_DOCUMENTS = "rtf";
    private static final String EXT_PROGRAMS = "exe";
    private static final String EXT_AUDIO = "mp3";
    private static final String EXT_VIDEO = "mpg";
    private static final String EXT_IMAGES = "jpg";

    /**
     * Type for 'any file'
     */
    private static final MediaType TYPE_ANY = 
        new MediaType(SCHEMA_ANY_TYPE, ANY_TYPE, EXT_ANY_TYPE, null);
                                       
    /**
     * Type for 'text'
     */
    private static final MediaType TYPE_TEXT =
        new MediaType(SCHEMA_DOCUMENTS, DOCUMENTS, EXT_DOCUMENTS,
            new String[] {
                "html", "htm", "xhtml", "mht", "mhtml", "xml",
                "txt", "ans", "asc", "diz", "eml",
                "pdf", "ps", "eps", "epsf",
                "rtf", "wri", "doc", "mcw", "wps",
                "xls", "wk1", "dif", "csv", "ppt",
                "hlp", "chm",
                "tex", "texi", "latex", "info", "man"
            });
        
    /**
     * Type for 'programs'
     */
    private static final MediaType TYPE_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS, EXT_PROGRAMS,
            new String[] {
                "exe", "bin", "mdb",
                "sh", "csh", "awk", "pl",
                "zip", "jar", "arj", "rar", "ace", "lzh", "lha",
                "cab", "rpm", "deb", "msi", "msp",
                "gz", "gzip", "z", "bz2", "zoo",
                "tar", "tgz", "taz", "shar",
                "hqx", "sit", "dmg", "7z",
                "iso", "nrg", "cue", "bin"
            });
        
    /**
     * Type for 'audio'
     */
    private static final MediaType TYPE_AUDIO =
        new MediaType(SCHEMA_AUDIO, AUDIO, EXT_AUDIO,
            new String[] {
                "mp3", "mpa", "mp1", "mpga",
                "ra", "rm", "ram", "rmj",
                "wma", "wav",
                "lqt", "ogg", "med",
                "aif", "aiff", "aifc",
                "au", "snd", "s3m",
                "mid", "midi", "rmi", "mod"
            });
        
    /**
     * Type for 'video'
     */
    private static final MediaType TYPE_VIDEO =
        new MediaType(SCHEMA_VIDEO, VIDEO, EXT_VIDEO,
            new String[] {
                "mpg", "mpeg", "mpe", "mng", "mpv", "m1v",
                "vob", "mp2", "mpv2", "mp2v", "m2p", "m2v",
                "vcd", "mp4", "dv", "dvd", "div", "divx", "dvx",
                "smi", "smil", "rm", "ram", "rv",
                "avi", "asf", "asx", "wmv", "qt", "mov",
                "fli", "flc", "flx",
                "wml", "vrml", "swf", "dcr", "jve", "nsv"
            });
        
    /**
     * Type for 'images'
     */
    private static final MediaType TYPE_IMAGES =
        new MediaType(SCHEMA_IMAGES, IMAGES, EXT_IMAGES,
            new String[] {
                "gif", "png",
                "jpg", "jpeg", "jpe", "jif", "jiff", "jfif",
                "tif", "tiff", "iff", "lbm", "ilbm", "eps",
                "mac", "drw", "pct", "img",
                "bmp", "dib", "rle", "ico", "ani", "icl", "cur",
                "emf", "wmf", "pcx",
                "pcd", "tga", "pic", "fig",
                "psd", "wpg", "dcx", "cpt", "mic",
                "pbm", "pnm", "ppm", "xbm", "xpm", "xwd",
                "sgi", "fax", "rgb", "ras"
            });
        
    /**
     * All media types.
     */
    private static final MediaType[] ALL_MEDIA_TYPES =
        new MediaType[] { TYPE_ANY, TYPE_TEXT, TYPE_PROGRAMS,
                          TYPE_AUDIO, TYPE_VIDEO, TYPE_IMAGES };
     
    
    /**
     * The description of this MediaType.
     */
    private final String schema;
    
    /**
     * The key to look up this MediaType.
     */
    private final String descriptionKey;
    
    /**
     * The list of extensions within this MediaType.
     */
    private final Set extensions;
    
    /**
     * The main extension for this MediaType.
     */
    private final String mainExt;
    
    /**
     * Whether or not this is one of the default media types.
     */
    private final boolean isDefault;
    
    /**
     * Constructs a MediaType with only a MIME-Type.
     */
    public MediaType(String schema) {
        this.schema = schema;
        this.descriptionKey = null;
        this.extensions = DataUtils.EMPTY_SET;
        this.mainExt = null;
        this.isDefault = false;
    }
    
    /**
     * @param schema a MIME compliant non-localizable identifier,
     *  that matches file categories (and XSD schema names).
     * @param descriptionKey a media identifier that can be used
     *  to retreive a localizable descriptive text.
     * @param extensions a list of all file extensions of this
     *  type.  Must be all lowercase.  If null, this matches
     *  any file.
     */
    public MediaType(String schema, String descriptionKey, String ext,
                     String[] extensions) {
        this.schema = schema;
        this.descriptionKey = descriptionKey;
        this.mainExt = ext;
        this.isDefault = true;
        if(extensions == null) {
            this.extensions = DataUtils.EMPTY_SET;
        } else {
            Set set =
                new TreeSet(Comparators.caseInsensitiveStringComparator());
            set.addAll(Arrays.asList(extensions));
            this.extensions = set;
        }
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
        String suffix = filename.substring(j+1);

        // Match with extensions.
        return extensions.contains(suffix);
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
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    /**
     * Returns the MIME-Type of this.
     */
    public String getMimeType() {
        return schema;
    }
    
    /**
     * Returns the probable extension for this media type.
     */
    public String getExtension() {
        return mainExt;
    }
    
    /**
     * Determines whether or not this is a default media type.
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * Returns all default media types.
     */
    public static final MediaType[] getDefaultMediaTypes() {
        return ALL_MEDIA_TYPES;
    }
    
    /**
     * Retrieves the media type for the specified schema's description.
     */
    public static MediaType getMediaTypeForSchema(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (schema.equals(ALL_MEDIA_TYPES[i].schema))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Retrieves the media type for the specified extension.
     */
    public static MediaType getMediaTypeForExtension(String ext) {
        for(int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if(ALL_MEDIA_TYPES[i].extensions.contains(ext))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Determines whether or not the specified schema is a default.
     */
    public static boolean isDefaultType(String schema) {
        final MediaType[] types = getDefaultMediaTypes();
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (schema.equals(ALL_MEDIA_TYPES[i].schema))
                return true;
        return false;
    }
    
    /**
     * Retrieves the audio media type.
     */
    public static MediaType getAudioMediaType() {
        return TYPE_AUDIO;
    }
    
    /**
     * Retrieves the video media type.
     */
    public static MediaType getVideoMediaType() {
        return TYPE_VIDEO;
    }
    
    /**
     * Retrieves the image media type.
     */
    public static MediaType getImageMediaType() {
        return TYPE_IMAGES;
    }
}
