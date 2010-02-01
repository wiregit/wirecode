package org.limewire.activation.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.activation.api.ActSettings;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.impl.ActivationResponse.Type;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.collection.Periodic;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.InvalidDataException;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * 
 */
@EagerSingleton
class ActivationManagerImpl implements ActivationManager, Service {
    
    private static final Log LOG = LogFactory.getLog(ActivationManagerImpl.class);

    private static final String validChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    
    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();

    private final ActivationModel activationModel;
    private final ScheduledExecutorService scheduler;
    private final ActivationCommunicator activationCommunicator;
    private final ActivationSerializer activationSerializer;
    private final ActivationResponseFactory activationResponseFactory;
    private final ActSettings activationSettings;
    private Periodic activationContactor = null;
    
    // interval in seconds between successive hits to the activation server
    private long refreshIntervalSeconds = 0;
    
    private enum State {
        NOT_ACTIVATED(ActivationState.NOT_AUTHORIZED),
        ACTIVATING(ActivationState.AUTHORIZING),
        REFRESHING(ActivationState.REFRESHING),
        ACTIVATED_FROM_SERVER(ActivationState.AUTHORIZED),
        ACTIVATED_FROM_DISK(ActivationState.AUTHORIZED);
        
        private ActivationState state;
        
        State(ActivationState state) {
            this.state = state;
        }
        
        public ActivationState getActivationState() {
            return state;
        }
    }

    private volatile ActivationError activationError = ActivationError.NO_ERROR;
    private volatile State lastState = State.NOT_ACTIVATED;
    private volatile State currentState = State.NOT_ACTIVATED;
    
    @Inject
    public ActivationManagerImpl(@Named("fastExecutor") ScheduledExecutorService scheduler,
                                 ActivationCommunicator activationCommunicator,
                                 ActivationModel activationModel, ActivationSerializer activationSerializer,
                                 ActivationResponseFactory activationReponseFactory,
                                 ActSettings activationSettings) {
        this.activationModel = activationModel;
        this.scheduler = scheduler;
        this.activationCommunicator = activationCommunicator;
        this.activationSerializer = activationSerializer;
        this.activationResponseFactory = activationReponseFactory;
        this.activationSettings = activationSettings;
    }
    
    @Override
    public void activateKey(final String key) {
        if (!activateKeyInitAndValidate(key)) {
            return;    
        }

        transitionToState(State.ACTIVATING);

        scheduleServerQueriesForKey(key, 0);
    }

    @Override
    public void refreshKey(final String key) {
        if (!activateKeyInitAndValidate(key)) {
            return;    
        }

        transitionToState(State.REFRESHING);

        scheduleServerQueriesForKey(key, 0);
    }
    
    private void activateKeyAtStartup(final String key) {
        if (!activateKeyInitAndValidate(key)) {
            return;    
        }

        if(currentState != State.ACTIVATED_FROM_DISK)
            transitionToState(State.REFRESHING);
        
        scheduleServerQueriesForKey(key, 5);
    }

    private void scheduleServerQueriesForKey(final String key, final int numberOfRetries) {
        activationContactor = new Periodic(new ActivationTask(key, numberOfRetries), scheduler);
        activationContactor.rescheduleIfSooner(0);
    }

    /**
     * Initialize and validate key before activating at server.
     * 
     * @param key activation key
     * @return true if the activation manager should continue to 
     * activate at the server, false otherwise
     */
    private boolean activateKeyInitAndValidate(String key) {
        // cancel any existing scheduled activation 
        // which is currently not contacting the server
        if (activationContactor != null) {
            if(currentState == State.ACTIVATING || currentState == State.REFRESHING)
                return false;
            else
                activationContactor.unschedule();
        }

        if (!isValidKey(key)) {
            transitionToState(State.NOT_ACTIVATED, activationResponseFactory.createErrorResponse(Type.NOTFOUND));
            return false;
        }
        refreshIntervalSeconds = 0;
        return true;
    }

    @Override
    public ActivationState getActivationState() {
        return currentState.getActivationState();
    }
    
    private void setCurrentState(State newState) {
        lastState = currentState;
        currentState = newState;
    }
    
    @Override
    public ActivationError getActivationError() {
        return activationError;
    }

    @Override
    public List<ActivationItem> getActivationItems() {
        return activationModel.getActivationItems();
    }
    
    public void setActivationItems(List<ActivationItem> items) {
        activationModel.setActivationItems(items);
    }

    @Override
    public String getLicenseKey() {
        return activationSettings.getActivationKey();
    }
    
    @Override
    public String getMCode() {
        return activationSettings.getMCode();
    }

    @Override
    public boolean isValidKey(String key) {
        if (key == null || key.length() != 12)
            return false;
        
        String givenChecksum = key.substring(key.length()-1, key.length());
        String keyPart = key.substring(0, key.length()-1);
        
        int sum = 0;
        for (int counter = 0; counter < keyPart.length(); counter++) {
            char currentChar = keyPart.charAt(counter);
            int positionInValidChars = validChars.indexOf(currentChar);
            
            if (positionInValidChars == -1) {
                return false;
            } else {
                sum += positionInValidChars;
            }
        }

        int modulusOfSum = sum % validChars.length();
        char correctChecksum = validChars.charAt(modulusOfSum);

        return givenChecksum.equals(String.valueOf(correctChecksum));
    }

    @Override
    public boolean isActive(ActivationID id) {
        return activationModel.isActive(id);
    }
       
    @Override
    public boolean isProActive() {
        return activationModel.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE) ||
               activationModel.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE) ||
               activationModel.isActive(ActivationID.TECH_SUPPORT_MODULE);
    }

    @Override
    public String getServiceName() {
        return "Activation-Manager Service";
    }
    
    @Inject
    public void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void initialize() {
        loadFromDisk();
    }
    
    /**
     * Attempts to load any serialized module on disk into the client. This will
     * precede any communication with the server to try and enable any modules
     * as quickly as possible. 
     */
    private void loadFromDisk() {
        scheduler.execute(new Runnable(){
            public void run() {
                try {
                    String jsonString = activationSerializer.readFromDisk();
                    if(jsonString != null && jsonString.length() > 0) { 
                        ActivationResponse response = activationResponseFactory.createFromDiskJson(jsonString);
                        transitionToState(State.ACTIVATED_FROM_DISK, response);
                    }
                } catch (IOException e) {
                    if(LOG.isErrorEnabled())
                        LOG.error("Error reading serialized json string.");
                } catch (InvalidDataException e) {
                    if(LOG.isErrorEnabled())
                        LOG.error("Error parsing json string.");
                }
            }
        });
    }

    @Override
    public void start() {
        // if a PKey exists, start the server and try contacting the authentication server
        String storedLicenseKey = getLicenseKey();
        if (!storedLicenseKey.isEmpty()) {
            activateKeyAtStartup(storedLicenseKey);
        }
    }

    @Override
    public void stop() {
        if (activationContactor != null) {
            activationContactor.unschedule();
        }
    }

    @Override
    public void addModuleListener(EventListener<ActivationModuleEvent> listener) {
        activationModel.addListener(listener);
    }
    
    @Override
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener) {
        return activationModel.removeListener(listener);
    }
        
    @Override
    public void addListener(EventListener<ActivationEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<ActivationEvent> listener) {
        return listeners.removeListener(listener);
    }
            
    private class ActivationResponseProcessor {
        private final static int MAX_ATTEMPTS = 5;
        private int numAttempted = 0;
        private final int numRetries;
        
        ActivationResponseProcessor(int numRetries) {
            this.numRetries = numRetries;    
        }
        
        public void processResponse(ActivationResponse response, Throwable error) {
            if (error != null) {
                if (numRetries > 0) {
                    if (error instanceof IOException) {
                        if (++numAttempted <= MAX_ATTEMPTS) {
                            int nextDelay = numAttempted * 5000;
                            transitionToState(State.NOT_ACTIVATED, activationResponseFactory.createErrorResponse(Type.ERROR));
                            activationContactor.rescheduleIfSooner(nextDelay);
                            return;
                        }
                    }
                }

                transitionToState(State.NOT_ACTIVATED, activationResponseFactory.createErrorResponse(Type.ERROR));
                return;
            }
            processActivationResponse(response);
        }
        
        private void processActivationResponse(ActivationResponse response) {
            if (response != null && response.getResponseType() == ActivationResponse.Type.VALID) {
                transitionToState(State.ACTIVATED_FROM_SERVER, response);
                
                // reset counter
                numAttempted = 0;
                
                // reschedule next ping of activation server if necessary
                long refreshVal = response.getRefreshInterval();
                if (refreshVal > 0) {
                    refreshIntervalSeconds = refreshVal;
                    activationContactor.rescheduleIfSooner(refreshIntervalSeconds*1000);
                }
            } else if(response != null){
                transitionToState(State.NOT_ACTIVATED, response);
            } else {
                transitionToState(State.NOT_ACTIVATED, activationResponseFactory.createErrorResponse(Type.ERROR));
            }
        }
    }
    
    private class ActivationTask implements Runnable {
        
        private final ActivationResponseProcessor responseProcessor;
        private final String key;

        ActivationTask(final String key, int numRetries) {
            this.responseProcessor = new ActivationResponseProcessor(numRetries);
            this.key = key;
        }

        @Override
        public void run() {
            ActivationResponse response = null;
            Throwable error = null;
            try {
                response = activationCommunicator.activate(key);
            } catch (Throwable e) {
                error = e;
            } finally {
                responseProcessor.processResponse(response, error);
            }
        }
    }

    private void transitionToState(State newState) {
        assert newState != State.ACTIVATED_FROM_DISK && newState != State.ACTIVATED_FROM_SERVER && 
                newState != State.NOT_ACTIVATED;
        transitionToState(newState, null);
    }
    
    private void transitionToState(State newState, ActivationResponse response) {
        synchronized(this) {
            switch(newState) {
                case NOT_ACTIVATED:
                    notActivated(response);
                    break;
                case ACTIVATING:
                    activating();
                    break;
                case REFRESHING:
                    refreshing();
                    break;
                case ACTIVATED_FROM_DISK:
                    if(currentState == State.ACTIVATED_FROM_SERVER)
                        return;
                    else
                        activatedFromDisk(response);
                    break;
                case ACTIVATED_FROM_SERVER:
                    activated(response);
                    break;
                default:
                    throw new IllegalStateException("Unknown state " + newState);
            }
        }
        listeners.broadcast(new ActivationEvent(getActivationState(), getActivationError()));
    }
    
    private void notActivated(ActivationResponse response) {
        ActivationError error;
        boolean removeMcode = false;
        boolean removeKey = false;
        switch(response.getResponseType()) {
            case ERROR:
                if(currentState == State.REFRESHING) {
                    activationError = ActivationError.COMMUNICATION_ERROR;
                    setCurrentState(lastState);
                    return;
                } else {
                    error = ActivationError.COMMUNICATION_ERROR;
                }
                break;
            case REMOVE:
                error = ActivationError.INVALID_KEY;
                removeMcode = true;
                removeKey = true;
                break;
            case STOP:
                error = ActivationError.INVALID_KEY;
                removeKey = true;
                break;
            case NOTFOUND:
                error = ActivationError.INVALID_KEY;
                removeMcode = true;
                removeKey = true;
                break;
            case BLOCKED:
                activationSettings.setActivationKey(response.getLid());
                error = ActivationError.BLOCKED_KEY;
                break;
            default:
                throw new IllegalStateException("Unknown state " + response.getResponseType());
        }
        if(!(currentState.getActivationState() == ActivationState.AUTHORIZED 
                && error == ActivationError.COMMUNICATION_ERROR)) {
            if(error == ActivationError.INVALID_KEY || error == ActivationError.BLOCKED_KEY)
                removeData(removeMcode, removeKey);
            activationError = error;
            setCurrentState(State.NOT_ACTIVATED);
            activationSettings.setLastStartPro(false);
        }
    }
        
    private void removeData(boolean removeMCode, boolean removeKey) {
        if(removeKey)
            activationSettings.setActivationKey("");
        scheduler.execute(new Runnable(){
            public void run() {
                try {
                    activationSerializer.writeToDisk("");                        
                } catch (Exception e) {
                    if(LOG.isErrorEnabled())
                        LOG.error("Error saving json string to disk.");
                }
            }
        });
        setActivationItems(Collections.<ActivationItem>emptyList());
        if(removeMCode)
            activationSettings.setMCode("");
    }
    
    private void refreshing() {
        activationError = ActivationError.NO_ERROR;
        setCurrentState(State.REFRESHING);
    }
    
    private void activating() {
        activationError = ActivationError.NO_ERROR;
        setCurrentState(State.ACTIVATING);
    }
    
    private void activatedFromDisk(final ActivationResponse response) {
        setActivationItems(response.getActivationItems());
        activationSettings.setLastStartPro(isProActive());
        activationError = ActivationError.NO_ERROR;
        setCurrentState(State.ACTIVATED_FROM_DISK);
    }
    
    private void activated(final ActivationResponse response) {
        scheduler.execute(new Runnable(){
            public void run() {
                try {
                    activationSerializer.writeToDisk(response.getJSONString());                        
                } catch (Exception e) {
                    if(LOG.isErrorEnabled())
                        LOG.error("Error saving json string to disk.");
                }
            }
        });

        activationSettings.setActivationKey(response.getLid());
        activationSettings.setMCode(response.getMCode());
        setActivationItems(response.getActivationItems());
        activationSettings.setLastStartPro(isProActive());
        activationError = ActivationError.NO_ERROR;
        setCurrentState(State.ACTIVATED_FROM_SERVER);
    }
}
