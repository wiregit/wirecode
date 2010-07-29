package org.limewire.ui.swing.update;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.limewire.core.api.updates.AutoUpdateHelper;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;

public class AutoUpdateExecutableInvoker {
    
    private static final String TITLE = I18n.tr("LimeWire Update Process");

    private static JFrame MAIN_FRAME = null;
    
    private static Icon LIME_ICON = null;
    
    /**
     * Initiates auto-update process.  If the <code>promptAndExit</code> indicator
     * is true, a user prompt is displayed, the process is started, and the 
     * application shuts down.  If the indicator is false, the process is started
     * and the method returns.
     */
    public static void initiateUpdateProcess(final boolean promptAndExit) {
        String updateCommand = UpdateSettings.AUTO_UPDATE_COMMAND.get();
        final String[] args = updateCommand.split(AutoUpdateHelper.SEPARATOR);
        
        MAIN_FRAME = new JFrame();
        MAIN_FRAME.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        URL limeLogo = ClassLoader.getSystemResource("org/limewire/ui/swing/mainframe/resources/icons/lime_32.png");
        LIME_ICON = new ImageIcon(limeLogo);
        
        if (!promptAndExit) {
            if (args.length > 0) {
                invokeDownloadProcess(args);
            }
            return;
        }
               
        SwingUtils.invokeNowOrWait(new Runnable() {           
            @Override
            public void run(){
                if(args.length > 0){
                    int beginDownload = JOptionPane.showOptionDialog(AutoUpdateExecutableInvoker.MAIN_FRAME, 
                           I18n.tr("A new version of LimeWire is available for download. Click 'Yes', to continue."),
                            AutoUpdateExecutableInvoker.TITLE,
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                            AutoUpdateExecutableInvoker.LIME_ICON, null, JOptionPane.YES_OPTION);
                    if(beginDownload == JOptionPane.YES_OPTION){
                        invokeDownloadProcess(args);
                    }
                }
                System.exit(0);
            }
        });
    }
    
    private static void invokeDownloadProcess(String[] args){
        
        try{          
            String[] silentDownloadArgs = new String[]{"--mode", "unattended", 
                                                       "--unattendedmodebehavior", "download",
                                                       "--unattendedmodeui", "minimal",
                                                       "--check_for_updates", "1",
                                                       "--version_id", "0",
                                                       "--update_download_location", "${system_temp_directory}",
                                                       "--settings_file", createEmptyUpdateINIFile().getAbsolutePath()
                                                       };

            String[] newargs = new String[args.length + silentDownloadArgs.length];
            System.arraycopy(args, 0, newargs, 0, args.length);
            System.arraycopy(silentDownloadArgs, 0, newargs, args.length, silentDownloadArgs.length);
            
            if(OSUtils.isWindowsVista() || OSUtils.isWindows7()){
                StringBuilder sb = new StringBuilder();
                for(int i=1;i<newargs.length;i++){
                    sb.append(newargs[i]).append(" ");
                }
                SystemUtils.openFile(args[0], sb.toString());
            }else{
                 Runtime.getRuntime().exec(newargs);
            }
        }catch(IOException ex){
            displayErrorMessage(I18n.tr("There was a problem downloading the new version. Restart LimeWire to try again."));
        }
    }
    
    private static void displayErrorMessage(final String errorMessage){       
        JOptionPane.showMessageDialog(AutoUpdateExecutableInvoker.MAIN_FRAME,
                errorMessage, AutoUpdateExecutableInvoker.TITLE, 
                JOptionPane.ERROR_MESSAGE, AutoUpdateExecutableInvoker.LIME_ICON);
    }
    
    private static File createEmptyUpdateINIFile() throws IOException{
        
        File update = new File(CommonUtils.getUserSettingsDir(), "LimeWireUpdate.ini");
        
        if( !update.exists() ){
            update.createNewFile();
            update.setWritable(true, false);
            FileWriter fstream = new FileWriter(update);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("[Update]");
        }
        
        return update;
    }

}
