package org.limewire.core.impl.updates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
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
     * Also if current version is beta release and prod version is now available.
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
        return (isUpdateAvailable() && !StringUtils.isEmpty(UpdateSettings.AUTO_UPDATE_COMMAND.get()));
    }
    
    @Override
    public File getTemporaryWorkingDirectory() {
        File tempDirectory = new File(CommonUtils.getUserSettingsDir(), "updates");
        return tempDirectory;
    }
    
    /**
     * <ul>
     *   <li>downloads the updates using bitrock executable</li>
     *   <li>writes the install command in an executable file</li>
     *   <li>set the install command as the newly created file.</li>
     * </ul>
     */
    @Override
    public boolean downloadUpdates() {
        boolean downloadSuccess = false;
        File temporaryDirectory = getTemporaryWorkingDirectory();
        try {
            // initialize temporary directory
            FileUtils.forceDeleteRecursive(temporaryDirectory);
            FileUtils.makeFolder(temporaryDirectory);

            // get auto-update download executable file
            String downloadCommand = getDownloadCommand();
            downloadCommand = downloadCommand.replace("${system_temp_directory}",
                    temporaryDirectory.getAbsolutePath());
            File downloadScript = createExecutableScriptFile(temporaryDirectory, "download",
                    downloadCommand);

            Process process = Runtime.getRuntime().exec(downloadScript.getAbsolutePath());
            String errorMessage = "Error: \n";
            InputStream is = process.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                errorMessage += line;
            }
            if (process.waitFor() == 0) {
                downloadScript.delete();
                assert temporaryDirectory.listFiles().length == 1;
                File installerFile = temporaryDirectory.listFiles()[0];
                String installCommand = getInstallCommands(installerFile.getAbsolutePath());
                File installScript = createExecutableScriptFile(temporaryDirectory, "install",
                        installCommand);
                UpdateSettings.AUTO_UPDATE_COMMAND.set(installScript.getCanonicalPath());
                downloadSuccess = true;
            } else {
                LOG.error(errorMessage);
            }
        } catch (InterruptedException ex) {
            LOG.error("error downloading update.", ex);
        } catch (IOException io) {
            LOG.error("error downloading update.", io);
        } catch (AssertionError err) {
            LOG.error("Temporary working directory corrupt.", err);
        }
        return downloadSuccess;
    }

    @Override
    public File getAutoUpdateCommandScript() {
        String downloadCommand = getDownloadCommand();
        File downloadScript = createExecutableScriptFile(getTemporaryWorkingDirectory(),
                "download", downloadCommand);
        return downloadScript;
    }
    
    /**
     * bitrock autoupdate options for update download.
     */
    private String getDownloadCommand(){
        String cmd = getAutoupdateExecutablePath() + 
                    " --mode unattended" +
                    " --unattendedmodebehavior download" +
                    " --unattendedmodeui  minimal" +
                    " --version_id 0" +
                    " --check_for_updates 1" +
                    " --url " + UpdateSettings.AUTO_UPDATE_XML_URL.get() +
                    " --update_download_location \"${system_temp_directory}\""; 
        return cmd;
    }
    
    /**
     * returns platform specific install commands to run the downloaded executables.
     */
    private String getInstallCommands(String installerPath){
        installerPath = escapeSpecialPathCharacters(installerPath);
        String cmd = null;
        if(OSUtils.isLinux()){
            /* Invokes gdebi-gtk, a GUI tool to install debian packages. 
             * This tool does not start limewire post install/update, so we need
             * to start limewire from this script
             */
            cmd = "gdebi-gtk " + installerPath + "; limewire";
        }else if(OSUtils.isMacOSX()){
            /*
             * Mounts the downloaded dpkg installer. After user completes the 
             * installation steps start the limewire application.
             */
            cmd = "hdiutil attach " + installerPath + "; limewire.app";
        }else if(OSUtils.isWindows()){
            /*
             * Invoke the downloaded NSIS installer in silent mode. The installer
             * starts the limewire client at the end of the installation process.
             */
            cmd = installerPath + " /S ";
        }else{
            cmd = installerPath;
        }
        return cmd;
    }
    
    /**
     * creates a new executable file with the contents in the specified directory.
     */
    private File createExecutableScriptFile(File directory, String fileName, String commands){
        /*
        if(directory.exists() && !directory.isDirectory() && directory.canWrite()){
            throw new IllegalArgumentException("");
        }
        */
        File execFile = new File(directory, fileName+getExecutableScriptFileExtension()); 
        FileWriter fStream = null;
        BufferedWriter out = null;
        try{
            execFile.createNewFile();
            fStream = new FileWriter(execFile);
            out = new BufferedWriter(fStream);
            out.write(commands);
            execFile.setExecutable(true, false);
        }catch(IOException ex){
            LOG.error("Error creating executable file.", ex);
        }finally{
            FileUtils.close(out);
            FileUtils.close(fStream);
        }
        return execFile;
    }
    
    /**
     * returns platform specific executable script file extension.
     */
    private String getExecutableScriptFileExtension(){
        if(OSUtils.isLinux() || OSUtils.isMacOSX())
            return ".sh";
        else if(OSUtils.isWindows())
            return ".bat";
        else
            return "";
    }
    
    /**
     * returns the path of bit-rock auto-update executable
     */
    private String getAutoupdateExecutablePath(){
        String executablePath = null; 
        String limewireDirectory = System.getProperty("jna.library.path");
        //String limewireDirectory = CommonUtils.getCurrentDirectory().getAbsolutePath();
        if(OSUtils.isLinux())
            executablePath = limewireDirectory + File.separator + "autoupdate-linux.bin";
        else if(OSUtils.isMacOSX())
            executablePath = limewireDirectory + File.separator + "autoupdate-osx.app";
        else if(OSUtils.isWindows())
            executablePath = limewireDirectory + File.separator + "autoupdate-windows.exe";

        return escapeSpecialPathCharacters(executablePath);
    }
    
    /**
     * encloses the file path in quotes so that special character like
     * <space>, $, etc do not interfere during command execution.
     */
    private String escapeSpecialPathCharacters(String pathStr){
        if(OSUtils.isLinux() || OSUtils.isMacOSX()){
            return "'" + pathStr + "'";
        }else if(OSUtils.isWindows()){
            return "\"" + pathStr + "\"";
        }
        return pathStr;
    }

}
