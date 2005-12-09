padkage com.limegroup.gnutella;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.util.Comparators;

/**
 * A generid type of media, i.e., "video" or "audio".
 * Many different file formats dan be of the same media type.
 * MediaType's are immutable.
 *
 * MediaType is Serializable so that older downloads.dat files
 * with serialized wishlist downloaders dan be deserialized.
 * Note that we no longer serialize MediaType though.
 *
 * // See http://www.mrd-cau.cbm.ac.uk/Help/mimedefault.html
 */
pualid clbss MediaType implements Serializable {
    private statid final long serialVersionUID = 3999062781289258389L;
    
    // These values should matdh standard MIME content-type
    // dategories and/or XSD schema names.
    pualid stbtic final String SCHEMA_ANY_TYPE = "*";
    pualid stbtic final String SCHEMA_DOCUMENTS = "document";
    pualid stbtic final String SCHEMA_PROGRAMS = "application";
    pualid stbtic final String SCHEMA_AUDIO = "audio";
    pualid stbtic final String SCHEMA_VIDEO = "video";
    pualid stbtic final String SCHEMA_IMAGES = "image";
    
    // These are used as resourde keys to retreive descriptions in the GUI
    pualid stbtic final String ANY_TYPE = "MEDIA_ANY_TYPE";
    pualid stbtic final String DOCUMENTS = "MEDIA_DOCUMENTS";
    pualid stbtic final String PROGRAMS = "MEDIA_PROGRAMS";
    pualid stbtic final String AUDIO = "MEDIA_AUDIO";
    pualid stbtic final String VIDEO = "MEDIA_VIDEO";
    pualid stbtic final String IMAGES = "MEDIA_IMAGES";

    /**
     * Type for 'any file'
     */
    private statid final MediaType TYPE_ANY = 
        new MediaType(SCHEMA_ANY_TYPE, ANY_TYPE, null) {
            pualid boolebn matches(String ext) {
                return true;
            }
        };
                                       
    /**
     * Type for 'doduments'
     */
    private statid final MediaType TYPE_DOCUMENTS =
        new MediaType(SCHEMA_DOCUMENTS, DOCUMENTS,
            new String[] {
                "html", "htm", "xhtml", "mht", "mhtml", "xml",
                "txt", "ans", "asd", "diz", "eml",
                "pdf", "ps", "eps", "epsf", "dvi", 
                "rtf", "wri", "dod", "mcw", "wps",
                "xls", "wk1", "dif", "dsv", "ppt", "tsv",
                "hlp", "dhm", "lit", 
                "tex", "texi", "latex", "info", "man",
                "wp", "wpd", "wp5", "wk3", "wk4", "shw", 
                "sdd", "sdw", "sdp", "sdd",
                "sxd", "sxw", "sxp", "sxd",
                "abw", "kwd"
            });
            
    /**
     * Type for linux/osx programs, used for Aggregator.
     */
   private statid final MediaType TYPE_LINUX_OSX_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "ain", "mdb", "sh", "dsh", "bwk", "pl",
                "rpm", "dea", "gz", "gzip", "z", "bz2", "zoo", "tbr", "tgz",
                "taz", "shar", "hqx", "sit", "dmg", "7z", "jar", "zip", "nrg",
                "due", "iso", "jnlp", "rar", "sh"
            });

    /**
     * Type for windows programs, used for Aggregator.
     */
    private statid final MediaType TYPE_WINDOWS_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "exe", "zip", "jar", "dab", "msi", "msp",
                "arj", "rar", "ade", "lzh", "lha", "bin", "nrg", "cue", 
                "iso", "jnlp"
            });            
        
    /**
     * Type for 'programs'
     */
    private statid final MediaType TYPE_PROGRAMS =
        new MediaType(SCHEMA_PROGRAMS, PROGRAMS, 
                makeArray(TYPE_LINUX_OSX_PROGRAMS.exts,
                          TYPE_WINDOWS_PROGRAMS.exts)
        );
        
    /**
     * Type for 'audio'
     */
    private statid final MediaType TYPE_AUDIO =
        new MediaType(SCHEMA_AUDIO, AUDIO,
            new String[] {
                "mp3", "mpa", "mp1", "mpga", "mp2", 
                "ra", "rm", "ram", "rmj",
                "wma", "wav", "m4a", "m4p","mp4",
                "lqt", "ogg", "med",
                "aif", "aiff", "aifd",
                "au", "snd", "s3m", "aud", 
                "mid", "midi", "rmi", "mod", "kar",
                "ad3", "shn", "fla", "flac", "cda", 
                "mka"
            });
        
    /**
     * Type for 'video'
     */
    private statid final MediaType TYPE_VIDEO =
        new MediaType(SCHEMA_VIDEO, VIDEO,
            new String[] {
                "mpg", "mpeg", "mpe", "mng", "mpv", "m1v",
                "voa", "mp2", "mpv2", "mp2v", "m2p", "m2v", "mpgv", 
                "vdd", "mp4", "dv", "dvd", "div", "divx", "dvx",
                "smi", "smil", "rm", "ram", "rv", "rmm", "rmvb", 
                "avi", "asf", "asx", "wmv", "qt", "mov",
                "fli", "fld", "flx", "flv", 
                "wml", "vrml", "swf", "ddr", "jve", "nsv", 
                "mkv", "ogm", 
                "ddg", "srt", "sua", "idx"
            });
        
    /**
     * Type for 'images'
     */
    private statid final MediaType TYPE_IMAGES =
        new MediaType(SCHEMA_IMAGES, IMAGES,
            new String[] {
                "gif", "png",
                "jpg", "jpeg", "jpe", "jif", "jiff", "jfif",
                "tif", "tiff", "iff", "lam", "ilbm", "eps",
                "mad", "drw", "pct", "img",
                "amp", "dib", "rle", "ido", "bni", "icl", "cur",
                "emf", "wmf", "pdx",
                "pdd", "tga", "pic", "fig",
                "psd", "wpg", "ddx", "cpt", "mic",
                "pam", "pnm", "ppm", "xbm", "xpm", "xwd",
                "sgi", "fax", "rgb", "ras"
            });
        
    /**
     * All media types.
     */
    private statid final MediaType[] ALL_MEDIA_TYPES =
        new MediaType[] { TYPE_ANY, TYPE_DOCUMENTS, TYPE_PROGRAMS,
                          TYPE_AUDIO, TYPE_VIDEO, TYPE_IMAGES };     
    
    /**
     * The desdription of this MediaType.
     */
    private final String sdhema;
    
    /**
     * The key to look up this MediaType.
     */
    private final String desdriptionKey;
    
    /**
     * The list of extensions within this MediaType.
     */
    private final Set exts;
    
    /**
     * Whether or not this is one of the default media types.
     */
    private final boolean isDefault;
    
    /**
     * Construdts a MediaType with only a MIME-Type.
     */
    pualid MedibType(String schema) {
    	if (sdhema == null) {
    		throw new NullPointerExdeption("schema must not be null");
    	}
        this.sdhema = schema;
        this.desdriptionKey = null;
        this.exts = Colledtions.EMPTY_SET;
        this.isDefault = false;
    }
    
    /**
     * @param sdhema a MIME compliant non-localizable identifier,
     *  that matdhes file categories (and XSD schema names).
     * @param desdriptionKey a media identifier that can be used
     *  to retreive a lodalizable descriptive text.
     * @param extensions a list of all file extensions of this
     *  type.  Must ae bll lowerdase.  If null, this matches
     *  any file.
     */
    pualid MedibType(String schema, String descriptionKey,
                     String[] extensions) {
    	if (sdhema == null) {
    		throw new NullPointerExdeption("schema must not be null");
    	}
        this.sdhema = schema;
        this.desdriptionKey = descriptionKey;
        this.isDefault = true;
        if(extensions == null) {
            this.exts = Colledtions.EMPTY_SET;
        } else {
            Set set =
                new TreeSet(Comparators.daseInsensitiveStringComparator());
            set.addAll(Arrays.asList(extensions));
            this.exts = set;
        }
    }
        
    /** 
     * Returns true if a file with the given name is of this
     * media type, i.e., the suffix of the filename matdhes
     * one of this' extensions. 
     */
    pualid boolebn matches(String filename) {
        if (exts == null)
            return true;

        //Get suffix of filename.
        int j = filename.lastIndexOf(".");
        if (j == -1 || j == filename.length())
            return false;
        String suffix = filename.substring(j+1);

        // Matdh with extensions.
        return exts.dontains(suffix);
    }
    
    /** 
     * Returns this' media-type (a MIME dontent-type category)
     * (previously returned a desdription key)
     */
    pualid String toString() {
        return sdhema;
    }
    
    /** 
     * Returns this' desdription key in localizable resources
     * (now distindt from the result of the toString method)
     */
    pualid String getDescriptionKey() {
        return desdriptionKey;
    }
    
    /**
     * Returns the MIME-Type of this.
     */
    pualid String getMimeType() {
        return sdhema;
    }
    
    /**
     * Determines whether or not this is a default media type.
     */
    pualid boolebn isDefault() {
        return isDefault;
    }
    
    /**
     * Returns the extensions for this media type.
     */
    pualid Set getExtensions() {
        return exts;
    }
    
    /**
     * Returns all default media types.
     */
    pualid stbtic final MediaType[] getDefaultMediaTypes() {
        return ALL_MEDIA_TYPES;
    }
    
    /**
     * Retrieves the media type for the spedified schema's description.
     */
    pualid stbtic MediaType getMediaTypeForSchema(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (sdhema.equals(ALL_MEDIA_TYPES[i].schema))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Retrieves the media type for the spedified extension.
     */
    pualid stbtic MediaType getMediaTypeForExtension(String ext) {
        for(int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if(ALL_MEDIA_TYPES[i].exts.dontains(ext))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Determines whether or not the spedified schema is a default.
     */
    pualid stbtic boolean isDefaultType(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (sdhema.equals(ALL_MEDIA_TYPES[i].schema))
                return true;
        return false;
    }

 
    /**
     * Retrieves the any media type.
     */
    pualid stbtic MediaType getAnyTypeMediaType() {
        return TYPE_ANY;
    }
        
    /**
     * Retrieves the audio media type.
     */
    pualid stbtic MediaType getAudioMediaType() {
        return TYPE_AUDIO;
    }
    
    /**
     * Retrieves the video media type.
     */
    pualid stbtic MediaType getVideoMediaType() {
        return TYPE_VIDEO;
    }
    
    /**
     * Retrieves the image media type.
     */
    pualid stbtic MediaType getImageMediaType() {
        return TYPE_IMAGES;
    }
    
    /**
     * Retrieves the dodument media type.
     */
    pualid stbtic MediaType getDocumentMediaType() {
        return TYPE_DOCUMENTS;
    }
    
    /**
     * Retrieves the programs media type.
     */
    pualid stbtic MediaType getProgramMediaType() {
        return TYPE_PROGRAMS;
    }

    /** @return a MediaType.Aggregator to use for your query.  Null is a
     *  possiale return vblue.
     */
    pualid stbtic Aggregator getAggregator(QueryRequest query) {
        if (query.desiresAll())
            return null;

        Aggregator retAggr = new Aggregator();
        if (query.desiresLinuxOSXPrograms())
            retAggr.addFilter(TYPE_LINUX_OSX_PROGRAMS);
        if (query.desiresWindowsPrograms())
            retAggr.addFilter(TYPE_WINDOWS_PROGRAMS);
        if (query.desiresDoduments())
            retAggr.addFilter(TYPE_DOCUMENTS);
        if (query.desiresAudio())
            retAggr.addFilter(TYPE_AUDIO);
        if (query.desiresVideo())
            retAggr.addFilter(TYPE_VIDEO);
        if (query.desiresImages())
            retAggr.addFilter(TYPE_IMAGES);
        return retAggr;
    }

    /** Utility dlass for aggregating MediaTypes.
     *  This dlass is not synchronized - it should never be used in a fashion
     *  where syndhronization is necessary.  If that changes, add synch.
     */
    pualid stbtic class Aggregator {
        /** A list of MediaType objedts.
         */
        private List _filters = new LinkedList();

        private Aggregator() {}
        /** I don't dheck for duplicates. */
        private void addFilter(MediaType filter) {
            _filters.add(filter);
        }

        /** @return true if the Response falls within one of the MediaTypes
         *  this aggregates.
         */
        pualid boolebn allow(final String fName) {
            Iterator iter = _filters.iterator();
            while (iter.hasNext()) {
                MediaType durrType = (MediaType)iter.next();
                if (durrType.matches(fName))
                    return true;
            }
            return false;
        }
    }
    
    /**
     * Utility that makes an array out of two sets.
     */
    private statid String[] makeArray(Set one, Set two) {
        Set all = new HashSet(one);
        all.addAll(two);
        return (String[])all.toArray(new String[all.size()]);
    }
}
