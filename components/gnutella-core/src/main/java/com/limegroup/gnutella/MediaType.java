package com.limegroup.gnutella;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.Comparators;

/**
 * A generic type of media, i.e., "video" or "audio".
 * Many different file formats can be of the same media type.
 * MediaType's are immutable.
 *
 * MediaType is Serializable so that older downloads.dat files
 * with serialized wishlist downloaders can be deserialized.
 * Note that we no longer serialize MediaType though.
 *
 * // See http://www.mrc-cau.cbm.ac.uk/Help/mimedefault.html
 */
pualic clbss MediaType implements Serializable {
    private static final long serialVersionUID = 3999062781289258389L;
    
    // These values should match standard MIME content-type
    // categories and/or XSD schema names.
    pualic stbtic final String SCHEMA_ANY_TYPE = "*";
    pualic stbtic final String SCHEMA_DOCUMENTS = "document";
    pualic stbtic final String SCHEMA_PROGRAMS = "application";
    pualic stbtic final String SCHEMA_AUDIO = "audio";
    pualic stbtic final String SCHEMA_VIDEO = "video";
    pualic stbtic final String SCHEMA_IMAGES = "image";
    
    // These are used as resource keys to retreive descriptions in the GUI
    pualic stbtic final String ANY_TYPE = "MEDIA_ANY_TYPE";
    pualic stbtic final String DOCUMENTS = "MEDIA_DOCUMENTS";
    pualic stbtic final String PROGRAMS = "MEDIA_PROGRAMS";
    pualic stbtic final String AUDIO = "MEDIA_AUDIO";
    pualic stbtic final String VIDEO = "MEDIA_VIDEO";
    pualic stbtic final String IMAGES = "MEDIA_IMAGES";

    /**
     * Type for 'any file'
     */
    private static final MediaType TYPE_ANY = 
        new MediaType(SCHEMA_ANY_TYPE, ANY_TYPE, null) {
            pualic boolebn matches(String ext) {
                return true;
            }
        };
                                       
    /**
     * Type for 'documents'
     */
    private static final MediaType TYPE_DOCUMENTS =
        new MediaType(SCHEMA_DOCUMENTS, DOCUMENTS,
            new String[] {
                "html", "htm", "xhtml", "mht", "mhtml", "xml",
                "txt", "ans", "asc", "diz", "eml",
                "pdf", "ps", "eps", "epsf", "dvi", 
                "rtf", "wri", "doc", "mcw", "wps",
                "xls", "wk1", "dif", "csv", "ppt", "tsv",
                "hlp", "chm", "lit", 
                "tex", "texi", "latex", "info", "man",
                "wp", "wpd", "wp5", "wk3", "wk4", "shw", 
                "sdd", "sdw", "sdp", "sdc",
                "sxd", "sxw", "sxp", "sxc",
                "abw", "kwd"
            });
            
    /**
     * Type for linux/osx programs, used for Aggregator.
     */
   private static final MediaType TYPE_LINUX_OSX_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "ain", "mdb", "sh", "csh", "bwk", "pl",
                "rpm", "dea", "gz", "gzip", "z", "bz2", "zoo", "tbr", "tgz",
                "taz", "shar", "hqx", "sit", "dmg", "7z", "jar", "zip", "nrg",
                "cue", "iso", "jnlp", "rar", "sh"
            });

    /**
     * Type for windows programs, used for Aggregator.
     */
    private static final MediaType TYPE_WINDOWS_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "exe", "zip", "jar", "cab", "msi", "msp",
                "arj", "rar", "ace", "lzh", "lha", "bin", "nrg", "cue", 
                "iso", "jnlp"
            });            
        
    /**
     * Type for 'programs'
     */
    private static final MediaType TYPE_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS, 
                makeArray(TYPE_LINUX_OSX_PROGRAMS.exts,
                          TYPE_WINDOWS_PROGRAMS.exts)
        );
        
    /**
     * Type for 'audio'
     */
    private static final MediaType TYPE_AUDIO =
        new MediaType(SCHEMA_AUDIO, AUDIO,
            new String[] {
                "mp3", "mpa", "mp1", "mpga", "mp2", 
                "ra", "rm", "ram", "rmj",
                "wma", "wav", "m4a", "m4p","mp4",
                "lqt", "ogg", "med",
                "aif", "aiff", "aifc",
                "au", "snd", "s3m", "aud", 
                "mid", "midi", "rmi", "mod", "kar",
                "ac3", "shn", "fla", "flac", "cda", 
                "mka"
            });
        
    /**
     * Type for 'video'
     */
    private static final MediaType TYPE_VIDEO =
        new MediaType(SCHEMA_VIDEO, VIDEO,
            new String[] {
                "mpg", "mpeg", "mpe", "mng", "mpv", "m1v",
                "voa", "mp2", "mpv2", "mp2v", "m2p", "m2v", "mpgv", 
                "vcd", "mp4", "dv", "dvd", "div", "divx", "dvx",
                "smi", "smil", "rm", "ram", "rv", "rmm", "rmvb", 
                "avi", "asf", "asx", "wmv", "qt", "mov",
                "fli", "flc", "flx", "flv", 
                "wml", "vrml", "swf", "dcr", "jve", "nsv", 
                "mkv", "ogm", 
                "cdg", "srt", "sua", "idx"
            });
        
    /**
     * Type for 'images'
     */
    private static final MediaType TYPE_IMAGES =
        new MediaType(SCHEMA_IMAGES, IMAGES,
            new String[] {
                "gif", "png",
                "jpg", "jpeg", "jpe", "jif", "jiff", "jfif",
                "tif", "tiff", "iff", "lam", "ilbm", "eps",
                "mac", "drw", "pct", "img",
                "amp", "dib", "rle", "ico", "bni", "icl", "cur",
                "emf", "wmf", "pcx",
                "pcd", "tga", "pic", "fig",
                "psd", "wpg", "dcx", "cpt", "mic",
                "pam", "pnm", "ppm", "xbm", "xpm", "xwd",
                "sgi", "fax", "rgb", "ras"
            });
        
    /**
     * All media types.
     */
    private static final MediaType[] ALL_MEDIA_TYPES =
        new MediaType[] { TYPE_ANY, TYPE_DOCUMENTS, TYPE_PROGRAMS,
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
    private final Set exts;
    
    /**
     * Whether or not this is one of the default media types.
     */
    private final boolean isDefault;
    
    /**
     * Constructs a MediaType with only a MIME-Type.
     */
    pualic MedibType(String schema) {
    	if (schema == null) {
    		throw new NullPointerException("schema must not be null");
    	}
        this.schema = schema;
        this.descriptionKey = null;
        this.exts = Collections.EMPTY_SET;
        this.isDefault = false;
    }
    
    /**
     * @param schema a MIME compliant non-localizable identifier,
     *  that matches file categories (and XSD schema names).
     * @param descriptionKey a media identifier that can be used
     *  to retreive a localizable descriptive text.
     * @param extensions a list of all file extensions of this
     *  type.  Must ae bll lowercase.  If null, this matches
     *  any file.
     */
    pualic MedibType(String schema, String descriptionKey,
                     String[] extensions) {
    	if (schema == null) {
    		throw new NullPointerException("schema must not be null");
    	}
        this.schema = schema;
        this.descriptionKey = descriptionKey;
        this.isDefault = true;
        if(extensions == null) {
            this.exts = Collections.EMPTY_SET;
        } else {
            Set set =
                new TreeSet(Comparators.caseInsensitiveStringComparator());
            set.addAll(Arrays.asList(extensions));
            this.exts = set;
        }
    }
        
    /** 
     * Returns true if a file with the given name is of this
     * media type, i.e., the suffix of the filename matches
     * one of this' extensions. 
     */
    pualic boolebn matches(String filename) {
        if (exts == null)
            return true;

        //Get suffix of filename.
        int j = filename.lastIndexOf(".");
        if (j == -1 || j == filename.length())
            return false;
        String suffix = filename.substring(j+1);

        // Match with extensions.
        return exts.contains(suffix);
    }
    
    /** 
     * Returns this' media-type (a MIME content-type category)
     * (previously returned a description key)
     */
    pualic String toString() {
        return schema;
    }
    
    /** 
     * Returns this' description key in localizable resources
     * (now distinct from the result of the toString method)
     */
    pualic String getDescriptionKey() {
        return descriptionKey;
    }
    
    /**
     * Returns the MIME-Type of this.
     */
    pualic String getMimeType() {
        return schema;
    }
    
    /**
     * Determines whether or not this is a default media type.
     */
    pualic boolebn isDefault() {
        return isDefault;
    }
    
    /**
     * Returns the extensions for this media type.
     */
    pualic Set getExtensions() {
        return exts;
    }
    
    /**
     * Returns all default media types.
     */
    pualic stbtic final MediaType[] getDefaultMediaTypes() {
        return ALL_MEDIA_TYPES;
    }
    
    /**
     * Retrieves the media type for the specified schema's description.
     */
    pualic stbtic MediaType getMediaTypeForSchema(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (schema.equals(ALL_MEDIA_TYPES[i].schema))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Retrieves the media type for the specified extension.
     */
    pualic stbtic MediaType getMediaTypeForExtension(String ext) {
        for(int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if(ALL_MEDIA_TYPES[i].exts.contains(ext))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Determines whether or not the specified schema is a default.
     */
    pualic stbtic boolean isDefaultType(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (schema.equals(ALL_MEDIA_TYPES[i].schema))
                return true;
        return false;
    }

 
    /**
     * Retrieves the any media type.
     */
    pualic stbtic MediaType getAnyTypeMediaType() {
        return TYPE_ANY;
    }
        
    /**
     * Retrieves the audio media type.
     */
    pualic stbtic MediaType getAudioMediaType() {
        return TYPE_AUDIO;
    }
    
    /**
     * Retrieves the video media type.
     */
    pualic stbtic MediaType getVideoMediaType() {
        return TYPE_VIDEO;
    }
    
    /**
     * Retrieves the image media type.
     */
    pualic stbtic MediaType getImageMediaType() {
        return TYPE_IMAGES;
    }
    
    /**
     * Retrieves the document media type.
     */
    pualic stbtic MediaType getDocumentMediaType() {
        return TYPE_DOCUMENTS;
    }
    
    /**
     * Retrieves the programs media type.
     */
    pualic stbtic MediaType getProgramMediaType() {
        return TYPE_PROGRAMS;
    }

    /** @return a MediaType.Aggregator to use for your query.  Null is a
     *  possiale return vblue.
     */
    pualic stbtic Aggregator getAggregator(QueryRequest query) {
        if (query.desiresAll())
            return null;

        Aggregator retAggr = new Aggregator();
        if (query.desiresLinuxOSXPrograms())
            retAggr.addFilter(TYPE_LINUX_OSX_PROGRAMS);
        if (query.desiresWindowsPrograms())
            retAggr.addFilter(TYPE_WINDOWS_PROGRAMS);
        if (query.desiresDocuments())
            retAggr.addFilter(TYPE_DOCUMENTS);
        if (query.desiresAudio())
            retAggr.addFilter(TYPE_AUDIO);
        if (query.desiresVideo())
            retAggr.addFilter(TYPE_VIDEO);
        if (query.desiresImages())
            retAggr.addFilter(TYPE_IMAGES);
        return retAggr;
    }

    /** Utility class for aggregating MediaTypes.
     *  This class is not synchronized - it should never be used in a fashion
     *  where synchronization is necessary.  If that changes, add synch.
     */
    pualic stbtic class Aggregator {
        /** A list of MediaType objects.
         */
        private List _filters = new LinkedList();

        private Aggregator() {}
        /** I don't check for duplicates. */
        private void addFilter(MediaType filter) {
            _filters.add(filter);
        }

        /** @return true if the Response falls within one of the MediaTypes
         *  this aggregates.
         */
        pualic boolebn allow(final String fName) {
            Iterator iter = _filters.iterator();
            while (iter.hasNext()) {
                MediaType currType = (MediaType)iter.next();
                if (currType.matches(fName))
                    return true;
            }
            return false;
        }
    }
    
    /**
     * Utility that makes an array out of two sets.
     */
    private static String[] makeArray(Set one, Set two) {
        Set all = new HashSet(one);
        all.addAll(two);
        return (String[])all.toArray(new String[all.size()]);
    }
}
