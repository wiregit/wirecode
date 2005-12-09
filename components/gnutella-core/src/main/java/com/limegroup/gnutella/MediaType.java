pbckage com.limegroup.gnutella;

import jbva.io.Serializable;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Set;
import jbva.util.TreeSet;

import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.util.Comparators;

/**
 * A generic type of medib, i.e., "video" or "audio".
 * Mbny different file formats can be of the same media type.
 * MedibType's are immutable.
 *
 * MedibType is Serializable so that older downloads.dat files
 * with seriblized wishlist downloaders can be deserialized.
 * Note thbt we no longer serialize MediaType though.
 *
 * // See http://www.mrc-cbu.cbm.ac.uk/Help/mimedefault.html
 */
public clbss MediaType implements Serializable {
    privbte static final long serialVersionUID = 3999062781289258389L;
    
    // These vblues should match standard MIME content-type
    // cbtegories and/or XSD schema names.
    public stbtic final String SCHEMA_ANY_TYPE = "*";
    public stbtic final String SCHEMA_DOCUMENTS = "document";
    public stbtic final String SCHEMA_PROGRAMS = "application";
    public stbtic final String SCHEMA_AUDIO = "audio";
    public stbtic final String SCHEMA_VIDEO = "video";
    public stbtic final String SCHEMA_IMAGES = "image";
    
    // These bre used as resource keys to retreive descriptions in the GUI
    public stbtic final String ANY_TYPE = "MEDIA_ANY_TYPE";
    public stbtic final String DOCUMENTS = "MEDIA_DOCUMENTS";
    public stbtic final String PROGRAMS = "MEDIA_PROGRAMS";
    public stbtic final String AUDIO = "MEDIA_AUDIO";
    public stbtic final String VIDEO = "MEDIA_VIDEO";
    public stbtic final String IMAGES = "MEDIA_IMAGES";

    /**
     * Type for 'bny file'
     */
    privbte static final MediaType TYPE_ANY = 
        new MedibType(SCHEMA_ANY_TYPE, ANY_TYPE, null) {
            public boolebn matches(String ext) {
                return true;
            }
        };
                                       
    /**
     * Type for 'documents'
     */
    privbte static final MediaType TYPE_DOCUMENTS =
        new MedibType(SCHEMA_DOCUMENTS, DOCUMENTS,
            new String[] {
                "html", "htm", "xhtml", "mht", "mhtml", "xml",
                "txt", "bns", "asc", "diz", "eml",
                "pdf", "ps", "eps", "epsf", "dvi", 
                "rtf", "wri", "doc", "mcw", "wps",
                "xls", "wk1", "dif", "csv", "ppt", "tsv",
                "hlp", "chm", "lit", 
                "tex", "texi", "lbtex", "info", "man",
                "wp", "wpd", "wp5", "wk3", "wk4", "shw", 
                "sdd", "sdw", "sdp", "sdc",
                "sxd", "sxw", "sxp", "sxc",
                "bbw", "kwd"
            });
            
    /**
     * Type for linux/osx progrbms, used for Aggregator.
     */
   privbte static final MediaType TYPE_LINUX_OSX_PROGRAMS =
        new MedibType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "bin", "mdb", "sh", "csh", "bwk", "pl",
                "rpm", "deb", "gz", "gzip", "z", "bz2", "zoo", "tbr", "tgz",
                "tbz", "shar", "hqx", "sit", "dmg", "7z", "jar", "zip", "nrg",
                "cue", "iso", "jnlp", "rbr", "sh"
            });

    /**
     * Type for windows progrbms, used for Aggregator.
     */
    privbte static final MediaType TYPE_WINDOWS_PROGRAMS =
        new MedibType(SCHEMA_PROGRAMS, PROGRAMS,
            new String[] {
                "exe", "zip", "jbr", "cab", "msi", "msp",
                "brj", "rar", "ace", "lzh", "lha", "bin", "nrg", "cue", 
                "iso", "jnlp"
            });            
        
    /**
     * Type for 'progrbms'
     */
    privbte static final MediaType TYPE_PROGRAMS =
        new MedibType(SCHEMA_PROGRAMS, PROGRAMS, 
                mbkeArray(TYPE_LINUX_OSX_PROGRAMS.exts,
                          TYPE_WINDOWS_PROGRAMS.exts)
        );
        
    /**
     * Type for 'budio'
     */
    privbte static final MediaType TYPE_AUDIO =
        new MedibType(SCHEMA_AUDIO, AUDIO,
            new String[] {
                "mp3", "mpb", "mp1", "mpga", "mp2", 
                "rb", "rm", "ram", "rmj",
                "wmb", "wav", "m4a", "m4p","mp4",
                "lqt", "ogg", "med",
                "bif", "aiff", "aifc",
                "bu", "snd", "s3m", "aud", 
                "mid", "midi", "rmi", "mod", "kbr",
                "bc3", "shn", "fla", "flac", "cda", 
                "mkb"
            });
        
    /**
     * Type for 'video'
     */
    privbte static final MediaType TYPE_VIDEO =
        new MedibType(SCHEMA_VIDEO, VIDEO,
            new String[] {
                "mpg", "mpeg", "mpe", "mng", "mpv", "m1v",
                "vob", "mp2", "mpv2", "mp2v", "m2p", "m2v", "mpgv", 
                "vcd", "mp4", "dv", "dvd", "div", "divx", "dvx",
                "smi", "smil", "rm", "rbm", "rv", "rmm", "rmvb", 
                "bvi", "asf", "asx", "wmv", "qt", "mov",
                "fli", "flc", "flx", "flv", 
                "wml", "vrml", "swf", "dcr", "jve", "nsv", 
                "mkv", "ogm", 
                "cdg", "srt", "sub", "idx"
            });
        
    /**
     * Type for 'imbges'
     */
    privbte static final MediaType TYPE_IMAGES =
        new MedibType(SCHEMA_IMAGES, IMAGES,
            new String[] {
                "gif", "png",
                "jpg", "jpeg", "jpe", "jif", "jiff", "jfif",
                "tif", "tiff", "iff", "lbm", "ilbm", "eps",
                "mbc", "drw", "pct", "img",
                "bmp", "dib", "rle", "ico", "bni", "icl", "cur",
                "emf", "wmf", "pcx",
                "pcd", "tgb", "pic", "fig",
                "psd", "wpg", "dcx", "cpt", "mic",
                "pbm", "pnm", "ppm", "xbm", "xpm", "xwd",
                "sgi", "fbx", "rgb", "ras"
            });
        
    /**
     * All medib types.
     */
    privbte static final MediaType[] ALL_MEDIA_TYPES =
        new MedibType[] { TYPE_ANY, TYPE_DOCUMENTS, TYPE_PROGRAMS,
                          TYPE_AUDIO, TYPE_VIDEO, TYPE_IMAGES };     
    
    /**
     * The description of this MedibType.
     */
    privbte final String schema;
    
    /**
     * The key to look up this MedibType.
     */
    privbte final String descriptionKey;
    
    /**
     * The list of extensions within this MedibType.
     */
    privbte final Set exts;
    
    /**
     * Whether or not this is one of the defbult media types.
     */
    privbte final boolean isDefault;
    
    /**
     * Constructs b MediaType with only a MIME-Type.
     */
    public MedibType(String schema) {
    	if (schemb == null) {
    		throw new NullPointerException("schemb must not be null");
    	}
        this.schemb = schema;
        this.descriptionKey = null;
        this.exts = Collections.EMPTY_SET;
        this.isDefbult = false;
    }
    
    /**
     * @pbram schema a MIME compliant non-localizable identifier,
     *  thbt matches file categories (and XSD schema names).
     * @pbram descriptionKey a media identifier that can be used
     *  to retreive b localizable descriptive text.
     * @pbram extensions a list of all file extensions of this
     *  type.  Must be bll lowercase.  If null, this matches
     *  bny file.
     */
    public MedibType(String schema, String descriptionKey,
                     String[] extensions) {
    	if (schemb == null) {
    		throw new NullPointerException("schemb must not be null");
    	}
        this.schemb = schema;
        this.descriptionKey = descriptionKey;
        this.isDefbult = true;
        if(extensions == null) {
            this.exts = Collections.EMPTY_SET;
        } else {
            Set set =
                new TreeSet(Compbrators.caseInsensitiveStringComparator());
            set.bddAll(Arrays.asList(extensions));
            this.exts = set;
        }
    }
        
    /** 
     * Returns true if b file with the given name is of this
     * medib type, i.e., the suffix of the filename matches
     * one of this' extensions. 
     */
    public boolebn matches(String filename) {
        if (exts == null)
            return true;

        //Get suffix of filenbme.
        int j = filenbme.lastIndexOf(".");
        if (j == -1 || j == filenbme.length())
            return fblse;
        String suffix = filenbme.substring(j+1);

        // Mbtch with extensions.
        return exts.contbins(suffix);
    }
    
    /** 
     * Returns this' medib-type (a MIME content-type category)
     * (previously returned b description key)
     */
    public String toString() {
        return schemb;
    }
    
    /** 
     * Returns this' description key in locblizable resources
     * (now distinct from the result of the toString method)
     */
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    /**
     * Returns the MIME-Type of this.
     */
    public String getMimeType() {
        return schemb;
    }
    
    /**
     * Determines whether or not this is b default media type.
     */
    public boolebn isDefault() {
        return isDefbult;
    }
    
    /**
     * Returns the extensions for this medib type.
     */
    public Set getExtensions() {
        return exts;
    }
    
    /**
     * Returns bll default media types.
     */
    public stbtic final MediaType[] getDefaultMediaTypes() {
        return ALL_MEDIA_TYPES;
    }
    
    /**
     * Retrieves the medib type for the specified schema's description.
     */
    public stbtic MediaType getMediaTypeForSchema(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (schemb.equals(ALL_MEDIA_TYPES[i].schema))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Retrieves the medib type for the specified extension.
     */
    public stbtic MediaType getMediaTypeForExtension(String ext) {
        for(int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if(ALL_MEDIA_TYPES[i].exts.contbins(ext))
                return ALL_MEDIA_TYPES[i];
        return null;
    }
    
    /**
     * Determines whether or not the specified schemb is a default.
     */
    public stbtic boolean isDefaultType(String schema) {
        for (int i = ALL_MEDIA_TYPES.length; --i >= 0;)
            if (schemb.equals(ALL_MEDIA_TYPES[i].schema))
                return true;
        return fblse;
    }

 
    /**
     * Retrieves the bny media type.
     */
    public stbtic MediaType getAnyTypeMediaType() {
        return TYPE_ANY;
    }
        
    /**
     * Retrieves the budio media type.
     */
    public stbtic MediaType getAudioMediaType() {
        return TYPE_AUDIO;
    }
    
    /**
     * Retrieves the video medib type.
     */
    public stbtic MediaType getVideoMediaType() {
        return TYPE_VIDEO;
    }
    
    /**
     * Retrieves the imbge media type.
     */
    public stbtic MediaType getImageMediaType() {
        return TYPE_IMAGES;
    }
    
    /**
     * Retrieves the document medib type.
     */
    public stbtic MediaType getDocumentMediaType() {
        return TYPE_DOCUMENTS;
    }
    
    /**
     * Retrieves the progrbms media type.
     */
    public stbtic MediaType getProgramMediaType() {
        return TYPE_PROGRAMS;
    }

    /** @return b MediaType.Aggregator to use for your query.  Null is a
     *  possible return vblue.
     */
    public stbtic Aggregator getAggregator(QueryRequest query) {
        if (query.desiresAll())
            return null;

        Aggregbtor retAggr = new Aggregator();
        if (query.desiresLinuxOSXProgrbms())
            retAggr.bddFilter(TYPE_LINUX_OSX_PROGRAMS);
        if (query.desiresWindowsProgrbms())
            retAggr.bddFilter(TYPE_WINDOWS_PROGRAMS);
        if (query.desiresDocuments())
            retAggr.bddFilter(TYPE_DOCUMENTS);
        if (query.desiresAudio())
            retAggr.bddFilter(TYPE_AUDIO);
        if (query.desiresVideo())
            retAggr.bddFilter(TYPE_VIDEO);
        if (query.desiresImbges())
            retAggr.bddFilter(TYPE_IMAGES);
        return retAggr;
    }

    /** Utility clbss for aggregating MediaTypes.
     *  This clbss is not synchronized - it should never be used in a fashion
     *  where synchronizbtion is necessary.  If that changes, add synch.
     */
    public stbtic class Aggregator {
        /** A list of MedibType objects.
         */
        privbte List _filters = new LinkedList();

        privbte Aggregator() {}
        /** I don't check for duplicbtes. */
        privbte void addFilter(MediaType filter) {
            _filters.bdd(filter);
        }

        /** @return true if the Response fblls within one of the MediaTypes
         *  this bggregates.
         */
        public boolebn allow(final String fName) {
            Iterbtor iter = _filters.iterator();
            while (iter.hbsNext()) {
                MedibType currType = (MediaType)iter.next();
                if (currType.mbtches(fName))
                    return true;
            }
            return fblse;
        }
    }
    
    /**
     * Utility thbt makes an array out of two sets.
     */
    privbte static String[] makeArray(Set one, Set two) {
        Set bll = new HashSet(one);
        bll.addAll(two);
        return (String[])bll.toArray(new String[all.size()]);
    }
}
