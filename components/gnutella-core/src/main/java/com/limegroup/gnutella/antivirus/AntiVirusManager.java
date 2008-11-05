package com.limegroup.gnutella.antivirus;

import java.io.File;
import org.limewire.core.settings.DownloadSettings;

public class AntiVirusManager {

    public static enum VirusScanResult {
        CLEAN,
        INFECTED,
        UNKNOWN
    };
    
    public static VirusScanResult scan(File file) {
        String[] cmd = DownloadSettings.VIRUS_SCANNER.getValue();
        String[] cmd1 = new String[cmd.length + 1];
        for(int i = 0; i < cmd.length; i++)
            cmd1[i] = cmd[i];
        cmd1[cmd.length] = file.getAbsolutePath();
        try {
            Process p = Runtime.getRuntime().exec(cmd1);
            int exitCode = p.waitFor();
            if(exitCode == 0)
                return VirusScanResult.CLEAN;
            else if(exitCode == 1)
                return VirusScanResult.INFECTED;
            else
                return VirusScanResult.UNKNOWN;
        } catch(Exception x) {
            return VirusScanResult.UNKNOWN;
        }
    }
}