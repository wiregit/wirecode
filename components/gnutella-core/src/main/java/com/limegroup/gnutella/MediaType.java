package com.limegroup.gnutella;

/**
 * A generic type of media, i.e., "Video" or "Audio".
 * Many different file formats can be of the same media type.
 * MediaType's are immutable.
 */
public class MediaType {
    private String description;
    private String[] extensions;
    private final static String ANY_TYPE = "MEDIA_ANY_TYPE";
    private final static String DOCUMENTS = "MEDIA_DOCUMENTS";
    private final static String PROGRAMS = "MEDIA_PROGRAMS";
    private final static String AUDIO = "MEDIA_AUDIO";
    private final static String VIDEO = "MEDIA_VIDEO";
    private final static String IMAGES = "MEDIA_IMAGES";



    /**
     * @param a human readable description of this media type,
     *  i.e., "Hypertext Documents"
     * @param extensions a list of all file extensions of this
     *  type.  Must be all lowercase.  If null, this matches
     *  any file.
     */
    public MediaType(String description, String[] extensions) {
		this.description=description;
		this.extensions=extensions;
    }

    /** 
     * Returns true if a file with the given name is of this
     * media type, i.e., the suffix of the filename matches
     * one of this' extensions. 
     */
    public boolean matches(String filename) {
		if (extensions==null)
			return true;

		//Get suffix of filename.
		int j=filename.lastIndexOf(".");
		if (j==-1 || j==filename.length())
			return false;
		String suffix=filename.substring(j+1).toLowerCase();
	
		//Match with extensions.
		for (int i=0; i<extensions.length; i++) {	    
			if (suffix.equals(extensions[i]))
				return true;
		}
		return false;
    }
			
    /** 
        Returns this' human-readable description
    */
    public String toString() {
        return description;
    }
    
    private static MediaType[] allMediaTypes = null;
    public static MediaType[] getDefaultMediaTypes() {
        if (allMediaTypes == null)
            allMediaTypes = getTypes();
        return allMediaTypes;
    }

    public static MediaType getAudioMediaType() {
        return (getDefaultMediaTypes())[3];
    }
    public static MediaType getVideoMediaType() {
        return (getDefaultMediaTypes())[4];
    }

    /** Returns an array of default media types. */
    private static MediaType[] getTypes() {
		MediaType any=new MediaType(ANY_TYPE, null);
		//This list isn't exhaustive, but it's not clear that we want it
		//to.  We may only want the most common file types here.
		//See http://www.mrc-cbu.cam.ac.uk/Help/mimedefault.html
		MediaType text=new MediaType(DOCUMENTS,
									 new String[] {"html", "htm", "xml", 
												   "txt", "pdf", "ps",
												   "rtf", "doc", "tex"});
		MediaType programs=new MediaType(PROGRAMS,
										 new String[] {"exe", "zip", "gz", "gzip",
													   "hqx", "tar", "tgz", "z",
										               "sit", "hqx", "bin",
										               "dmg"});
		MediaType audio=new MediaType(AUDIO, 
									  new String[] {"mp3", "wav", "au", 
													"aif", "aiff", "ra", 
													"ram", "rmj", "lqt", "ogg"});
		MediaType video=new MediaType(VIDEO, 
									  new String[] {"mpg", "mpeg", "asf", "qt", 
													"mov", "avi", "mpe", "swf", "dcr",
									                "rm", "ram", "jve"});
		MediaType images=new MediaType(IMAGES,
									   new String[] {"gif", "jpg", "jpeg", "jpe",
													 "png", "tif", "tiff"});	

        //Added by Sumeet Thadani to allow a rich search window to be popped up.
		return new MediaType[] {any, text, programs, audio, video, images};
    }		    

	//      public static void main(String args[]) {
	//  	MediaType[] types=getDefaultMediaTypes();
	//  	MediaType mt;

	//  	mt=types[0];
	//  	Assert.that(mt.matches("foo.jpg"));
	//  	Assert.that(mt.matches("foo"));
	//  	Assert.that(mt.matches(""));

	//  	mt=types[1];
	//  	Assert.that(mt.toString().equals("Documents"));
	//  	Assert.that(mt.matches("foo.html"));
	//  	Assert.that(mt.matches("foo.HTML"));
	//  	Assert.that(mt.matches("foo.ps"));
	//  	Assert.that(mt.matches("foo.PS"));
	//  	Assert.that(! mt.matches("foo.jpg"));
	//  	Assert.that(! mt.matches("foo"));	
	//  	Assert.that(! mt.matches("foo."));	
	//      }
}
