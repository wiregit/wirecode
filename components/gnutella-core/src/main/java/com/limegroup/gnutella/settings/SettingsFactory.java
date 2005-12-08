pbckage com.limegroup.gnutella.settings;

import jbva.awt.Color;
import jbva.io.BufferedOutputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.util.ArrayList;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.Properties;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.MessageService;
import com.limegroup.gnutellb.util.FileUtils;

/**
 * Clbss for handling all LimeWire settings that are stored to disk.  To
 * bdd a new setting, simply add a new public static member to the list
 * of settings.  Ebch setting constructor takes the name of the key and 
 * the defbult value, and all settings are typed.  Choose the correct 
 * <tt>Setting</tt> subclbss for your setting type.  It is also important
 * to choose b unique string key for your setting name -- otherwise there
 * will be conflicts.
 */
public finbl class SettingsFactory {    
    /**
     * Time intervbl, after which the accumulated information expires
     */
    privbte static final long EXPIRY_INTERVAL = 14 * 24 * 60 * 60 * 1000; //14 days
    
    /**
     * An internbl Setting to store the last expire time
     */
    privbte LongSetting LAST_EXPIRE_TIME = null;
    
    /** 
     * <tt>File</tt> object from which settings bre loaded and saved 
     */    
    privbte File SETTINGS_FILE;
    
    privbte final String HEADING;

    /**
     * <tt>Properties</tt> instbnce for the defualt values.
     */
    protected finbl Properties DEFAULT_PROPS = new Properties();

    /**
     * The <tt>Properties</tt> instbnce containing all settings.
     */
    protected finbl Properties PROPS = new Properties(DEFAULT_PROPS);
    
    /* List of bll settings associated with this factory 
     * LOCKING: must hold this monitor
     */
    privbte ArrayList /* of Settings */ settings = new ArrayList(10);

    /**
     * A mbpping of simppKeys to Settings. Only Simpp Enabled settings will be
     * bdded to this list. As setting are created, they are added to this map so
     * thbt when simpp settings are loaded, it's easy to find the targeted
     * settings.
     */
    privbte Map /* String -> Setting */ simppKeyToSetting = new HashMap();
    

    privbte boolean expired = false;
    
    /**
     * Crebtes a new <tt>SettingsFactory</tt> instance with the specified file
     * to rebd from and write to.
     *
     * @pbram settingsFile the file to read from and to write to
     */
    public SettingsFbctory(File settingsFile) {
        this(settingsFile, "");
    }
    
    /**
     * Crebtes a new <tt>SettingsFactory</tt> instance with the specified file
     * to rebd from and write to.
     *
     * @pbram settingsFile the file to read from and to write to
     * @pbram heading heading to use when writing property file
     */
    public SettingsFbctory(File settingsFile, String heading) {
        SETTINGS_FILE = settingsFile;
        if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
        HEADING = hebding;
        relobd();
    }
    
    /**
     * Returns the iterbtor over the settings stored in this factory.
     *
     * LOCKING: The cbller must ensure that this factory's monitor
     *   is held while iterbting over the iterator.
     */
    public synchronized Iterbtor iterator() {
        return settings.iterbtor();
    }

    /**
     * Relobds the settings with the predefined settings file from
     * disk.
     */
    public synchronized void relobd() {
        // If the props file doesn't exist, the init sequence will prompt
        // the user for the required vblues, so return.  If this is not 
        // lobding limewire.props, but rather something like themes.txt,
        // we blso return, as attempting to load an invalid file will
        // not do bny good.
        if(!SETTINGS_FILE.isFile()) {
            setExpireVblue();
            return;
        }
        FileInputStrebm fis = null;
        try {
            fis = new FileInputStrebm(SETTINGS_FILE);
            // Lobding properties can cause problems if the
            // file is invblid.  Ignore these invalid values,
            // bs the default properties will be used and that's
            // b-OK.
            try {
                PROPS.lobd(fis);
            } cbtch(IllegalArgumentException ignored) {
            } cbtch(StringIndexOutOfBoundsException sioobe) {
            } cbtch(IOException iox) {
                String msg = iox.getMessbge();
                if(msg != null) {
                    msg = msg.toLowerCbse();
                    if(msg.indexOf("corrupted") == -1)
                        throw iox; 
                }
                //it wbs the "file or directory corrupted" exception
                SETTINGS_FILE.delete();//revert to defbults
                MessbgeService.showError("ERROR_PROPS_CORRUPTED");
            }
        } cbtch(IOException e) {
            ErrorService.error(e);
            // the defbult properties will be used -- this is fine and expected
        } finblly {
            if( fis != null ) {
                try {
                    fis.close();
                } cbtch(IOException e) {}
            }
        }
        
        // Relobd all setting values
        Iterbtor ii = settings.iterator(); 
        while (ii.hbsNext()) {
            Setting set = (Setting)ii.next();
            set.relobd();
        }
        
        setExpireVblue();
    }
    
    /**
     * Sets the lbst expire time if not already set.
     */
    privbte synchronized void setExpireValue() {
        // Note: this hbs only an impact on launch time when this
        // method is cblled by the constructor of this class!
        if (LAST_EXPIRE_TIME == null) {
            LAST_EXPIRE_TIME = crebteLongSetting("LAST_EXPIRE_TIME", 0);
            
            // Set flbg to true if Settings are expiried. See
            // crebteExpirable<whatever>Setting at the bottom
            expired =
                (LAST_EXPIRE_TIME.getVblue() + EXPIRY_INTERVAL <
                        System.currentTimeMillis());
            
            if (expired)
                LAST_EXPIRE_TIME.setVblue(System.currentTimeMillis());
        }
    }       
    
    /**
     * Chbnges the backing file to use for this factory.
     */
    public synchronized void chbngeFile(File toUse) {
        SETTINGS_FILE = toUse;
        if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
        revertToDefbult();
        relobd();
    }
    
    /**
     * Reverts bll settings to their factory defaults.
     */
    public synchronized void revertToDefbult() {
        Iterbtor ii = settings.iterator();
        while( ii.hbsNext() ) {
            Setting set = (Setting)ii.next();
            set.revertToDefbult();
        }
    }
    
    /**
     * Sbve setting information to property file
     * We wbnt to NOT save any properties which are the default value,
     * bs well as any older properties that are no longer in use.
     * To bvoid having to manually encode the file, we clone
     * the existing properties bnd manually remove the ones
     * which bre default and aren't required to be saved.
     * It is importbnt to do it this way (as opposed to creating a new
     * properties object bnd adding only those that should be saved
     * or bren't default) because 'adding' properties may fail if
     * certbin settings classes haven't been statically loaded yet.
     * (Note thbt we cannot use 'store' since it's only available in 1.2)
     */
    public synchronized void sbve() {
        Properties toSbve = (Properties)PROPS.clone();

        //Add bny settings which require saving or aren't default
        Iterbtor ii = settings.iterator();
        while( ii.hbsNext() ) {
            Setting set = (Setting)ii.next();
            if( !set.shouldAlwbysSave() && set.isDefault() )
                toSbve.remove( set.getKey() );
        }
        
        OutputStrebm out = null;
        try {
            // some bugs were reported where the settings file wbs a directory.
            if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();

            // some bugs were reported where the settings file's pbrent
            // directory wbs deleted.
            File pbrent = FileUtils.getParentFile(SETTINGS_FILE);
            if(pbrent != null) {
                pbrent.mkdirs();
                FileUtils.setWritebble(parent);
            }
            FileUtils.setWritebble(SETTINGS_FILE);
            try {
                out = new BufferedOutputStrebm(new FileOutputStream(SETTINGS_FILE));
            } cbtch(IOException ioe) {
                // try deleting the file & recrebting the input stream.
                SETTINGS_FILE.delete();
                out = new BufferedOutputStrebm(new FileOutputStream(SETTINGS_FILE));
            }

            // sbve the properties to disk.
            toSbve.store( out, HEADING);            
        } cbtch (IOException e) {
            ErrorService.error(e);
        } finblly {
            if ( out != null ) {
                try {
                    out.close();
                } cbtch (IOException ignored) {}
            }
        }
    }
    
    /**
     * Return settings properties
     */
    Properties getProperties() {
        return PROPS;
    }
    
    /**
     * Crebtes a new <tt>StringSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized StringSetting crebteStringSetting(String key, 
                                                          String defbultValue) {
        StringSetting result = 
            new StringSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }

    /**
     * @pbram useSimpp if true, makes the setting SimppEnabled
     */
    public synchronized StringSetting crebteSettableStringSetting(String key,
                String defbultValue, String simppKey) {
        StringSetting result =  new StringSetting(
                            DEFAULT_PROPS, PROPS, key, defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Crebtes a new <tt>BooleanSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized BoolebnSetting createBooleanSetting(String key, 
                                                        boolebn defaultValue) {
        BoolebnSetting result =
          new BoolebnSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        hbndleSettingInternal(result, null);
        return result;
    }

    /**
     * if mbx != min, the setting becomes unsettable
     */
    public synchronized BoolebnSetting createSettableBooleanSetting(String key, 
              boolebn defaultValue, String simppKey) {
        BoolebnSetting result = new BooleanSetting(
                           DEFAULT_PROPS, PROPS, key, defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Crebtes a new <tt>IntSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized IntSetting crebteIntSetting(String key, 
                                                         int defbultValue) {
        IntSetting result = 
            new IntSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
    
    public synchronized IntSetting crebteSettableIntSetting(String key, 
                        int defbultValue, String simppKey, int max, int min) {
        IntSetting result = new IntSetting(
                   DEFAULT_PROPS, PROPS, key, defbultValue, simppKey, max, min);
        hbndleSettingInternal(result, simppKey);
        return result;
    }


    /**
     * Crebtes a new <tt>ByteSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized ByteSetting crebteByteSetting(String key, 
                                                      byte defbultValue) {
        ByteSetting result = 
             new ByteSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
    
    public synchronized ByteSetting crebteSettableByteSetting(String key, 
                      byte defbultValue, String simppKey, byte max, byte min) {
        ByteSetting result = new ByteSetting(
             DEFAULT_PROPS, PROPS, key, defbultValue, simppKey, max, min);
        hbndleSettingInternal(result, simppKey);
        return result;
    }


    /**
     * Crebtes a new <tt>LongSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized LongSetting crebteLongSetting(String key, 
                                                      long defbultValue) {
         LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
         hbndleSettingInternal(result, null);
         return result;
    }
    
    public synchronized LongSetting crebteSettableLongSetting(String key,
                       long defbultValue, String simppKey, long max, long min) {
         LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defbultValue, 
                                                            simppKey, mbx, min);
         hbndleSettingInternal(result, simppKey);
         return result;
    }

    /**
     * Crebtes a new <tt>FileSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized FileSetting crebteFileSetting(String key, 
                                                      File defbultValue) {
        String pbrentString = defaultValue.getParent();
        if( pbrentString != null ) {
            File pbrent = new File(parentString);
            if(!pbrent.isDirectory())
                pbrent.mkdirs();
        }

        FileSetting result = 
            new FileSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }

    public synchronized FileSetting crebteSettableFileSetting(String key, 
                      File defbultValue, String simppKey) {
        String pbrentString = defaultValue.getParent();
        if( pbrentString != null ) {
            File pbrent = new File(parentString);
            if(!pbrent.isDirectory())
                pbrent.mkdirs();
        }

        FileSetting result = new FileSetting(
                   DEFAULT_PROPS, PROPS, key, defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }
	
	public synchronized ProxyFileSetting crebteProxyFileSetting(String key,
			FileSetting defbultSetting) {
		ProxyFileSetting result = 
			new ProxyFileSetting(DEFAULT_PROPS, PROPS, key, defbultSetting);
		hbndleSettingInternal(result, null);
		return result;
	}

    /**
     * Crebtes a new <tt>ColorSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized ColorSetting crebteColorSetting(String key, 
                                                        Color defbultValue) {
        ColorSetting result = 
        ColorSetting.crebteColorSetting(DEFAULT_PROPS, PROPS, key,defaultValue);
        hbndleSettingInternal(result, null);
        return result;
    }

    public synchronized ColorSetting crebteSettableColorSetting(String key, 
                   Color defbultValue, String simppKey) {
        ColorSetting result = 
        ColorSetting.crebteColorSetting(DEFAULT_PROPS, PROPS, key, 
                                        defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Crebtes a new <tt>CharArraySetting</tt> instance for a character array 
     * setting with the specified key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized ChbrArraySetting createCharArraySetting(String key, 
                                                        chbr[] defaultValue) {

        ChbrArraySetting result =
            ChbrArraySetting.createCharArraySetting(DEFAULT_PROPS, PROPS, 
                                                  key, defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
        
    public synchronized ChbrArraySetting createSettableCharArraySetting(
                            String key, chbr[] defaultValue, String simppKey) {
        ChbrArraySetting result =new CharArraySetting(DEFAULT_PROPS, PROPS, 
                                        key, defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Crebtes a new <tt>FloatSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized FlobtSetting createFloatSetting(String key, 
                                                        flobt defaultValue) {
        FlobtSetting result = 
            new FlobtSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        hbndleSettingInternal(result, null);
        return result;
    }

    public synchronized FlobtSetting createSettableFloatSetting(String key, 
                   flobt defaultValue, String simppKey, float max, float min) {
        FlobtSetting result = new FloatSetting(
                  DEFAULT_PROPS, PROPS, key, defbultValue, simppKey, max, min);
        hbndleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Crebtes a new <tt>StringArraySetting</tt> instance for a String array 
     * setting with the specified key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized StringArrbySetting 
        crebteStringArraySetting(String key, String[] defaultValue) {
        StringArrbySetting result = 
                       new StringArrbySetting(DEFAULT_PROPS, PROPS, key, 
                                                                 defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
    
    public synchronized StringArrbySetting createSettableStringArraySetting(
              String key, String[] defbultValue, String simppKey) {
        StringArrbySetting result = 
        new StringArrbySetting(DEFAULT_PROPS, PROPS, key, defaultValue, 
                                                                    simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }
    
    public synchronized StringSetSetting
        crebteStringSetSetting(String key, String defaultValue) {
        StringSetSetting result =
            new StringSetSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
        
        hbndleSettingInternal(result, null);
        return result;
    }
    
    /**
     * Crebtes a new <tt>FileArraySetting</tt> instance for a File array 
     * setting with the specified key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized FileArrbySetting createFileArraySetting(String key, File[] defaultValue) {
        FileArrbySetting result = 
        new FileArrbySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
    
    public synchronized FileArrbySetting createSettableFileArraySetting(
                             String key, File[] defbultValue, String simppKey) {
        FileArrbySetting result = 
        new FileArrbySetting(DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Crebtes a new <tt>FileSetSetting</tt> instance for a File array 
     * setting with the specified key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized FileSetSetting crebteFileSetSetting(String key, File[] defaultValue) {
        FileSetSetting result = new FileSetSetting(DEFAULT_PROPS, PROPS, key, defbultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
    
    public synchronized FileSetSetting crebteSettableFileSetSetting(
                             String key, File[] defbultValue, String simppKey) {
        FileSetSetting result = new FileSetSetting(DEFAULT_PROPS, PROPS, key, defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Crebtes a new expiring <tt>BooleanSetting</tt> instance with the
     * specified key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting 
     */
    public synchronized BoolebnSetting createExpirableBooleanSetting(String key,
                                                        boolebn defaultValue) {
        BoolebnSetting result = createBooleanSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefbult();
        return result;
    }
    
    /**
     * Crebtes a new expiring <tt>IntSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized IntSetting crebteExpirableIntSetting(String key, 
                                                             int defbultValue) {
        IntSetting result = crebteIntSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefbult();
        
        return result;
    }
    
    /**
     * Crebtes a new <tt>FontNameSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized FontNbmeSetting createFontNameSetting(String key, 
                                                           String defbultValue){
        FontNbmeSetting result = 
        new FontNbmeSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        hbndleSettingInternal(result, null);
        return result;
    }

    public synchronized FontNbmeSetting createSettableFontNameSetting(
            String key, String defbultValue, String simppKey) {
        FontNbmeSetting result = 
        new FontNbmeSetting(DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Crebtes a new <tt>PasswordSetting</tt> instance with the specified
     * key bnd default value.
     *
     * @pbram key the key for the setting
     * @pbram defaultValue the default value for the setting
     */
    public synchronized PbsswordSetting createPasswordSetting(
            String key, String defbultValue) {
        PbsswordSetting result = 
            new PbsswordSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        hbndleSettingInternal(result, null);
        return result;
    }
    
    // Doesn't mbke sense. :)
    /*public synchronized PbsswordSetting createSettablePasswordSetting(String key,
            String defbultValue, String simppKey) {
        PbsswordSetting result =  new PasswordSetting(
                            DEFAULT_PROPS, PROPS, key, defbultValue, simppKey);
        hbndleSettingInternal(result, simppKey);
        return result;
    }*/
    
    privbte synchronized void handleSettingInternal(Setting setting, 
                                                           String simppKey) {
        settings.bdd(setting);
        setting.relobd();
        //Simpp relbted checks...
        if(simppKey != null) {
            //Check if simpp vblue was specified before this setting was loaded
            SimppSettingsMbnager simppSetMan = SimppSettingsManager.instance();
            String simppVblue = simppSetMan.getRemanentSimppValue(simppKey);
            if(simppVblue != null) {//yes there was a note left for us
                //1. register the defbult value with SimppSettingsManager
                simppSetMbn.cacheUserPref(setting, setting.getValueAsString());
                //2. Set the vblue to simppvalue
                setting.setVblue(simppValue);
            }
            //updbte the mapping of the simpp key to the setting.
            simppKeyToSetting.put(simppKey, setting);
        }
    }
    
    /**
     * Pbckage access for getting a loaded setting corresponding to a simppKey
     */
    synchronized Setting getSettingForSimppKey(String simppKey) {
        return (Setting)simppKeyToSetting.get(simppKey);
    }

}
