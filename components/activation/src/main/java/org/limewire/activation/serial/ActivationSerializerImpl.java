package org.limewire.activation.serial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;

/**
 * Serializes and Deserializes ActivationModules to and from Disk.
 */
public class ActivationSerializerImpl implements ActivationSerializer {

    private static final Log LOG = LogFactory.getLog(ActivationSerializerImpl.class);
    
    private final ActivationSerializerSettings settings;
    
    @Inject
    public ActivationSerializerImpl(ActivationSerializerSettings settings){
        this.settings = settings;
    }
    
    @Override
    public List<ActivationMemento> readFromDisk() throws IOException {
        if(!settings.getSaveFile().exists() && !settings.getSaveFile().exists())
            return Collections.emptyList();
        
        Throwable exception;
        ObjectInputStream in = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(settings.getSaveFile())));
            return GenericsUtils.scanForList(in.readObject(), ActivationMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            exception = ignored;
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        // Falls through to here only on error with normal file.
        
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(settings.getBackupFile())));
            return GenericsUtils.scanForList(in.readObject(), ActivationMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        if(exception instanceof IOException)
            throw (IOException)exception;
        else
            throw (IOException)new IOException().initCause(exception);
    }

    @Override
    public synchronized boolean writeToDisk(List<ActivationMemento> mementos) {
        return FileUtils.writeWithBackupFile(mementos, settings.getBackupFile(), settings.getSaveFile(), LOG);
    }
}
