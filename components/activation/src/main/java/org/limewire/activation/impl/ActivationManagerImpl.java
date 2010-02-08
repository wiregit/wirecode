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
import org.limewire.activation.api.MCodeEvent;
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

@EagerSingleton
class ActivationManagerImpl implements ActivationManager, Service {
    
    private static final Log LOG = LogFactory.getLog(ActivationManagerImpl.class);

    private static final String validChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    
    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();
    private final EventListenerList<MCodeEvent> mcodeListeners = new EventListenerList<MCodeEvent>();

    private final ActivationModel activationModel;
    private final ScheduledExecutorService scheduler;
    private final ActivationCommunicator activationCommunicator;
    private final ActivationSerializer activationSerializer;
    private final ActivationResponseFactory activationResponseFactory;
    private final ActSettings activationSettings;
    private Periodic activationContactor = null;
        
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
    private volatile boolean attemptedToContactActivationServer = false;
    
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

        if (key == null || key.equals("")) {
            transitionToState(State.NOT_ACTIVATED, null);
            return false;
        }
        
        if (!isValidKey(key)) {
            transitionToState(State.NOT_ACTIVATED, activationResponseFactory.createErrorResponse(Type.NOTFOUND));
            return false;
        }
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
    
    public boolean isMCodeUpToDate() {
        return activationSettings.getActivationKey().isEmpty() || attemptedToContactActivationServer;
    }

    @Override
    public ActivationError getActivationError() {
        return activationError;
    }

    @Override
    public List<ActivationItem> getActivationItems() {
        return activationModel.getActivationItems();
    }
    
    private void setActivationItems(List<ActivationItem> items) {
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

    /**
     * Asynchronously serializes to disk.
     * 
     * @param moduleInfoAsJson contents of activation response as a json
     */
    private void writeToDisk(final String moduleInfoAsJson) {
        scheduler.execute(new Runnable(){
            public void run() {
                try {
                    activationSerializer.writeToDisk(moduleInfoAsJson);                        
                } catch (Exception e) {
                    // todo: maybe invoke an error callback if we ever decide to do anything with the error
                    if(LOG.isErrorEnabled())
                        LOG.error("Error saving json string to disk.");
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
        } else {
        // if not PKey exists, then the mcode doesn't exist either. so, we can show the nag now.
            mcodeListeners.broadcast(new MCodeEvent(""));
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

    @Override
    public void addMCodeListener(EventListener<MCodeEvent> listener) {
        mcodeListeners.addListener(listener);
    }

    @Override
    public boolean removeMCodeListener(EventListener<MCodeEvent> listener) {
        return mcodeListeners.removeListener(listener);
    }

    private class ActivationTask implements Runnable {
        
        private int consecutiveFailedRetries = 0;
        private final int maxFailedConsecutiveRetries;
        private final String key;

        ActivationTask(String keyParam, int maxFailedConsecutiveRetriesParam) {
            maxFailedConsecutiveRetries = maxFailedConsecutiveRetriesParam;
            key = keyParam;
        }

        @Override
        public void run() {
            ActivationResponse response;
            Throwable error = null;
            try {
                response = activationCommunicator.activate(key);
                consecutiveFailedRetries = 0;
            } catch (Throwable e) {
                error = e;
                response = activationResponseFactory.createErrorResponse(Type.ERROR);
            }
            State state = getNextState(response.getResponseType());
            attemptedToContactActivationServer = true;
            transitionToState(state, response);

            mcodeListeners.broadcast(new MCodeEvent(getMCode()));

            if (state == State.ACTIVATED_FROM_SERVER) {
                // reschedule next ping of activation server if necessary
                long refreshVal = response.getRefreshInterval();
                if (refreshVal > 0) {
                    activationContactor.rescheduleIfSooner(refreshVal*1000);
                }        
            } else if (error != null) {
                retryOnErrorIfNecessary(error);
            }
        }
        
        private State getNextState(Type type) {
            return (type == ActivationResponse.Type.VALID) ? State.ACTIVATED_FROM_SERVER : State.NOT_ACTIVATED;
        }
        
        private void retryOnErrorIfNecessary(Throwable error) {
            if ((error instanceof IOException) && (++consecutiveFailedRetries <= maxFailedConsecutiveRetries)) {
                int nextDelayInMs = consecutiveFailedRetries * 5000;
                activationContactor.rescheduleIfSooner(nextDelayInMs);    
            }
        }
    }

    private void transitionToState(State newState) {
        assert newState != State.ACTIVATED_FROM_DISK && newState != State.ACTIVATED_FROM_SERVER && 
                newState != State.NOT_ACTIVATED;
        transitionToState(newState, null);
    }
    
    private void transitionToState(State newState, ActivationResponse response) {
        LOG.debugf("transitioning to state:", newState);
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
        // if the user is clearing the key from the dialog, then we didn't go to the server and the response is null.
        if (response == null) {
            error = ActivationError.NO_ERROR;
            removeMcode = true;
            removeKey = true;
        } else {
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
        }
        if(!(currentState.getActivationState() == ActivationState.AUTHORIZED 
                && error == ActivationError.COMMUNICATION_ERROR)) {
            if(error == ActivationError.NO_ERROR || error == ActivationError.INVALID_KEY || error == ActivationError.BLOCKED_KEY)
                removeData(removeMcode, removeKey);
            activationError = error;
            setCurrentState(State.NOT_ACTIVATED);
            activationSettings.setLastStartPro(false);
        }
    }
        
    private void removeData(boolean removeMCode, boolean removeKey) {
        if(removeKey)
            activationSettings.setActivationKey("");
        writeToDisk("");
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
        writeToDisk(response.getJSONString());
        activationSettings.setActivationKey(response.getLid());
        activationSettings.setMCode(response.getMCode());
        setActivationItems(response.getActivationItems());
        activationSettings.setLastStartPro(isProActive());
        activationError = ActivationError.NO_ERROR;
        setCurrentState(State.ACTIVATED_FROM_SERVER);
    }

}
