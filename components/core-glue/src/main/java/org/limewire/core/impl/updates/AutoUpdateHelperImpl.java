package org.limewire.core.impl.updates;

import java.io.File;
import java.io.IOException;

import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;

import com.limegroup.gnutella.util.LimeWireUtils;

public final class AutoUpdateHelperImpl implements AutoUpdateHelper{
    
    private static final Log LOG = LogFactory.getLog(AutoUpdateHelperImpl.class);
    
    /**
     * return true if new version is available for download thru auto-update route
     * and client's guid starts with auto-update prefix.
     */  
    @Override
    public boolean isUpdateAvailable() {

        boolean updateAvailable = false;
        try {
            Version limewireVersion = new Version(LimeWireUtils.getLimeWireVersion());
            Version updateVersion = new Version(UpdateSettings.AUTO_UPDATE_VERSION.get());
            if (updateVersion.compareTo(limewireVersion) > 0) { // new version is available.
                String guid = ApplicationSettings.CLIENT_ID.get();
                String updatePrefix = UpdateSettings.AUTO_UPDATE_PREFIX.get();
                if (StringUtils.isEmpty(updatePrefix) || guid.startsWith(updatePrefix)) { // new version is applicable.
                    updateAvailable = true;
                }
            }
        } catch (VersionFormatException ex) {
            LOG.warnf("Error parsing version info. CurrentVersion: {0}, UpdateVersion: {1}",
                    LimeWireUtils.getLimeWireVersion(), UpdateSettings.AUTO_UPDATE_VERSION.get());
        }

        return updateAvailable;
    }   
    
    @Override
    public boolean isUpdateReadyForInstall(){
        boolean updateReadyForInstall = false;
        try{
            Version limewireVersion = new Version(LimeWireUtils.getLimeWireVersion());
            Version downloadedLimewireVersion = new Version(UpdateSettings.DOWNLOADED_UPDATE_VERSION.get());
            updateReadyForInstall = (downloadedLimewireVersion.compareTo(limewireVersion) > 0 && 
                                     !StringUtils.isEmpty(UpdateSettings.AUTO_UPDATE_COMMAND.get())
                                    );
        }catch(VersionFormatException ex){
            LOG.warnf("Error parsing version info. CurrentVersion: {0}, DownloadedVersion: {1}",
                    LimeWireUtils.getLimeWireVersion(), UpdateSettings.DOWNLOADED_UPDATE_VERSION.get());
        }
        return updateReadyForInstall;
    }
    
    /**
     * bitrock autoupdate options for update download.
     */
    @Override
    public String getAutoUpdateCommand(){
        StringBuilder updateCommand = new StringBuilder(getAutoupdateExecutablePath());
        updateCommand.append(SEPARATOR).append("--url");
        updateCommand.append(SEPARATOR).append(UpdateSettings.AUTO_UPDATE_XML_URL.get());
        return updateCommand.toString();
    }
    
    /**
     * 
     */
    @Override
    public void initiateUpdateProcess(){
        
        String javaExecutable =  getAbsoultePathToJavaExecutable();
        String classpath = System.getProperty("java.class.path");
        String main = "org.limewire.ui.swing.update.AutoUpdateExecutableInvoker";
        
        String updateCommand = UpdateSettings.AUTO_UPDATE_COMMAND.get();
        String[] args = updateCommand.split(AutoUpdateHelper.SEPARATOR);
        
        String[] cmdArray = new String[4 + args.length];
        
        cmdArray[0] = javaExecutable;
        cmdArray[1] = "-cp";
        cmdArray[2] = classpath;
        cmdArray[3] = main;
        System.arraycopy(args, 0, cmdArray, 4, args.length);       
        
        try{
            Runtime.getRuntime().exec(cmdArray);
        }catch(IOException io){
            LOG.error("Error starting the download process", io);
        }
    }
    
    /**
     * returns the path of bit-rock auto-update executable
     */
    private String getAutoupdateExecutablePath(){
        String executablePath = null; 
        String nativeLibDirectory = System.getProperty("jna.library.path", 
                CommonUtils.getCurrentDirectory().getAbsolutePath());       
        String absPath = new File(nativeLibDirectory).getAbsolutePath();       
        
        if(OSUtils.isLinux())
            executablePath = absPath + File.separator + "autoupdate-linux.bin";
        else if(OSUtils.isMacOSX())
            executablePath = absPath + File.separator + "autoupdate-osx.app/Contents/MacOS/installbuilder.sh";
        else if(OSUtils.isWindows())
            executablePath = absPath + File.separator + "autoupdate-windows.exe";

        return  executablePath;
    }
    
    private String getAbsoultePathToJavaExecutable(){
        StringBuilder sb = new StringBuilder(System.getProperty("java.home"));
        sb.append(File.separator).append("bin").append(File.separator).append("java");
        
        if(OSUtils.isWindows()){
            sb.append(".exe");
        }
        
        return sb.toString();
    }

}
