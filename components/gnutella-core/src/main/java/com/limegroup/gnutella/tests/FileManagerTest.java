package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.SettingsManager;

/**
 * This test will load all the files into the FileManager and then dump the
 * results.  Directories and extensions are obtained from the SettingsManager.
 */
public class FileManagerTest
{
    public static void main(String[] args)
    {
        SettingsManager.instance(); // Force initialization
        FileManager fileManager = FileManager.instance();
        // It helps that we can assume that no files have been removed
        System.out.println("Number of files: " + fileManager.getNumFiles());
        for(int i = 0; i < fileManager.getNumFiles(); i++)
        {
            FileDesc fileDesc = fileManager.get(i);
            System.out.println("Name: " + fileDesc.getName());
            System.out.println("Index: " + fileDesc.getIndex());
            System.out.println("Size: " + fileDesc.getSize());
            System.out.println("Path: " + fileDesc.getPath());
            System.out.println("Metadata: " + fileDesc.getMetadata());
        }
    }
}
