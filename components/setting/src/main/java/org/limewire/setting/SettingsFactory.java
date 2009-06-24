package org.limewire.setting;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.limewire.util.FileUtils;


/**
 * Coordinates the creating, storing and reloading of persistent data to and 
 * from disk for {@link AbstractSetting} objects. Each <code>Setting</code> creation 
 * method takes the name of the key and the default value, and all settings 
 * are typed. Since duplicate keys aren't allowed, you must choose a unique 
 * string for your setting key name, otherwise an exception, 
 * <code>IllegalArgumentException</code> is thrown.  
 * <p>
 * When you add a new <code>Setting</code> subclass, add a public synchronized 
 * method to <code>SettingsFactory</code> to create an instance of the setting.
 * For example, subclass {@link IntSetting}, <code>SettingsFactory</code> has 
 * {@link #createIntSetting(String, int)} and
 * {@link #createRemoteIntSetting(String, int, String, int, int)}.
 * <p>
 * An example of creating an {@link IntSetting} that uses setting.txt, without the key 
 * MAX_MESSAGE_SIZE previously included:
<pre>
        File f = new File("setting.txt");
        SettingsFactory sf = new SettingsFactory(f);

        IntSetting intsetting = sf.createIntSetting("MAX_MESSAGE_SIZE", 1492);

        System.out.println("1: " + intsetting.getValue());
        intsetting.setValue("2984");
        System.out.println("2: " + intsetting.getValue());
        sf.save();

    Output:
        1: 1492
        2: 2984
 </pre>

With the call sf.save(), setting.txt now includes:
 <pre>
        MAX_MESSAGE_SIZE=2984
 </pre>
 * Additionally, the value stored in disk is loaded for each key
 * you specify regardless of the default value in the create method. For example 
 * with "MAX_MESSAGE_SIZE=2984" stored in setting.txt:
 <pre>
        File f = new File("setting.txt");
        SettingsFactory sf = new SettingsFactory(f);

        IntSetting intsetting = sf.createIntSetting("MAX_MESSAGE_SIZE", 0);
        System.out.println(intsetting.getValue());
        sf.save();
    Output:
        2984
  </pre>  
    font.txt still includes:
 <pre>
        MAX_MESSAGE_SIZE=2984
 </pre>
 * If setting.txt didn't have the key MAX_MESSAGE_SIZE prior to the 
 * <code>createIntSetting</code> call, then the MAX_MESSAGE_SIZE is 0.
 */
public final class SettingsFactory implements Iterable<AbstractSetting>, RemoteSettingController {
    
    /** Marked true in the event of an error in the load/save of any settings file. */ 
    private static boolean loadSaveFailureEncountered = false;   
    
    /** Time interval, after which the accumulated information expires. */
    private static final long EXPIRY_INTERVAL = 14 * 24 * 60 * 60 * 1000; //14 days
    
    /** An internal Setting to store the last expire time. */
    private LongSetting LAST_EXPIRE_TIME = null;
    
    /** An internal Setting that controls whether or not unlisted remote settings revert to default. */
    private BooleanSetting REVERT_UNLISTED_REMOTE = null;
    
    /** <tt>File</tt> object from which settings are loaded and saved. */    
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
    private ArrayList<AbstractSetting> settings = new ArrayList<AbstractSetting>(10);

    /**
     * A mapping of remoteKeys to Settings. Only remote Enabled settings will be
     * added to this list. As setting are created, they are added to this map so
     * that when remote settings are loaded, it's easy to find the targeted
     * settings.
     */
    private Map<String, AbstractSetting> remoteKeyToSetting = new HashMap<String, AbstractSetting>();
    
    /**
     * The RemoteSettingsManager being used to control remote settings.
     */
    private RemoteSettingManager remoteManager = new NullRemoteManager();
    
    /** Whether or not expirable settings have expired. */
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
        if (SETTINGS_FILE.isDirectory())
            SETTINGS_FILE.delete();
        HEADING = heading;
        reload();
    }
    
    /**
     * Indicated if a failure has occurred for delayed reporting 
     */
    public static boolean hasLoadSaveFailure() {
        return loadSaveFailureEncountered;
    }

    /**
     * Saves a failure event for delayed reporting.
     */
    private static void markFailure() {
        loadSaveFailureEncountered = true;
    }
    
    /**
     * Resets the failure flag. 
     */
    public static void resetLoadSaveFailure() {
        loadSaveFailureEncountered = false;
    }
    
    /**
     * Returns the iterator over the settings stored in this factory.
     * <p>
     * LOCKING: The caller must ensure that this factory's monitor
     *   is held while iterating over the iterator.
     */
    public synchronized Iterator<AbstractSetting> iterator() {
        return settings.iterator();
    }
    
    /**
     * Returns the setting that controls whether or not remote settings
     * are reverted when loaded.
     */
    public BooleanSetting getRevertSetting() {
        return REVERT_UNLISTED_REMOTE;
    }

    /**
     * Reloads the settings with the predefined settings file from
     * disk.
     */
    public synchronized void reload() {
        // Setup the key that tells us whether or not to revert
        // remote settings that are not listed in the remote updates.
        if(REVERT_UNLISTED_REMOTE == null)
            REVERT_UNLISTED_REMOTE = createBooleanSetting("REVERT_UNLISTED_REMOTE", true); 
        
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
            
            try {
                PROPS.load(fis);
            } catch(IllegalArgumentException e) {
                // Ignored -- Use best guess
            } catch(StringIndexOutOfBoundsException e) {
                // Ignored -- Use best guess
            } catch(IOException e) {
                // Serious Problems --- Use defaults
                markFailure();
            }
            
        } catch(FileNotFoundException e) {            
            if (SETTINGS_FILE.exists()) {
                markFailure();
            }
        } finally {
            FileUtils.close(fis);
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
        if (SETTINGS_FILE.isDirectory())
            SETTINGS_FILE.delete();
        revertToDefault();
        reload();
    }
    
    /**
     * Reverts all settings to their factory defaults.
     */
    public synchronized boolean revertToDefault() {
        boolean any = false;
        for(Setting setting : settings) {
            any |= setting.revertToDefault();
        }
        return any;
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
     * (Note that we cannot use 'store' since it's only available in 1.2.)
     */
    public synchronized void save() {
        Properties toSave = (Properties) PROPS.clone();

        //Add any settings which require saving or aren't default
        for(Setting set : settings) {
            if( !set.shouldAlwaysSave() && set.isDefault() )
                toSave.remove( set.getKey() );
        }
        
        OutputStream out = null;
        try {
            // some bugs were reported where the settings file was a directory.
            if (SETTINGS_FILE.isDirectory())
                SETTINGS_FILE.delete();

            // some bugs were reported where the settings file's parent
            // directory was deleted.
            File parent = SETTINGS_FILE.getParentFile();
            if(parent != null) {
                parent.mkdirs();
            }
            
            FileUtils.setWriteable(SETTINGS_FILE);
            
            if (SETTINGS_FILE.exists() && !SETTINGS_FILE.canRead()) {
                SETTINGS_FILE.delete();
            }
            
            try {
                out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE));
            } catch(IOException ioe) {
                // Try again.
                if (SETTINGS_FILE.exists()) {
                    SETTINGS_FILE.delete();
                    out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE));
                }
            }

            if(out != null) {
                // save the properties to disk.
                toSave.store(out, HEADING);
            } else {
                markFailure();
            }
            
        } catch (IOException e) {
            markFailure();
        } finally {
            FileUtils.close(out);
        }
    }
    
    @Override
    public String toString() {
        return PROPS.toString();
    }
    
    /**
     * Return settings properties.
     */
    Properties getProperties() {
        return PROPS;
    }
    
    /** Sets a new RemoteSettingManager to control remote settings. 
     * */
    public synchronized void setRemoteSettingManager(RemoteSettingManager manager) {
        this.remoteManager = manager;
        manager.setRemoteSettingController(this);
    }
    
    public synchronized boolean updateSetting(String remoteKey, String value) {
        AbstractSetting setting = remoteKeyToSetting.get(remoteKey);                    
        if(setting != null) {
            setting.setValueInternal(value);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * If we're reverting unlisted remote settings, then go through every setting
     * we know of that's considered remote and revert it, if it isn't in the list
     * of current remotely set settings.
     */
    public synchronized void revertRemoteSettingsUnlessIn(Set<String> keySet) {
        if(REVERT_UNLISTED_REMOTE.getValue()) {
            for(Map.Entry<String, AbstractSetting> entry : remoteKeyToSetting.entrySet()) {
                if(!keySet.contains(entry.getKey())) {
                    entry.getValue().revertToDefault();
                }
            }
        }
    }
    
    /**
     * Creates a new <tt>StringSetting</tt> instance with the specified
     * key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting, cannot be null
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
          new BooleanSettingImpl(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal((AbstractSetting)result, null);
        return result;
    }

    /**
     * If max != min, the setting becomes unsettable.
     */
    public synchronized BooleanSetting createRemoteBooleanSetting(String key, 
              boolean defaultValue, String remoteKey) {
        BooleanSetting result = new BooleanSettingImpl(DEFAULT_PROPS, PROPS, key, defaultValue);
        handleSettingInternal((AbstractSetting)result, remoteKey);
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
        // Creation of parent dirs removed per LWC-1323.

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

    public synchronized FloatSetting createFloatSetting(String key, 
                    float defaultValue, float min, float max) {
        FloatSetting result = 
            new FloatSetting(DEFAULT_PROPS, PROPS, key, defaultValue, min, max);
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
   
    /**
     * Creates a new <tt>PropertiesSetting</tt> instance for a Properties
     * setting with the specified key and default value.
     *
     * @param key the key for the setting
     * @param defaultValue the default value for the setting
     */
    public synchronized PropertiesSetting 
        createPropertiesSetting(String key, Properties defaultValue) {
        PropertiesSetting result = 
                       new PropertiesSetting(DEFAULT_PROPS, PROPS, key, 
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
    
    private synchronized void handleSettingInternal(AbstractSetting setting, 
                                                           String remoteKey) {
        settings.add(setting);
        setting.reload();
        //remote related checks...
        if(remoteKey != null) {
            if (remoteKeyToSetting.containsKey(remoteKey)) {
                throw new IllegalArgumentException("duplicate setting remoteKey: " + remoteKey);
            }
            
            String remoteValue = remoteManager.getUnloadedValueFor(remoteKey);
            if(remoteValue != null) {
                setting.setValueInternal(remoteValue);
            } else if(REVERT_UNLISTED_REMOTE.getValue()) {
                // As we load this setting, if it's not in the remote settings,
                // revert it.  It's OK if we revert settings that are later added
                // to a remote setting, because it'll update the value.
                setting.revertToDefault();
            }
            //update the mapping of the remote key to the setting.
            remoteKeyToSetting.put(remoteKey, setting);
        }
    }

}
