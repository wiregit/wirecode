package com.limegroup.gnutella;

/**
 * A generic type of media, i.e., "Video" or "Audio".
 * Many different file formats can be of the same media type.
 * MediaType's are immutable.
 */
public class MediaType {
    private String description;
    private String[] extensions;

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
			
    /** Returns this' human-readable description */
    public String toString() {
		return description;
    }

    /** Returns an array of default media types. */
    public static MediaType[] getDefaultMediaTypes() {
		MediaType any=new MediaType("Any Type", null);
		//This list isn't exhaustive, but it's not clear that we want it
		//to.  We may only want the most common file types here.
		//See http://www.mrc-cbu.cam.ac.uk/Help/mimedefault.html
		MediaType text=new MediaType("Documents",
									 new String[] {"html", "htm", "xml", 
												   "txt", "pdf", "ps",
												   "rtf", "doc", "tex"});
		MediaType programs=new MediaType("Programs",
										 new String[] {"exe", "zip", "gz", "gzip",
													   "hqx", "tar", "tgz", "z"});
		MediaType audio=new MediaType("Audio", 
									  new String[] {"mp3", "wav", "au", 
													"aif", "aiff", "ra", "ram"});
		MediaType video=new MediaType("Video", 
									  new String[] {"mpg", "mpeg", "asf", "qt", 
													"mov", "avi", "mpe", "swf", "dcr"});
		MediaType images=new MediaType("Images",
									   new String[] {"gif", "jpg", "jpeg", "jpe",
													 "png", "tif", "tiff"});	
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
