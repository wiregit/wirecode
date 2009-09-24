package org.limewire.core.impl.inspections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.InspectionsSettings;
import org.limewire.facebook.service.settings.InspectionsServerUrls;
import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.inject.EagerSingleton;
import org.limewire.inspection.Inspector;
import org.limewire.io.InvalidDataException;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.Join;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.StringSetting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * - request/receive instructions from LW server for which inspections
 *   to send and when (how frequent)
 * - receive inspections from classes with event driven inspection points
 *   (if requested by server)
 * - pre-emptively perform inspections as per server instructions 
 *   (which inspections, when and how frequent)
 */
@EagerSingleton
public class InspectionsCommunicatorImpl implements InspectionsCommunicator, Service {
    
    private static final Log LOG = LogFactory.getLog(InspectionsCommunicatorImpl.class);
    private static final String SERVICE_NAME = "Push Inspections Communicator";

    // used to schedule standard inspection tasks
    private final ScheduledExecutorService scheduler;
    
    // for performing inspections
    private final Inspector inspector;
    
    private InspectionsResultProcessor processor;
    private final ClientConnectionManager httpConnectionManager;
    private final InspectionsParser parser;
    private final Provider<Map<String, StringSetting>> inspectionsServerUrls;
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    private List<InspectionsSpec> inspectionsSpecs = new ArrayList<InspectionsSpec>();
    private final HttpClientInstanceUtils httpClientInstanceUtils;
        
    
    @Inject
    public InspectionsCommunicatorImpl(@Named("fastExecutor")ScheduledExecutorService scheduler,
                                       @Named("sslConnectionManager") ClientConnectionManager httpConnectionManager,
                                       @InspectionsServerUrls Provider<Map<String, StringSetting>> inspectionsServerUrls,
                                       Inspector inspector,
                                       HttpClientInstanceUtils httpClientInstanceUtils) {
        this.scheduler = scheduler;
        this.inspector = inspector;
        this.httpConnectionManager = httpConnectionManager;
        this.inspectionsServerUrls = inspectionsServerUrls;
        this.httpClientInstanceUtils = httpClientInstanceUtils;
        this.processor = null;
        this.parser = new InspectionsParser();
    }
    
    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this);
    }
    
    @Override
    public void setResultProcessor(InspectionsResultProcessor processor) {
        this.processor = processor;    
    }

    @Override
    public InspectionsResultProcessor getResultProcessor() {
        if (processor == null) {
            processor = new DefaultInspectionsProcessor();
        }
        return processor;
    }

    @Override
    public synchronized void initInspectionSpecs(List<InspectionsSpec> inspSpecs) {
        for (InspectionsSpec spec : inspSpecs) {
            // check inspections - skip insp. spec if no inspections.
            if (!spec.getInspectionPoints().isEmpty()) {
                spec.schedule(getResultProcessor(), inspector, scheduler);
                inspectionsSpecs.add(spec);
            }
        }
    }
    
    public synchronized void cancelInspections(List<InspectionsSpec> inspSpecs) {
        // cancel pending inspections scheduled
        for (InspectionsSpec spec : inspSpecs) {
            spec.ensureCancelled();
        }
        inspectionsSpecs.removeAll(inspSpecs);    
    }
    
    private byte[] requestInspectionsFromServer() throws IOException {
        String requestUrl = inspectionsServerUrls.get().get(
            InspectionsServerUrls.INSPECTION_SPEC_REQUEST_URL).get();
        HttpGet httpGet = new HttpGet(addClientInfoToUrl(requestUrl));
        httpGet.addHeader("Accept-Encoding", "gzip");
        httpGet.addHeader("Connection", "close");
        return executeRequest(httpGet);    
    }
    
    private byte[] sendInspectionsResultsToServer(byte[] bytesToPost) throws IOException {
        String submitUrl = inspectionsServerUrls.get().get(
                InspectionsServerUrls.INSPECTION_SPEC_SUBMIT_URL).get();
        HttpEntity postContent = new ByteArrayEntity(bytesToPost);
        HttpPost httpPost = new HttpPost(addClientInfoToUrl(submitUrl));
        httpPost.addHeader("Accept-Encoding", "gzip");
        httpPost.addHeader("Connection", "close");
        httpPost.addHeader("Content-Encoding", "gzip");
        httpPost.addHeader("Content-Type", "binary/octet-stream");
        httpPost.setEntity(postContent);
        return executeRequest(httpPost);
    }
    
    private String addClientInfoToUrl(String serverUrl) {
        boolean usage = ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.get();
        return httpClientInstanceUtils.addClientInfoToUrl(serverUrl) + "&urs=" + Boolean.toString(usage);    
    }
    
    
    /********** HTTP methods **********/

    /**
     * Executes the http request, returning the response as byte[]
     * @param request http request
     * @return byte[] response
     * @throws java.io.IOException for but a 200 response.
     */
    private byte[] executeRequest(HttpUriRequest request) throws IOException {
        // if inspections were disabled, stop everything
        if (!InspectionsSettings.PUSH_INSPECTIONS_ON.getBoolean()) {
            stop();
            return new byte[0];
        }
        HttpClient httpClient = new DefaultHttpClient(httpConnectionManager, null);
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (statusCode != 200 || entity == null) {
            throw new IOException("invalid http response, status: " + statusCode 
                + ", entity: " + ((entity != null) ? EntityUtils.toString(entity) : "none"));
        }
        byte[] responseBytes = EntityUtils.toByteArray(entity);
        HttpClientUtils.releaseConnection(response);
        return responseBytes;
    }



    /********** Service methods **********/
    
    /**
     * async because we contact the http server, retrying upon failure
     */
    @Asynchronous(join = Join.NONE)
    @Override
    public void start() {
        if (InspectionsSettings.PUSH_INSPECTIONS_ON.getBoolean()) {
            started.set(true);
            // contact server, get insp. instructions
            List<InspectionsSpec> specs = Collections.emptyList();
            try {
                byte[] rawInspectionSpecs = requestInspectionsFromServer();
                specs = parser.parseInspectionSpecs(rawInspectionSpecs);
            } catch (IOException e) {
                LOG.error("Error in getting inspections specifications from server", e);
            } catch (InvalidDataException e) {
                LOG.error("Error in getting inspections specifications from server", e);
            }

            if (!specs.isEmpty()) {
                initInspectionSpecs(specs);
            }
        }
    }

    @Override
    public void stop() {
        // todo: send any unsent (such as previously failed, or not yet sent) inspections results
        if (started.get()) {
            // cancel any pending inspections scheduled
            cancelInspections(inspectionsSpecs);
        }
    }

    @Override
    public void initialize() { }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    // default implementation currently just logs and sends inspection results to server
    //
    // todo: another, better implementation can add to a queue, and a separate thread is responsible for all sending
    private class DefaultInspectionsProcessor implements InspectionsResultProcessor {

        @Override
        public void inspectionsPerformed(InspectionDataContainer insps) throws InspectionProcessingException {
            try {
                byte[] bytesToSend = parser.inspectionResultToByteArray(insps);
                sendInspectionsResultsToServer(bytesToSend);
            } catch (IOException e) {
                LOG.debug("Error sending inspections results to server", e);
                throw new InspectionProcessingException(e);
            }
        }
    }
}