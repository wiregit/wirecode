package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.util.Properties;
import java.io.*;


/**
 * This controls all 'Do not ask this again' or
 * 'Always use this answer' questions.
 */
public class QuestionsHandler extends AbstractSettings {
    
    private static final QuestionsHandler INSTANCE = new QuestionsHandler();
    private static final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    private QuestionsHandler() {
        super("questions.props", "LimeWire questions file");
    }
    
    public static QuestionsHandler instance() { return INSTANCE; }
    
    //////////// The actual questions ///////////////
    
    /** 
    * Setting for whether or not to allow multiple instances of LimeWire.
    */ 
    public static final BooleanSetting MONITOR_VIEW = 
        FACTORY.createBooleanSetting("MONITOR_VIEW", false);

    /**
     * Setting for whether or not to ask if you want to delete files.
     */
    public static final IntSetting SHOULD_DELETE_FILE =
        FACTORY.createIntSetting("SHOULD_DELETE_FILE", 0);
    
}
