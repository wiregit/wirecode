package org.limewire.core.impl.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.limewire.core.api.browser.LoadURLEvent;
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
import com.amazon.s3.HeadResponse;
import com.amazon.s3.ListBucketResponse;
import com.amazon.s3.ListEntry;
import com.amazon.s3.Response;
import com.amazon.s3.S3Object;
import com.amazonaws.ls.AmazonLSException;
import com.amazonaws.ls.http.AmazonLSQuery;
import com.amazonaws.ls.model.ActivateDesktopProduct;
import com.amazonaws.ls.model.ActivateDesktopProductResponse;
import com.amazonaws.ls.model.ActivateDesktopProductResult;
import com.amazonaws.ls.model.VerifyProductSubscriptionByTokens;
import com.amazonaws.ls.model.VerifyProductSubscriptionByTokensResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

@Singleton
public class BackupManager implements Service {

    private static final Log LOG = LogFactory.getLog(BackupManager.class);

    private final LibraryManager libraryManager;
    private final EventListener<LoadURLEvent> loadURLListener;

    @Inject
    public BackupManager(LibraryManager libraryManager, EventListener<LoadURLEvent> loadURLListener){
        this.libraryManager = libraryManager;
        this.loadURLListener = loadURLListener;
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

    private ActivateDesktopProductResult activate(Properties awsProps) throws AmazonLSException, URISyntaxException, InterruptedException, IOException {
        if(awsProps.getProperty("activationKey") == null) {
            loadURLListener.handleEvent(new LoadURLEvent(new URI(awsProps.getProperty("purchaseURL"))));
            // TODO offer a choice to show activation page, for existing customers?
        }
        while(awsProps.getProperty("activationKey") == null) {
            Thread.sleep(30 * 1000);
            InputStream awsPropStream = new FileInputStream(new File("aws.properties"));
            awsProps.load(awsPropStream);
        }

        AmazonLSQuery lsQuery = new AmazonLSQuery("dummy", "dummy");
        ActivateDesktopProductResponse response = lsQuery.activateDesktopProduct(getActivateDesktopProductAction(awsProps.getProperty("productToken"), awsProps.getProperty("activationKey")));
        return response.getActivateDesktopProductResult();
    }

    private ActivateDesktopProduct getActivateDesktopProductAction(String productToken, String activationKey) {
        ActivateDesktopProduct product = new ActivateDesktopProduct();
        product.setProductToken(productToken);
        product.setActivationKey(activationKey);
        return product;
    }

    public void backup() throws IOException, AmazonLSException, URISyntaxException, InterruptedException {
        InputStream awsPropStream = new FileInputStream(new File("aws.properties"));
        if(awsPropStream != null) {
            Properties awsProps = initBackupService(awsPropStream);
            LibraryFileList managedFiles = libraryManager.getLibraryManagedList();
            List<String> securityTokens = new ArrayList<String>();
            securityTokens.add(awsProps.getProperty("userToken"));
            securityTokens.add(awsProps.getProperty("productToken"));
            AWSAuthConnection connection = new AWSAuthConnection(awsProps.getProperty("id"), awsProps.getProperty("password"), securityTokens);
            createLimewireLibraryBucketIfNecessary(connection, awsProps);
            deleteRemovedEntries(managedFiles, connection, awsProps);
            uploadNewOrUpdatedFiles(managedFiles, connection, awsProps);
        }
    }

    private Properties initBackupService(InputStream awsPropStream) throws IOException, AmazonLSException, URISyntaxException, InterruptedException {
        Properties awsProps = new Properties();
        awsProps.load(awsPropStream);
        String userToken = awsProps.getProperty("userToken");
        if(userToken == null ||
                !verifySubscription(awsProps).getVerifyProductSubscriptionByTokensResult().isSubscribed()) {
            ActivateDesktopProductResult activateResult = activate(awsProps);
            awsPropStream = new FileInputStream(new File("aws.properties"));
            awsProps.load(awsPropStream);
            awsProps.setProperty("id", activateResult.getAWSAccessKeyId());
            awsProps.setProperty("password", activateResult.getSecretAccessKey());
            awsProps.setProperty("userToken", activateResult.getUserToken());
            awsProps.store(new FileOutputStream(new File("aws.properties")), null);
        }
        return awsProps;
    }

    public void restore() throws IOException, URISyntaxException, AmazonLSException, InterruptedException {
        InputStream awsPropStream = new FileInputStream(new File("aws.properties"));
        if(awsPropStream != null) {
            Properties awsProps = initBackupService(awsPropStream);
            LibraryFileList managedFiles = libraryManager.getLibraryManagedList();
            List<String> securityTokens = new ArrayList<String>();
            securityTokens.add(awsProps.getProperty("userToken"));
            securityTokens.add(awsProps.getProperty("productToken"));
            AWSAuthConnection connection = new AWSAuthConnection(awsProps.getProperty("id"), awsProps.getProperty("password"), securityTokens);
            downloadMissingFiles(connection, awsProps, managedFiles);
        }
    }

    private void downloadMissingFiles(AWSAuthConnection connection, Properties awsProps, LibraryFileList managedFiles) throws IOException {
        if(checkBucketExists(connection, awsProps.getProperty("id").toLowerCase())) {
            String marker = null;
            List<ListEntry> entries;
            do {
                ListBucketResponse response = connection.listBucket(awsProps.getProperty("id").toLowerCase(),
                    null, marker, 256, null);
                entries = response.entries;
                for(ListEntry entry : entries) {
                    String fileName = entry.key;
                    marker = fileName;
                    if(!managedFiles.contains(new File(fileName))) { // TODO check by sha1 instead?
                        LOG.debugf("restoring {0} ...", fileName);
                        GetResponse getResponse = connection.get(awsProps.getProperty("id").toLowerCase(), fileName, null);
                        if(getResponse.connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            LOG.debugf("error restoring {0}.  HTTP status = {1}", fileName,
                                    getResponse.connection.getResponseCode());
                        }
                    }
                }
            } while (entries.size() > 0);
        }
    }

    private VerifyProductSubscriptionByTokensResponse verifySubscription(Properties awsProps) throws AmazonLSException {
        AmazonLSQuery lsQuery = new AmazonLSQuery(awsProps.getProperty("id"), awsProps.getProperty("password"));
        return lsQuery.verifyProductSubscriptionByTokens(getTokens(awsProps.getProperty("productToken"), awsProps.getProperty("userToken")));
    }

    private VerifyProductSubscriptionByTokens getTokens(String productToken, String userToken) {
        VerifyProductSubscriptionByTokens tokens = new VerifyProductSubscriptionByTokens();
        tokens.setProductToken(productToken);
        tokens.setUserToken(userToken);
        return tokens;
    }


    private void uploadNewOrUpdatedFiles(LibraryFileList managedFiles, AWSAuthConnection connection, Properties awsProps) {
        for(LocalFileItem file : managedFiles.getModel()) {
            try {
                HeadResponse response = connection.head(awsProps.getProperty("id").toLowerCase(), file.getFile().getCanonicalPath(), null);
                if(response.connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    uploadFile(connection, file, awsProps);
                } else if(response.connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                    Map<String, List<String>> metadata = response.metadata;
                    if(fileHasChanged(file, metadata) || !isFullyStored(response, file)) {
                        uploadFile(connection, file, awsProps);
                    }
                }  else {
                    // TODO
                }
            } catch (IOException ioe) {
                LOG.debugf(ioe, "failed to backup {0}:", file.getFileName());
            }
        }
    }

    private boolean isFullyStored(HeadResponse response, LocalFileItem file) {
        return response.connection.getContentLength() == file.getFile().length();
    }

    private boolean fileHasChanged(LocalFileItem file, Map<String, List<String>> metadata) {
        return !metadata.get("sha1").get(0).equals(file.getUrn().toString());
    }

    private void createLimewireLibraryBucketIfNecessary(AWSAuthConnection connection, Properties awsProps) throws IOException {
        if(!checkBucketExists(connection, awsProps.getProperty("id").toLowerCase())) {
            LOG.debugf("creating bucket {0} ...", awsProps.getProperty("id").toLowerCase());
            Response response = connection.createBucket(awsProps.getProperty("id").toLowerCase(), AWSAuthConnection.LOCATION_DEFAULT, null);
            if(response.connection.getResponseCode() != 200) {
                throw new IOException();
            }
        }
    }

    private boolean checkBucketExists(AWSAuthConnection connection, String limewireLibrary) throws IOException {
        HttpURLConnection response  = connection.head(limewireLibrary, "", null).connection;
        int httpCode = response.getResponseCode();
        return httpCode >= 200 && httpCode < 300;
    }

    private void deleteRemovedEntries(LibraryFileList managedFiles, AWSAuthConnection connection, Properties awsProps) throws IOException {
        String marker = null;
        List<ListEntry> entries;
        do {
            ListBucketResponse response = connection.listBucket(awsProps.getProperty("id").toLowerCase(),
                null, marker, 256, null);
            entries = response.entries;
            for(ListEntry entry : entries) {
                String fileName = entry.key;
                marker = fileName;
                if(!managedFiles.contains(new File(fileName))) {
                    LOG.debugf("removing {0} ...", fileName);
                    Response deleteResponse = connection.delete(awsProps.getProperty("id").toLowerCase(), fileName, null);
                    if(deleteResponse.connection.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                        LOG.debugf("error deleting {0}.  HTTP status = {1}", fileName,
                                deleteResponse.connection.getResponseCode());
                    }
                }
            }
        } while (entries.size() > 0);
    }

    private void uploadFile(AWSAuthConnection connection, LocalFileItem file, Properties awsProps) throws IOException {
        // TODO populate metadata with limexmldocument ?
        Map<String, List<String>> metadata = new HashMap<String, List<String>>();
        metadata.put("sha1", Arrays.asList(file.getUrn().toString()));
        S3Object backedUpFile = new S3Object(IOUtils.readFully(new FileInputStream(file.getFile())), metadata);
        LOG.debugf("uploading {0}...", file.getFileName());
        Response response = connection.put(awsProps.getProperty("id").toLowerCase(), file.getFile().getCanonicalPath(), backedUpFile, null);
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
                    //backup();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
}
