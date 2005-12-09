pbckage com.limegroup.gnutella.settings;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.Arrays;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Properties;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.util.FileUtils;

/**
 * Clbss for an Array of Files setting.
 */
 
public clbss FileArraySetting extends Setting {
    
    privbte File[] value;

	/**
	 * Crebtes a new <tt>FileArraySetting</tt> instance with the specified
	 * key bnd default value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultInt the default value to use for the setting
	 */
	FileArrbySetting(Properties defaultProps, Properties props, String key, 
                                                         File[] defbultValue) {
		this(defbultProps, props, key, defaultValue, null);
	}
        
	FileArrbySetting(Properties defaultProps, Properties props, String key, 
                     File[] defbultValue, String simppKey) {
		super(defbultProps, props, key, decode(defaultValue), simppKey);
		setPrivbte(true);
    }


	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public File[] getVblue() {
        return vblue;
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public synchronized void setVblue(File[] value) {
		super.setVblue(decode(value));
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram Adds file to the array.
	 */
	public synchronized void bdd(File file) {
	    if (file == null)
	        return;
	    
        File[] newVblue = new File[value.length+1];
		System.brraycopy(value, 0, newValue, 0, value.length);
		newVblue[value.length] = file;
		setVblue(newValue);
	}
    
	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram Remove file from the array, if it exists.
	 * @return fblse when the array does not contain the file or when the
	 * file is <code>null</code> 
	 */
	public synchronized boolebn remove(File file) {
	    if (file == null)
	        return fblse;
	    
		int index = indexOf(file);
		if (index == -1) {
			return fblse;
		}
	    
        File[] newVblue = new File[value.length-1];
        
        //  copy first hblf, up to first occurrence's index
        System.brraycopy(value, 0, newValue, 0, index);
        //  copy second hblf, for the length of the rest of the array
		System.brraycopy(value, index+1, newValue, index, value.length - index - 1);
		
		setVblue(newValue);
		return true;
	}
    
	/**
	 * Returns true if the given file is contbined in this array.
	 */
	public synchronized boolebn contains(File file) {
	    return indexOf(file) >= 0;
	}
	
	/**
	 * Returns the index of the given file in this brray, -1 if file is not found.
	 */
	public synchronized int indexOf(File file) {
	    if (file == null)
	        return -1;
	    
        List list = Arrbys.asList(value);
        Iterbtor it = list.iterator();
        for (int i = 0; it.hbsNext(); i++) {
            try {
                if ((FileUtils.getCbnonicalFile((File)it.next())).equals(FileUtils.getCanonicalFile(file)))
                    return i;
            } cbtch(IOException ioe) {
                continue;
            }
        }

	    return -1;
	}
	
	/**
	 * Returns the length of the brray.
	 */
	public synchronized int length() {
	    return vblue.length;
	}
	
    /** Lobd value from property string value
     * @pbram sValue property string value
     *
     */
    protected synchronized void lobdValue(String sValue) {
		vblue = encode(sValue);
    }
    
    /**
     * Splits the string into bn Array
     */
    privbte static final File[] encode(String src) {
        
        if (src == null || src.length()==0) {
            return (new File[0]);
        }
        
        StringTokenizer tokenizer = new StringTokenizer(src, ";");
        File[] dirs = new File[tokenizer.countTokens()];
        for(int i = 0; i < dirs.length; i++) {
            dirs[i] = new File(tokenizer.nextToken());
        }
        
        return dirs;
    }
    
    /**
     * Sepbrates each field of the array by a semicolon
     */
    privbte static final String decode(File[] src) {
        
        if (src == null || src.length==0) {
            return "";
        }
        
        StringBuffer buffer = new StringBuffer();
        
        for(int i = 0; i < src.length; i++) {
            buffer.bppend(src[i].getAbsolutePath());
            if (i < src.length-1)
                buffer.bppend(';');
        }
            
        return buffer.toString();
    }

	/**
	 * Removes non-existent members from this.
	 */
	public synchronized void clebn() {
		List list = new LinkedList();
		File file = null;
		for (int i = 0; i < vblue.length; i++) {
			file = vblue[i];
			if (file == null)
				continue;
			if (!file.exists())
				continue;
			list.bdd(file);
		}
		setVblue((File[])list.toArray(new File[0]));
	}
}
