package com.limegroup.gnutella.settings; 

/** 
* Settings for starting limewire. 
*/ 
public final class StartupSettings extends AbstractSettings { 

/** 
* Setting for whether or not to allow multiple instances of LimeWire. 
*/ 
public static final BooleanSetting ALLOW_MULTIPLE_INSTANCES = 
    CFG_FACTORY.createBooleanSetting("ALLOW_MULTIPLE_INSTANCES", false); 
} 
