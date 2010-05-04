package com.limegroup.gnutella;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;

/**
 * Looks for a file ToolbarChecker that is created when the Windows
 * installer is run. This file, if it exists describes the state in which
 * the Ask Toolbar is after installation.
 */
class AskInstallChecker {
    
    /** Ask Toolbar was not installed. */
    public static final String ASK_NOT_INSTALLED = "not_installed";
    
    /** Ask Toolbar is already installed or not shown. NOTE: this may not be the LW
     *  Toolbar but another Ask Toolbar. */
    public static final String ASK_ALREADY_INSTALLED = "not_shown";
    
    /** Ask Toolbar was installed on this LW installation. */
    public static final String ASK_INSTALLED = "installed";
    
    /** Ask Toolbar was installed on this LW installation and the 
     * Ask Search was selected. */
    public static final String ASK_INSTALLED_SEARCH = "installed_search";
    
    /** Ask Toolbar was installed on this LW installation and the 
     * Ask HomePage takeover was selected. */
    public static final String ASK_INSTALLED_HOME = "installed_home";
    
    /** Ask Toolbar was installed on this LW installation and the Ask
     * Search and Ask HomePage takeover was selected. */
    public static final String ASK_INSTALLED_SEARCH_HOME = "installed_search_home";
    
    
    /** Text when Ask Toolbar was already found installed. */
    private static final String ASK_RESULT_ALREADY_INSTALLED = "Already Installed";
    /** Text when Ask Toolbar was installed during this installtion. */
    private static final String ASK_RESULT_TOOLBAR = "/tbr";
    /** Text when Ask Search was selected. */
    private static final String ASK_RESULT_SEARCH = "/sa";
    /** Text when Ask HomePage takeover was selected. */
    private static final String ASK_RESULT_HOME = "/hpr";
       
    private final AskInstallCheckerSettings settings;
    
    @Inject
    public AskInstallChecker(AskInstallCheckerSettings settings) {
        this.settings = settings;
    }
    
    /**
     * Attempts to read the toolbarResult file that was created on installation
     * of a Windows machine. This file contains information about the state of
     * the Ask Toolbar at installation time. Returns the given state of the 
     * Ask Toolbar.
     */
    public String readToolbarResult() {
        File file = settings.getFile();
      
        if(file == null || !file.exists() || !file.canRead()) {
            return ASK_NOT_INSTALLED;
        } else {
            byte[] data = FileUtils.readFileFully(file);
            if(data == null || data.length == 0)
                return ASK_NOT_INSTALLED;

            String line = StringUtils.toUTF8String(data);
          
            if(StringUtils.isEmpty(line)) {
                return ASK_NOT_INSTALLED;
            } else if(line.equals(ASK_RESULT_ALREADY_INSTALLED)) {
                return ASK_ALREADY_INSTALLED;
            } else {
                List<String> results = Arrays.asList(line.split(" "));
                // ASK_RESULT_TOOLBAR should always be there when it has 
                // been installed.
                if(results.contains(ASK_RESULT_TOOLBAR)) {
                    boolean search = results.contains(ASK_RESULT_SEARCH);
                    boolean homePage = results.contains(ASK_RESULT_HOME);
                    if(search && homePage) {
                        return ASK_INSTALLED_SEARCH_HOME;
                    } else if(search) {
                        return ASK_INSTALLED_SEARCH;
                    } else if(homePage) {
                        return ASK_INSTALLED_HOME;
                    } else {
                        return ASK_INSTALLED;
                    }
                } else {
                    return ASK_NOT_INSTALLED;
                }
            }
        }
    }
}
