package org.limewire.core.impl.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.amazon.s3.AWSAuthConnection;
import com.amazon.s3.GetResponse;
import com.amazon.s3.ListBucketResponse;
import com.amazon.s3.ListEntry;
import com.amazon.s3.Response;
import com.amazon.s3.S3Object;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

@Singleton
public class BackupManager implements Service {
    
    private static final Log LOG = LogFactory.getLog(BackupManager.class);

    private final LibraryManager libraryManager;
    private static final String LIMEWIRE_LIBRARY = "tjulienlibrary";

    @Inject
    public BackupManager(LibraryManager libraryManager){
        this.libraryManager = libraryManager;
    }

    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }

    @Inject
    public void register(FileManager fileManager) {
        fileManager.getManagedFileList().addManagedListStatusListener(new FinishedLoadingListener());
    }

    public void start() {
//        try {
//            backup();
//        } catch (IOException e) {
//            LOG.error(e.getMessage(), e);
//        }
    }

    public void backup() throws IOException {
        InputStream awsPropStream = getClass().getClassLoader().getResourceAsStream("org/limewire/core/impl/library/aws.properties");
        if(awsPropStream != null) {
            Properties awsProps = new Properties();
            awsProps.load(awsPropStream);
            LibraryFileList managedFiles = libraryManager.getLibraryManagedList();
            AWSAuthConnection connection = new AWSAuthConnection(awsProps.getProperty("id"), awsProps.getProperty("password"));
            createLimewireLibraryBucketIfNecessary(connection);
            deleteRemovedEntries(managedFiles, connection);
            uploadNewOrUpdatedFiles(managedFiles, connection);
        }
    }

    private void uploadNewOrUpdatedFiles(LibraryFileList managedFiles, AWSAuthConnection connection) {
        for(LocalFileItem file : managedFiles.getModel()) {
            try {
                GetResponse response = connection.get(LIMEWIRE_LIBRARY, file.getFileName(), null);
                if(response.object == null) {
                    uploadFile(connection, file);
                } else {
                    Map<String, List<String>> metadata = response.object.metadata;
                    if(!metadata.get("sha1").get(0).equals(file.getUrn().toString())) {
                        uploadFile(connection, file);
                    }
                }
            } catch (IOException ioe) {
                LOG.debugf(ioe, "failed to backup {0}:", file.getFileName());
            }
        }
    }

    private void createLimewireLibraryBucketIfNecessary(AWSAuthConnection connection) throws IOException {
        if(!connection.checkBucketExists(LIMEWIRE_LIBRARY)) {
            LOG.debugf("creating bucket {0} ...", LIMEWIRE_LIBRARY);
            Response response = connection.createBucket(LIMEWIRE_LIBRARY, AWSAuthConnection.LOCATION_DEFAULT, null);
            if(response.connection.getResponseCode() != 200) {
                throw new IOException();
            }
        }
    }

    private void deleteRemovedEntries(LibraryFileList managedFiles, AWSAuthConnection connection) throws IOException {
        String marker = null;
        List<ListEntry> entries;
        do {
            ListBucketResponse response = connection.listBucket(LIMEWIRE_LIBRARY,
                null, marker, 256, null);
            entries = response.entries;
            for(ListEntry entry : entries) {
                String fileName = entry.key;
                marker = fileName;
                if(managedFiles.getFileItem(new File(fileName)) == null) {
                    connection.delete(LIMEWIRE_LIBRARY, fileName, null);
                }
            }
        } while (entries.size() > 0);
    }

    private void uploadFile(AWSAuthConnection connection, LocalFileItem file) throws IOException {
        // TODO populate metadata with limexmldocument ?
        Map<String, List<String>> metadata = new HashMap<String, List<String>>();
        metadata.put("sha1", Arrays.asList(file.getUrn().toString()));
        S3Object backedUpFile = new S3Object(IOUtils.readFully(new FileInputStream(file.getFile())), metadata);
        LOG.debugf("uploading {0}...", file.getFileName());
        Response response = connection.put(LIMEWIRE_LIBRARY, file.getName(), backedUpFile, null);
        if(response.connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOG.debugf("failed to backup {0}:  HTTP status {1}", file.getName(), response.connection.getResponseCode());
        }
    }

    public void stop() {
    }

    public void initialize() {
    }

    public String getServiceName() {
        return getClass().getSimpleName();
    }

    class FinishedLoadingListener implements EventListener<ManagedListStatusEvent> {
        @SuppressWarnings("unchecked")
        @BlockingEvent
        public void handleEvent(ManagedListStatusEvent evt) {
            if(evt.getType() == ManagedListStatusEvent.Type.LOAD_COMPLETE) {
                try {
                    backup();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
}
