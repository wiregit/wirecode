package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.util.*;
import java.io.*;

/**
 * Class for handling all LimeWire settings that are stored to disk.  To
 * add a new setting, simply add a new public static member to the list
 * of settings.  Construct settings using the <tt>FACTORY</tt> instance
 * from the <tt>AbstractSettings</tt> superclass.  Each setting factory 
 * constructor takes the name of the key and the default value, and all 
 * settings are typed.  Choose the correct <tt>Setting</tt> factory constructor 
 * for your setting type.  It is also important to choose a unique string key 
 * for your setting name -- otherwise there will be conflicts, and a runtime
 * exception will be thrown.
 */
public final class Settings extends AbstractSettings {
}
