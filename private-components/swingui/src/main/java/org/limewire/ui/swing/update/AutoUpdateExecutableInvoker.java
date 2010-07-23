package org.limewire.ui.swing.update;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.limewire.ui.swing.util.I18n;

public class AutoUpdateExecutableInvoker {
    
    private static final String[] BITROCK_SILENT_DOWNLOAD_ARGS = new String[]{"--mode", "unattended", 
                                                                              "--unattendedmodebehavior", "download",
                                                                              "--unattendedmodeui", "minimal",
                                                                              "--check_for_updates", "1",
                                                                              "--version_id", "0"
                                                                             };
    
    private static final String TITLE = I18n.tr("LimeWire Update Process");

    private static JFrame MAIN_FRAME = null;
    
    private static Icon LIME_ICON = null;
    

    public static void main(final String[] args) throws InvocationTargetException, InterruptedException{
        
        MAIN_FRAME = new JFrame();
       MAIN_FRAME.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        URL limeLogo = ClassLoader.getSystemResource("org/limewire/ui/swing/mainframe/resources/icons/lime_32.png");
        LIME_ICON = new ImageIcon(limeLogo);
               
        SwingUtilities.invokeAndWait(new Runnable() {           
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
                }else{
                    displayErrorMessage(I18n.tr("Help Text"));
                }
                System.exit(0);
            }
        });
    }
    
    private static void invokeDownloadProcess(String[] args){
        String[] newargs = new String[args.length + BITROCK_SILENT_DOWNLOAD_ARGS.length];
        System.arraycopy(args, 0, newargs, 0, args.length);
        System.arraycopy(BITROCK_SILENT_DOWNLOAD_ARGS, 0, newargs, args.length, BITROCK_SILENT_DOWNLOAD_ARGS.length);
        try{
            Process p = Runtime.getRuntime().exec(newargs);
            int exitCode = p.waitFor();
            if(exitCode > 0){
                displayErrorMessage(interpretBitRockExitCode(exitCode));
            }
        }catch(IOException ex){
            displayErrorMessage(I18n.tr("There was a problem downloading the new version. Restart LimeWire to try again."));
        }catch(InterruptedException interupt){
            
        }
    }
    
    private static void displayErrorMessage(final String errorMessage){       
        JOptionPane.showMessageDialog(AutoUpdateExecutableInvoker.MAIN_FRAME,
                errorMessage, AutoUpdateExecutableInvoker.TITLE, 
                JOptionPane.ERROR_MESSAGE, AutoUpdateExecutableInvoker.LIME_ICON);
    }
    
    
    //@SuppressWarnings("unused")
    private static String interpretBitRockExitCode(int exitCode){
        String message;
        switch(exitCode){
            case 0:
                message = "Successfully downloaded and executed the installer.";
                break;
            case 1:
                message = "No updates available or update download was aborted";
                break;
            case 2:
                message = "Error connecting to remote server or invalid XML file";
                break;
            case 3:
                message = "An error occurred downloading the file";
                break;
            case 4:
                message = "An error occurred executing the downloaded update or its <postUpdateDownloadActionList>";
                break;
            case 5:
                message = "Update check disabled through check_for_updates setting";
                break;               
            default:
                message = "undefined.";
                break;
        }
        return message;
    }
 

}
