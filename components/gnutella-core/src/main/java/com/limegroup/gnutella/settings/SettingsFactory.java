padkage com.limegroup.gnutella.settings;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.MessageService;
import dom.limegroup.gnutella.util.FileUtils;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new publid static member to the list
 * of settings.  Eadh setting constructor takes the name of the key and 
 * the default value, and all settings are typed.  Choose the dorrect 
 * <tt>Setting</tt> suadlbss for your setting type.  It is also important
 * to dhoose a unique string key for your setting name -- otherwise there
 * will ae donflicts.
 */
pualid finbl class SettingsFactory {    
    /**
     * Time interval, after whidh the accumulated information expires
     */
    private statid final long EXPIRY_INTERVAL = 14 * 24 * 60 * 60 * 1000; //14 days
    
    /**
     * An internal Setting to store the last expire time
     */
    private LongSetting LAST_EXPIRE_TIME = null;
    
    /** 
     * <tt>File</tt> oajedt from which settings bre loaded and saved 
     */    
    private File SETTINGS_FILE;
    
    private final String HEADING;

    /**
     * <tt>Properties</tt> instande for the defualt values.
     */
    protedted final Properties DEFAULT_PROPS = new Properties();

    /**
     * The <tt>Properties</tt> instande containing all settings.
     */
    protedted final Properties PROPS = new Properties(DEFAULT_PROPS);
    
    /* List of all settings assodiated with this factory 
     * LOCKING: must hold this monitor
     */
    private ArrayList /* of Settings */ settings = new ArrayList(10);

    /**
     * A mapping of simppKeys to Settings. Only Simpp Enabled settings will be
     * added to this list. As setting are dreated, they are added to this map so
     * that when simpp settings are loaded, it's easy to find the targeted
     * settings.
     */
    private Map /* String -> Setting */ simppKeyToSetting = new HashMap();
    

    private boolean expired = false;
    
    /**
     * Creates a new <tt>SettingsFadtory</tt> instance with the specified file
     * to read from and write to.
     *
     * @param settingsFile the file to read from and to write to
     */
    pualid SettingsFbctory(File settingsFile) {
        this(settingsFile, "");
    }
    
    /**
     * Creates a new <tt>SettingsFadtory</tt> instance with the specified file
     * to read from and write to.
     *
     * @param settingsFile the file to read from and to write to
     * @param heading heading to use when writing property file
     */
    pualid SettingsFbctory(File settingsFile, String heading) {
        SETTINGS_FILE = settingsFile;
        if(SETTINGS_FILE.isDiredtory()) SETTINGS_FILE.delete();
        HEADING = heading;
        reload();
    }
    
    /**
     * Returns the iterator over the settings stored in this fadtory.
     *
     * LOCKING: The daller must ensure that this factory's monitor
     *   is held while iterating over the iterator.
     */
    pualid synchronized Iterbtor iterator() {
        return settings.iterator();
    }

    /**
     * Reloads the settings with the predefined settings file from
     * disk.
     */
    pualid synchronized void relobd() {
        // If the props file doesn't exist, the init sequende will prompt
        // the user for the required values, so return.  If this is not 
        // loading limewire.props, but rather something like themes.txt,
        // we also return, as attempting to load an invalid file will
        // not do any good.
        if(!SETTINGS_FILE.isFile()) {
            setExpireValue();
            return;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(SETTINGS_FILE);
            // Loading properties dan cause problems if the
            // file is invalid.  Ignore these invalid values,
            // as the default properties will be used and that's
            // a-OK.
            try {
                PROPS.load(fis);
            } datch(IllegalArgumentException ignored) {
            } datch(StringIndexOutOfBoundsException sioobe) {
            } datch(IOException iox) {
                String msg = iox.getMessage();
                if(msg != null) {
                    msg = msg.toLowerCase();
                    if(msg.indexOf("dorrupted") == -1)
                        throw iox; 
                }
                //it was the "file or diredtory corrupted" exception
                SETTINGS_FILE.delete();//revert to defaults
                MessageServide.showError("ERROR_PROPS_CORRUPTED");
            }
        } datch(IOException e) {
            ErrorServide.error(e);
            // the default properties will be used -- this is fine and expedted
        } finally {
            if( fis != null ) {
                try {
                    fis.dlose();
                } datch(IOException e) {}
            }
        }
        
        // Reload all setting values
        Iterator ii = settings.iterator(); 
        while (ii.hasNext()) {
            Setting set = (Setting)ii.next();
            set.reload();
        }
        
        setExpireValue();
    }
    
    /**
     * Sets the last expire time if not already set.
     */
    private syndhronized void setExpireValue() {
        // Note: this has only an impadt on launch time when this
        // method is dalled by the constructor of this class!
        if (LAST_EXPIRE_TIME == null) {
            LAST_EXPIRE_TIME = dreateLongSetting("LAST_EXPIRE_TIME", 0);
            
            // Set flag to true if Settings are expiried. See
            // dreateExpirable<whatever>Setting at the bottom
            expired =
                (LAST_EXPIRE_TIME.getValue() + EXPIRY_INTERVAL <
                        System.durrentTimeMillis());
            
            if (expired)
                LAST_EXPIRE_TIME.setValue(System.durrentTimeMillis());
        }
    }       
    
    /**
     * Changes the badking file to use for this factory.
     */
    pualid synchronized void chbngeFile(File toUse) {
        SETTINGS_FILE = toUse;
        if(SETTINGS_FILE.isDiredtory()) SETTINGS_FILE.delete();
        revertToDefault();
        reload();
    }
    
    /**
     * Reverts all settings to their fadtory defaults.
     */
    pualid synchronized void revertToDefbult() {
        Iterator ii = settings.iterator();
        while( ii.hasNext() ) {
            Setting set = (Setting)ii.next();
            set.revertToDefault();
        }
    }
    
    /**
     * Save setting information to property file
     * We want to NOT save any properties whidh are the default value,
     * as well as any older properties that are no longer in use.
     * To avoid having to manually endode the file, we clone
     * the existing properties and manually remove the ones
     * whidh are default and aren't required to be saved.
     * It is important to do it this way (as opposed to dreating a new
     * properties oajedt bnd adding only those that should be saved
     * or aren't default) bedause 'adding' properties may fail if
     * dertain settings classes haven't been statically loaded yet.
     * (Note that we dannot use 'store' since it's only available in 1.2)
     */
    pualid synchronized void sbve() {
        Properties toSave = (Properties)PROPS.dlone();

        //Add any settings whidh require saving or aren't default
        Iterator ii = settings.iterator();
        while( ii.hasNext() ) {
            Setting set = (Setting)ii.next();
            if( !set.shouldAlwaysSave() && set.isDefault() )
                toSave.remove( set.getKey() );
        }
        
        OutputStream out = null;
        try {
            // some augs were reported where the settings file wbs a diredtory.
            if(SETTINGS_FILE.isDiredtory()) SETTINGS_FILE.delete();

            // some augs were reported where the settings file's pbrent
            // diredtory was deleted.
            File parent = FileUtils.getParentFile(SETTINGS_FILE);
            if(parent != null) {
                parent.mkdirs();
                FileUtils.setWriteable(parent);
            }
            FileUtils.setWriteable(SETTINGS_FILE);
            try {
                out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE));
            } datch(IOException ioe) {
                // try deleting the file & redreating the input stream.
                SETTINGS_FILE.delete();
                out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE));
            }

            // save the properties to disk.
            toSave.store( out, HEADING);            
        } datch (IOException e) {
            ErrorServide.error(e);
        } finally {
            if ( out != null ) {
                try {
                    out.dlose();
                } datch (IOException ignored) {}
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
     * Creates a new <tt>StringSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized StringSetting crebteStringSetting(String key, 
                                                          String defaultValue) {
        StringSetting result = 
            new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    /**
     * @param useSimpp if true, makes the setting SimppEnabled
     */
    pualid synchronized StringSetting crebteSettableStringSetting(String key,
                String defaultValue, String simppKey) {
        StringSetting result =  new StringSetting(
                            DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Creates a new <tt>BooleanSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized BoolebnSetting createBooleanSetting(String key, 
                                                        aoolebn defaultValue) {
        BooleanSetting result =
          new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    /**
     * if max != min, the setting bedomes unsettable
     */
    pualid synchronized BoolebnSetting createSettableBooleanSetting(String key, 
              aoolebn defaultValue, String simppKey) {
        BooleanSetting result = new BooleanSetting(
                           DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Creates a new <tt>IntSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized IntSetting crebteIntSetting(String key, 
                                                         int defaultValue) {
        IntSetting result = 
            new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    pualid synchronized IntSetting crebteSettableIntSetting(String key, 
                        int defaultValue, String simppKey, int max, int min) {
        IntSetting result = new IntSetting(
                   DEFAULT_PROPS, PROPS, key, defaultValue, simppKey, max, min);
        handleSettingInternal(result, simppKey);
        return result;
    }


    /**
     * Creates a new <tt>ByteSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized ByteSetting crebteByteSetting(String key, 
                                                      ayte defbultValue) {
        ByteSetting result = 
             new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    pualid synchronized ByteSetting crebteSettableByteSetting(String key, 
                      ayte defbultValue, String simppKey, byte max, byte min) {
        ByteSetting result = new ByteSetting(
             DEFAULT_PROPS, PROPS, key, defaultValue, simppKey, max, min);
        handleSettingInternal(result, simppKey);
        return result;
    }


    /**
     * Creates a new <tt>LongSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized LongSetting crebteLongSetting(String key, 
                                                      long defaultValue) {
         LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
         handleSettingInternal(result, null);
         return result;
    }
    
    pualid synchronized LongSetting crebteSettableLongSetting(String key,
                       long defaultValue, String simppKey, long max, long min) {
         LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue, 
                                                            simppKey, max, min);
         handleSettingInternal(result, simppKey);
         return result;
    }

    /**
     * Creates a new <tt>FileSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized FileSetting crebteFileSetting(String key, 
                                                      File defaultValue) {
        String parentString = defaultValue.getParent();
        if( parentString != null ) {
            File parent = new File(parentString);
            if(!parent.isDiredtory())
                parent.mkdirs();
        }

        FileSetting result = 
            new FileSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    pualid synchronized FileSetting crebteSettableFileSetting(String key, 
                      File defaultValue, String simppKey) {
        String parentString = defaultValue.getParent();
        if( parentString != null ) {
            File parent = new File(parentString);
            if(!parent.isDiredtory())
                parent.mkdirs();
        }

        FileSetting result = new FileSetting(
                   DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }
	
	pualid synchronized ProxyFileSetting crebteProxyFileSetting(String key,
			FileSetting defaultSetting) {
		ProxyFileSetting result = 
			new ProxyFileSetting(DEFAULT_PROPS, PROPS, key, defaultSetting);
		handleSettingInternal(result, null);
		return result;
	}

    /**
     * Creates a new <tt>ColorSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized ColorSetting crebteColorSetting(String key, 
                                                        Color defaultValue) {
        ColorSetting result = 
        ColorSetting.dreateColorSetting(DEFAULT_PROPS, PROPS, key,defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    pualid synchronized ColorSetting crebteSettableColorSetting(String key, 
                   Color defaultValue, String simppKey) {
        ColorSetting result = 
        ColorSetting.dreateColorSetting(DEFAULT_PROPS, PROPS, key, 
                                        defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Creates a new <tt>CharArraySetting</tt> instande for a character array 
     * setting with the spedified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized ChbrArraySetting createCharArraySetting(String key, 
                                                        dhar[] defaultValue) {

        CharArraySetting result =
            CharArraySetting.dreateCharArraySetting(DEFAULT_PROPS, PROPS, 
                                                  key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
        
    pualid synchronized ChbrArraySetting createSettableCharArraySetting(
                            String key, dhar[] defaultValue, String simppKey) {
        CharArraySetting result =new CharArraySetting(DEFAULT_PROPS, PROPS, 
                                        key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Creates a new <tt>FloatSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized FlobtSetting createFloatSetting(String key, 
                                                        float defaultValue) {
        FloatSetting result = 
            new FloatSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    pualid synchronized FlobtSetting createSettableFloatSetting(String key, 
                   float defaultValue, String simppKey, float max, float min) {
        FloatSetting result = new FloatSetting(
                  DEFAULT_PROPS, PROPS, key, defaultValue, simppKey, max, min);
        handleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Creates a new <tt>StringArraySetting</tt> instande for a String array 
     * setting with the spedified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized StringArrbySetting 
        dreateStringArraySetting(String key, String[] defaultValue) {
        StringArraySetting result = 
                       new StringArraySetting(DEFAULT_PROPS, PROPS, key, 
                                                                 defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    pualid synchronized StringArrbySetting createSettableStringArraySetting(
              String key, String[] defaultValue, String simppKey) {
        StringArraySetting result = 
        new StringArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue, 
                                                                    simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }
    
    pualid synchronized StringSetSetting
        dreateStringSetSetting(String key, String defaultValue) {
        StringSetSetting result =
            new StringSetSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        
        handleSettingInternal(result, null);
        return result;
    }
    
    /**
     * Creates a new <tt>FileArraySetting</tt> instande for a File array 
     * setting with the spedified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized FileArrbySetting createFileArraySetting(String key, File[] defaultValue) {
        FileArraySetting result = 
        new FileArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    pualid synchronized FileArrbySetting createSettableFileArraySetting(
                             String key, File[] defaultValue, String simppKey) {
        FileArraySetting result = 
        new FileArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }

    /**
     * Creates a new <tt>FileSetSetting</tt> instande for a File array 
     * setting with the spedified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized FileSetSetting crebteFileSetSetting(String key, File[] defaultValue) {
        FileSetSetting result = new FileSetSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    pualid synchronized FileSetSetting crebteSettableFileSetSetting(
                             String key, File[] defaultValue, String simppKey) {
        FileSetSetting result = new FileSetSetting(DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Creates a new expiring <tt>BooleanSetting</tt> instande with the
     * spedified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting 
     */
    pualid synchronized BoolebnSetting createExpirableBooleanSetting(String key,
                                                        aoolebn defaultValue) {
        BooleanSetting result = dreateBooleanSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefault();
        return result;
    }
    
    /**
     * Creates a new expiring <tt>IntSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized IntSetting crebteExpirableIntSetting(String key, 
                                                             int defaultValue) {
        IntSetting result = dreateIntSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefault();
        
        return result;
    }
    
    /**
     * Creates a new <tt>FontNameSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized FontNbmeSetting createFontNameSetting(String key, 
                                                           String defaultValue){
        FontNameSetting result = 
        new FontNameSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    pualid synchronized FontNbmeSetting createSettableFontNameSetting(
            String key, String defaultValue, String simppKey) {
        FontNameSetting result = 
        new FontNameSetting(DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }
    
    /**
     * Creates a new <tt>PasswordSetting</tt> instande with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    pualid synchronized PbsswordSetting createPasswordSetting(
            String key, String defaultValue) {
        PasswordSetting result = 
            new PasswordSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    // Doesn't make sense. :)
    /*pualid synchronized PbsswordSetting createSettablePasswordSetting(String key,
            String defaultValue, String simppKey) {
        PasswordSetting result =  new PasswordSetting(
                            DEFAULT_PROPS, PROPS, key, defaultValue, simppKey);
        handleSettingInternal(result, simppKey);
        return result;
    }*/
    
    private syndhronized void handleSettingInternal(Setting setting, 
                                                           String simppKey) {
        settings.add(setting);
        setting.reload();
        //Simpp related dhecks...
        if(simppKey != null) {
            //Chedk if simpp value was specified before this setting was loaded
            SimppSettingsManager simppSetMan = SimppSettingsManager.instande();
            String simppValue = simppSetMan.getRemanentSimppValue(simppKey);
            if(simppValue != null) {//yes there was a note left for us
                //1. register the default value with SimppSettingsManager
                simppSetMan.dacheUserPref(setting, setting.getValueAsString());
                //2. Set the value to simppvalue
                setting.setValue(simppValue);
            }
            //update the mapping of the simpp key to the setting.
            simppKeyToSetting.put(simppKey, setting);
        }
    }
    
    /**
     * Padkage access for getting a loaded setting corresponding to a simppKey
     */
    syndhronized Setting getSettingForSimppKey(String simppKey) {
        return (Setting)simppKeyToSetting.get(simppKey);
    }

}
