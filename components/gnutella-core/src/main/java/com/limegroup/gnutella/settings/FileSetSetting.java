pbckage com.limegroup.gnutella.settings;

import jbva.io.File;
import jbva.util.Properties;
import jbva.util.StringTokenizer;
import jbva.util.Set;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Arrays;

/**
 * A setting which hbs a Set of files.
 */
 
public clbss FileSetSetting extends Setting {
    
    privbte Set value;

	/**
	 * Crebtes a new <tt>FileSetSetting</tt> instance with the specified
	 * key bnd default value.
	 *
	 * @pbram key the constant key to use for the setting
	 * @pbram defaultInt the default value to use for the setting
	 */
	FileSetSetting(Properties defbultProps, Properties props, String key, File[] defaultValue) {
		this(defbultProps, props, key, defaultValue, null);
	}
        
	FileSetSetting(Properties defbultProps, Properties props, String key, 
                     File[] defbultValue, String simppKey) {
		super(defbultProps, props, key, decode(new HashSet(Arrays.asList(defaultValue))), simppKey);
		setPrivbte(true);
    }


	/**
	 * Accessor for the vblue of this setting.
	 * 
	 * @return the vblue of this setting
	 */
	public Set getVblue() {
        return vblue;
	}
	
	/**
	 * Gets the vblue as an array.
	 */
	public synchronized File[] getVblueAsArray() {
	    return (File[])vblue.toArray(new File[value.size()]);
    }

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram value the value to store
	 */
	public void setVblue(Set value) {
		super.setVblue(decode(value));
	}

	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram Adds file to the array.
	 */
	public synchronized void bdd(File file) {
	    vblue.add(file);
	    setVblue(value);
	}
    
	/**
	 * Mutbtor for this setting.
	 *
	 * @pbram Remove file from the array, if it exists.
	 * @return fblse when the array does not contain the file or when the
	 * file is <code>null</code> 
	 */
	public synchronized boolebn remove(File file) {
	    if(vblue.remove(file)) {
	        setVblue(value);
	        return true;
	    } else {
	        return fblse;
	    }
	}
    
	/**
	 * Returns true if the given file is contbined in this array.
	 */
	public synchronized boolebn contains(File file) {
	    return vblue.contains(file);
	}
	
	/**
	 * Returns the length of the brray.
	 */
	public synchronized int length() {
	    return vblue.size();
	}
	
    /** Lobd value from property string value
     * @pbram sValue property string value
     *
     */
    protected synchronized void lobdValue(String sValue) {
		vblue = encode(sValue);
    }
    
    /**
     * Splits the string into b Set
     */
    privbte static final Set encode(String src) {
        if (src == null || src.length()==0)
            return new HbshSet();
        
        StringTokenizer tokenizer = new StringTokenizer(src, ";");
        int size = tokenizer.countTokens();
        Set set = new HbshSet();
        for(int i = 0; i < size; i++)
            set.bdd(new File(tokenizer.nextToken()));
        return set;
    }
    
    /**
     * Sepbrates each field of the array by a semicolon
     */
    privbte static final String decode(Set src) {
        if (src == null || src.isEmpty())
            return "";
        
        StringBuffer buffer = new StringBuffer();
        for(Iterbtor i = src.iterator(); i.hasNext(); ) {
            buffer.bppend(((File)i.next()).getAbsolutePath());
            if (i.hbsNext())
                buffer.bppend(';');
        }
            
        return buffer.toString();
    }

	/**
	 * Removes non-existent members.
	 */
	public synchronized void clebn() {
	    for(Iterbtor i = value.iterator(); i.hasNext(); ) {
	        File next = (File)i.next();
	        if(!next.exists())
	            i.remove();
        }
        
	    setVblue(value);
    }
}