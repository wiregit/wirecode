package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ID3Tag;
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
        FileManager fileManager = FileManager.getFileManager();
        // It helps that we can assume that no files have been removed
        System.out.println("Number of files: " + fileManager.getNumFiles());
        for(int i = 0; i < fileManager.getNumFiles(); i++)
        {
            FileDesc fileDesc = fileManager.get(i);
            System.out.println("Name: " + fileDesc._name);
            System.out.println("Index: " + fileDesc._index);
            System.out.println("Size: " + fileDesc._size);
            System.out.println("Path: " + fileDesc._path);

            ID3Tag id3Tag = fileDesc._id3Tag;
            if(id3Tag != null)
            {
                System.out.println("Title: " + id3Tag.getTitle() +
                                   "(" + id3Tag.getTitle().length() + ")");
                System.out.println("Artist: " + id3Tag.getArtist() +
                                   "(" + id3Tag.getArtist().length() + ")");
                System.out.println("Album: " + id3Tag.getAlbum() +
                                   "(" + id3Tag.getAlbum().length() + ")");
                System.out.println("Year: " + id3Tag.getYear() +
                                   "(" + id3Tag.getYear().length() + ")");
                System.out.println("Comment: " + id3Tag.getComment() +
                                   "(" + id3Tag.getComment().length() + ")");
                System.out.println("Track: " + id3Tag.getTrack());
                System.out.println("Genre: " + id3Tag.getGenre());
            }

            System.out.println();
        }
    }
}
