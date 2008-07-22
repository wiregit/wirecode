package org.limewire.xmpp.client;

import java.io.File;
import java.io.FileFilter;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.Iterator;
import java.util.ArrayList;

import org.limewire.xmpp.client.service.LibraryProvider;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileTransferMetaData;
import org.limewire.io.IOUtils;

import com.google.inject.Provider;

public class LibraryProviderImpl implements LibraryProvider, Provider<LibraryProvider> {
    File lib;
    File saveDir;
    
    public LibraryProviderImpl() throws IOException {
        this.lib = createMockLibrary();
        saveDir = new File(System.getProperty("java.io.tmpdir"), "saveDir" + new Random().nextInt());
        saveDir.mkdirs();
        saveDir.deleteOnExit();
    }
    
    public Iterator<FileMetaData> getFiles() {
        ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
        File [] toAdd = lib.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        for(File f : toAdd) {
            files.add(toFile(f));
        }
        return files.iterator();
    }

    public FileTransferMetaData readFile(FileMetaData file) {
        File toRead = new File(lib, file.getName());
        return null;//new FileInputStream(toRead);
    }

    public OutputStream writeFile(FileMetaData file) throws IOException {
        File toWrite = new File(saveDir, file.getName());
        toWrite.createNewFile();
        toWrite.deleteOnExit();
        return new FileOutputStream(toWrite);
    }
    
    private FileMetaDataImpl toFile(File f) {
        return new FileMetaDataImpl(new Random().nextInt() + "", f.getName());
    }

    public LibraryProvider get() {
        return this;
    }
    
    private File createMockLibrary() throws IOException {
        File lib = new File(new File(System.getProperty("java.io.tmpdir")), "lib" + new Random().nextInt());
        lib.mkdirs();
        lib.deleteOnExit();
        for(int i = 0; i < 5; i++) {
            File file = new File(lib, "file" + i);
            file.createNewFile();
            file.deleteOnExit();  
            FileOutputStream fos = new FileOutputStream(file);
            writeMockData(fos);
            IOUtils.close(fos);
        }
        return lib;
    }
    
    private void writeMockData(FileOutputStream fos) throws IOException {
        for(int i = 0; i < 10; i++) {
            fos.write(new Random().nextInt());
        }
    }
}
