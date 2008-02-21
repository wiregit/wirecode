package com.limegroup.gnutella;

import java.io.File;

public final class TestUtil {
    
    public final void deleteAllFiles(File dataDir, File saveDir) {
        if (!dataDir.exists())
            return;

        File[] files = dataDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (files[i].getName().equalsIgnoreCase("incomplete"))
                    deleteDirectory(files[i]);
                else if (files[i].getName().equals(saveDir.getName()))
                    deleteDirectory(files[i]);
            }
        }
        dataDir.delete();
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
            files[i].delete();
        dir.delete();
    }
}
