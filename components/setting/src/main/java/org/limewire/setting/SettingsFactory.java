package org.limewire.setting;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.util.FileUtils;


/**
 * Coordinates the creating, storing and reloading of persistent data to and 
 * from disk for {@link Setting} objects. Each <code>Setting</code> creation 
 * method takes the name of the key and the default value, and all settings 
 * are typed. Since duplicate keys aren't allowed, you must choose a unique 
 * string for your setting key name.  
 * <p>
 * When you add a new setting, add a public synchronized member to 
 * <code>SettingsFactory</code> to create a setting instance.
 * <p>
 * A <code>Setting</code> is stored to disk when the value is altered from the 
 * default value. For example, with a file font.txt without the key ARIAL included:
<pre>
        File f = new File("font.txt");
        SettingsFactory sf = new SettingsFactory(f);

        FontNameSetting font = sf.createRemoteFontNameSetting("ARIAL", 
                                                       "defaultValue", 
                                                       "ARIAL_REMOTE");
        System.out.println(font.getValue());
        font.setValue("Arial");
        System.out.println(font.getValue());

    Output:
        defaultValue
        Arial

With the change from defaultValue to Arial, font.txt now includes:
        ARIAL=Arial
 </pre>
 * Additionally, the value stored in disk is loaded for each key
 * you specify regardless of the default value in create method. For example 
 * with "ARIAL=Arial" stored in font.txt:
 <pre>
        File f = new File("font.txt");
        SettingsFactory sf = new SettingsFactory(f);

        FontNameSetting font = sf.createRemoteFontNameSetting("ARIAL", 
                                                        "defaultValue2", 
                                                        "ARIAL_REMOTE");
        System.out.println(font.getValue());

    Output:
        Arial
    
    font.txt still includes:
        ARIAL=Arial
 </pre>
 * If font.txt doesn't have the key ARIAL, then the value is "defaultValue2"
 * and font.txt won't have the ARIAL key and value (since the default value 
 * didn't change).
 */
public final class SettingsFactory implements Iterable<Setting> {    
    /** Time interval, after which the accumulated information expires */
    private static final long EXPIRY_INTERVAL = 14 * 24 * 60 * 60 * 1000; //14 days
    
    /** An internal Setting to store the last expire time */
    private LongSetting LAST_EXPIRE_TIME = null;
    
    /** <tt>File</tt> object from which settings are loaded and saved */    
    private File SETTINGS_FILE;
    
    /** The header written to the settings file. */
    private final String HEADING;

    /** <tt>Properties</tt> instance for the default values. */
    protected final Properties DEFAULT_PROPS = new Properties();

    /** The <tt>Properties</tt> instance containing all settings.  */
    protected final Properties PROPS = new Properties(DEFAULT_PROPS);
    
    /**
     * List of all settings associated with this factory 
     * LOCKING: must hold this monitor
     */
    private ArrayList<Setting> settings = new ArrayList<Setting>(10);

    /**
     * A mapping of remoteKeys to Settings. Only remote Enabled settings will be
     * added to this list. As setting are created, they are added to this map so
     * that when remote settings are loaded, it's easy to find the targeted
     * settings.
     */
    private Map<String, Setting> remoteKeyToSetting = new HashMap<String, Setting>();
    
    /**
     * The RemoteSettingsManager being used to control remote settings.
     */
    private RemoteSettingManager remoteManager = new NullRemoteManager();
    
    /** Whether or not expire-able settings have expired. */
    private boolean expired = false;
    
    /**
     * Creates a new <tt>SettingsFactory</tt> instance with the specified file
     * to read from and write to.
     *
     * @param settingsFile the file to read from and to write to
     */
    public SettingsFactory(File settingsFile) {
        this(settingsFile, "");
    }
    
    /**
     * Creates a new <tt>SettingsFactory</tt> instance with the specified file
     * to read from and write to.
     *
     * @param settingsFile the file to read from and to write to
     * @param heading heading to use when writing property file
     */
    public SettingsFactory(File settingsFile, String heading) {
        SETTINGS_FILE = settingsFile;
        if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
        HEADING = heading;
        reload();
    }
    
    /**
     * Returns the iterator over the settings stored in this factory.
     *
     * LOCKING: The caller must ensure that this factory's monitor
     *   is held while iterating over the iterator.
     */
    public synchronized Iterator<Setting> iterator() {
        return settings.iterator();
    }

    /**
     * Reloads the settings with the predefined settings file from
     * disk.
     */
    public synchronized void reload() {
        // If the props file doesn't exist, the init sequence will prompt
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
            // Loading properties can cause problems if the
            // file is invalid.  Ignore these invalid values,
            // as the default properties will be used and that's
            // a-OK.
            try {
                PROPS.load(fis);
            } catch(IllegalArgumentException ignored) {
            } catch(StringIndexOutOfBoundsException sioobe) {
            } catch(IOException iox) {
                String msg = iox.getMessage();
                if(msg != null) {
                    msg = msg.toLowerCase();
                    if(msg.indexOf("corrupted") == -1)
                        throw iox; 
                }
                //it was the "file or directory corrupted" exception
                SETTINGS_FILE.delete();//revert to defaults
                MessageService.showError("ERROR_PROPS_CORRUPTED");
            }
        } catch(IOException e) {
            ErrorService.error(e);
            // the default properties will be used -- this is fine and expected
        } finally {
            if( fis != null ) {
                try {
                    fis.close();
                } catch(IOException e) {}
            }
        }
        
        // Reload all setting values
        for(Setting set : settings)
            set.reload();
        
        setExpireValue();
    }
    
    /**
     * Sets the last expire time if not already set.
     */
    private synchronized void setExpireValue() {
        // Note: this has only an impact on launch time when this
        // method is called by the constructor of this class!
        if (LAST_EXPIRE_TIME == null) {
            LAST_EXPIRE_TIME = createLongSetting("LAST_EXPIRE_TIME", 0);
            
            // Set flag to true if Settings are expired. See
            // createExpirable<whatever>Setting at the bottom
            expired =
                (LAST_EXPIRE_TIME.getValue() + EXPIRY_INTERVAL <
                        System.currentTimeMillis());
            
            if (expired)
                LAST_EXPIRE_TIME.setValue(System.currentTimeMillis());
        }
    }       
    
    /**
     * Changes the backing file to use for this factory.
     */
    public synchronized void changeFile(File toUse) {
        SETTINGS_FILE = toUse;
        if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();
        revertToDefault();
        reload();
    }
    
    /**
     * Reverts all settings to their factory defaults.
     */
    public synchronized void revertToDefault() {
        for(Setting set : settings)
            set.revertToDefault();
    }
    
    /**
     * Save setting information to property file
     * We want to NOT save any properties which are the default value,
     * as well as any older properties that are no longer in use.
     * To avoid having to manually encode the file, we clone
     * the existing properties and manually remove the ones
     * which are default and aren't required to be saved.
     * It is important to do it this way (as opposed to creating a new
     * properties object and adding only those that should be saved
     * or aren't default) because 'adding' properties may fail if
     * certain settings classes haven't been statically loaded yet.
     * (Note that we cannot use 'store' since it's only available in 1.2)
     */
    public synchronized void save() {
        Properties toSave = (Properties)PROPS.clone();

        //Add any settings which require saving or aren't default
        for(Setting set : settings) {
            if( !set.shouldAlwaysSave() && set.isDefault() )
                toSave.remove( set.getKey() );
        }
        
        OutputStream out = null;
        try {
            // some bugs were reported where the settings file was a directory.
            if(SETTINGS_FILE.isDirectory()) SETTINGS_FILE.delete();

            // some bugs were reported where the settings file's parent
            // directory was deleted.
            File parent = SETTINGS_FILE.getParentFile();
            if(parent != null) {
                parent.mkdirs();
                FileUtils.setWriteable(parent);
            }
            FileUtils.setWriteable(SETTINGS_FILE);
            try {
                out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE));
            } catch(IOException ioe) {
                // try deleting the file & recreating the input stream.
                SETTINGS_FILE.delete();
                out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE));
            }

            // save the properties to disk.
            toSave.store( out, HEADING);            
        } catch (IOException e) {
            ErrorService.error(e);
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
    }
    
    /**
     * Return settings properties
     */
    Properties getProperties() {
        return PROPS;
    }
    
    /** Sets a new RemoteSettingManager to control remote settings. 
     * */
    public synchronized void setRemoteSettingManager(RemoteSettingManager manager) {
        this.remoteManager = manager;
        manager.setRemoteSettingController(new RemoteSettingController() {
            public boolean updateSetting(String remoteKey, String value) {
                synchronized(SettingsFactory.this) {
                    Setting setting = remoteKeyToSetting.get(remoteKey);                    
                    if(setting != null) {
                        setting.setValue(value);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        });
    }
    
    /**
     * Creates a new <tt>StringSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized StringSetting createStringSetting(String key, 
                                                          String defaultValue) {
        StringSetting result = 
            new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    public synchronized StringSetting createRemoteStringSetting(String key,
                String defaultValue, String remoteKey) {
        StringSetting result =  new StringSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }

    /**
     * Creates a new <tt>BooleanSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized BooleanSetting createBooleanSetting(String key, 
                                                        boolean defaultValue) {
        BooleanSetting result =
          new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    /**
     * if max != min, the setting becomes unsettable
     */
    public synchronized BooleanSetting createRemoteBooleanSetting(String key, 
              boolean defaultValue, String remoteKey) {
        BooleanSetting result = new BooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }

    /**
     * Creates a new <tt>IntSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized IntSetting createIntSetting(String key, 
                                                         int defaultValue) {
        IntSetting result = 
            new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    public synchronized IntSetting createRemoteIntSetting(String key, 
                        int defaultValue, String remoteKey, int min, int max) {
        IntSetting result = new IntSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
        handleSettingInternal(result, remoteKey);
        return result;
    }


    /**
     * Creates a new <tt>ByteSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized ByteSetting createByteSetting(String key, 
                                                      byte defaultValue) {
        ByteSetting result = 
             new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
  
    public synchronized ByteSetting createRemoteByteSetting(String key, 
                      byte defaultValue, String remoteKey, byte min, byte max) {
        ByteSetting result = new ByteSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
        handleSettingInternal(result, remoteKey);
        return result;
    }


    /**
     * Creates a new <tt>LongSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized LongSetting createLongSetting(String key, 
                                                      long defaultValue) {
         LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
         handleSettingInternal(result, null);
         return result;
    }

    public synchronized LongSetting createRemoteLongSetting(String key,
                       long defaultValue, String remoteKey, long min, long max) {
         LongSetting result = 
             new LongSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
         handleSettingInternal(result, remoteKey);
         return result;
    }

    /**
     * Creates a new <tt>PowerOfTwoSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting, which must be a
     *            power of two.
     */
    public synchronized PowerOfTwoSetting createPowerOfTwoSetting(String key,
                       long defaultValue) {
        PowerOfTwoSetting result = 
            new PowerOfTwoSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    public synchronized PowerOfTwoSetting createRemotePowerOfTwoSetting(String key,
            long defaultValue, String remoteKey, long min, long max) {
        PowerOfTwoSetting result = 
            new PowerOfTwoSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    /**
     * Creates a new <tt>FileSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized FileSetting createFileSetting(String key, 
                                                      File defaultValue) {
        String parentString = defaultValue.getParent();
        if( parentString != null ) {
            File parent = new File(parentString);
            if(!parent.isDirectory())
                parent.mkdirs();
        }

        FileSetting result = 
            new FileSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
 
    public synchronized FileSetting createRemoteFileSetting(String key, 
                      File defaultValue, String remoteKey) {
        String parentString = defaultValue.getParent();
        if( parentString != null ) {
            File parent = new File(parentString);
            if(!parent.isDirectory())
                parent.mkdirs();
        }

        FileSetting result = new FileSetting(
                   DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }
	
	public synchronized ProxyFileSetting createProxyFileSetting(String key,
			FileSetting defaultSetting) {
		ProxyFileSetting result = 
			new ProxyFileSetting(DEFAULT_PROPS, PROPS, key, defaultSetting);
		handleSettingInternal(result, null);
		return result;
	}

    /**
     * Creates a new <tt>ColorSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized ColorSetting createColorSetting(String key, 
                                                        Color defaultValue) {
        ColorSetting result = 
        ColorSetting.createColorSetting(DEFAULT_PROPS, PROPS, key,defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    public synchronized ColorSetting createRemoteColorSetting(String key, 
                   Color defaultValue, String remoteKey) {
        ColorSetting result = 
        ColorSetting.createColorSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }

    /**
     * Creates a new <tt>CharArraySetting</tt> instance for a character array 
     * setting with the specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized CharArraySetting createCharArraySetting(String key, 
                                                        char[] defaultValue) {
        CharArraySetting result = new CharArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
   
    public synchronized CharArraySetting createRemoteCharArraySetting(
                            String key, char[] defaultValue, String remoteKey) {
        CharArraySetting result =new CharArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    /**
     * Creates a new <tt>FloatSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized FloatSetting createFloatSetting(String key, 
                                                        float defaultValue) {
        FloatSetting result = 
            new FloatSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    public synchronized FloatSetting createRemoteFloatSetting(String key, 
                   float defaultValue, String remoteKey, float min, float max) {
        FloatSetting result = new FloatSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    /**
     * Creates a new <tt>StringArraySetting</tt> instance for a String array 
     * setting with the specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized StringArraySetting 
        createStringArraySetting(String key, String[] defaultValue) {
        StringArraySetting result = 
                       new StringArraySetting(DEFAULT_PROPS, PROPS, key, 
                                                                 defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
   
    public synchronized StringArraySetting createRemoteStringArraySetting(
              String key, String[] defaultValue, String remoteKey) {
        StringArraySetting result = 
        new StringArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    public synchronized StringSetSetting
        createStringSetSetting(String key, String defaultValue) {
        StringSetSetting result =
            new StringSetSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        
        handleSettingInternal(result, null);
        return result;
    }
    
    /**
     * Creates a new <tt>FileArraySetting</tt> instance for a File array 
     * setting with the specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized FileArraySetting createFileArraySetting(String key, File[] defaultValue) {
        FileArraySetting result = 
        new FileArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
   
    public synchronized FileArraySetting createRemoteFileArraySetting(
                             String key, File[] defaultValue, String remoteKey) {
        FileArraySetting result = 
        new FileArraySetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }

    /**
     * Creates a new <tt>FileSetSetting</tt> instance for a File array 
     * setting with the specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized FileSetSetting createFileSetSetting(String key, File[] defaultValue) {
        FileSetSetting result = new FileSetSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
   
    public synchronized FileSetSetting createRemoteFileSetSetting(
                             String key, File[] defaultValue, String remoteKey) {
        FileSetSetting result = new FileSetSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    /**
     * Creates a new expiring <tt>BooleanSetting</tt> instance with the
     * specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting 
     */
    public synchronized BooleanSetting createExpirableBooleanSetting(String key,
                                                        boolean defaultValue) {
        BooleanSetting result = createBooleanSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefault();
        return result;
    }
    
    /**
     * Creates a new expiring <tt>IntSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized IntSetting createExpirableIntSetting(String key, 
                                                             int defaultValue) {
        IntSetting result = createIntSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefault();
        
        return result;
    }
    
    /**
     * Creates a new expiring <tt>LongSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized LongSetting createExpirableLongSetting(String key, 
                                                             long defaultValue) {
        LongSetting result = createLongSetting(key, defaultValue);
        
        if (expired)
            result.revertToDefault();
        
        return result;
    }
    
    /**
     * Creates a new <tt>FontNameSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized FontNameSetting createFontNameSetting(String key, 
                                                           String defaultValue){
        FontNameSetting result = 
        new FontNameSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }

    public synchronized FontNameSetting createRemoteFontNameSetting(
            String key, String defaultValue, String remoteKey) {
        FontNameSetting result = 
        new FontNameSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    public synchronized ProbabilisticBooleanSetting createProbabilisticBooleanSetting(
            String key, float defaultValue) {
        ProbabilisticBooleanSetting result =
            new ProbabilisticBooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    public synchronized ProbabilisticBooleanSetting createRemoteProbabilisticBooleanSetting(
            String key, float defaultValue, String remoteKey, float min, float max) {
        ProbabilisticBooleanSetting result =
            new ProbabilisticBooleanSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
        handleSettingInternal(result, remoteKey);
        return result;
    }
    
    /**
     * Creates a new <tt>PasswordSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized PasswordSetting createPasswordSettingMD5(
            String key, String defaultValue) {
        PasswordSetting result = 
            new PasswordSetting(DEFAULT_PROPS, PROPS, PasswordSetting.MD5, key, defaultValue);
        handleSettingInternal(result, null);
        return result;
    }
    
    private synchronized void handleSettingInternal(Setting setting, 
                                                           String remoteKey) {
        settings.add(setting);
        setting.reload();
        //remote related checks...
        if(remoteKey != null) {
            if (remoteKeyToSetting.containsKey(remoteKey)) {
                throw new IllegalArgumentException("duplicate setting remoteKey: " + remoteKey);
            }
            
            String remoteValue = remoteManager.getUnloadedValueFor(remoteKey);
            if(remoteValue != null)
                setting.setValue(remoteValue);
            //update the mapping of the remote key to the setting.
            remoteKeyToSetting.put(remoteKey, setting);
        }
    }

}
