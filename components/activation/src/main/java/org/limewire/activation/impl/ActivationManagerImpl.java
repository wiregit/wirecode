package org.limewire.activation.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.collection.Periodic;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.InvalidDataException;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.ActivationSettings;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * 
 */
@EagerSingleton
public class ActivationManagerImpl implements ActivationManager, Service {
    
    
//    /**
//     * States the ActivationManager can cycle through. 
//     * 
//     * The state machine behaves this way:
//     * 
//     *       UNINITIALIZED
//     *            @------------|
//     *                         V  ACTIVATING
//     *    |--------------------@---------------->@  ACTIVATED
//     *    | ActivationError    ^
//     *    V                    |
//     *    @--------------------|
//     *   NOT_ACTIVATED
//     *   
//     *   or
//     *   
//     *   PROVISIONALLY_ACTIVATED 
//     *   
//     */
//    private enum ActivatorState {
//        /**
//         * State when program starts. If a key already exists it has not yet
//         * attempted to contact the server. Once an activation is attempted,
//         * this state can never be returned to unless the program is restarted.
//         */
//        UNINITIALIZED,
//        
//        /**
//         * Same state as UNINITIALIZED except the server has already been contacted,
//         * with the key information and has failed. Unless a new key is entered or the
//         * user takes some action, the state will remain here.
//         */
//        NOT_ACTIVATED,
//        
//        /**
//         * Same state as ACTIVATED except the server could not be contacted.
//         * So, the last saved state was read from disk and we continue to 
//         * try to contact the server to verify the client's activation.
//         */
//        PROVISIONALLY_ACTIVATED,
//
//        /**
//         * ActivationManager is attempting to validate the key. If the key is valid,
//         * this will transition to a ACTIVATED state or a NOT_ACTIVATED state.
//         */
//        ACTIVATING,
//
//        /**
//         * ActivationManager has successfully authenticated the key for this session.
//         */
//        ACTIVATED;   
//    }
    
    private static final Log LOG = LogFactory.getLog(ActivationManagerImpl.class);

    private static final String validChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    
    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();

    private volatile ActivationError activationError = ActivationError.NO_ERROR;

    private final ActivationModel activationModel;
    private final ScheduledExecutorService scheduler;
    private final ActivationCommunicator activationCommunicator;
    private final ActivationSerializer activationSerializer;
    private final ActivationResponseFactory activationResponseFactory;
    private Periodic activationContactor = null;
    
    // interval in seconds between successive hits to the activation server
    private long refreshIntervalSeconds = 0;

    private final ActivationStateImpl UNINITIALIZED = new ActivationStateImpl(ActivationState.NOT_AUTHORIZED);
    private final NotActivated NOT_ACTIVATED = new NotActivated(ActivationState.NOT_AUTHORIZED);
    private final Activating ACTIVATING = new Activating(ActivationState.AUTHORIZING);
    private final ActivatedFromServer ACTIVATED_FROM_SERVER = new ActivatedFromServer(ActivationState.AUTHORIZED);
    private final ActivatedFromCache ACTIVATED_FROM_CACHE = new ActivatedFromCache(ActivationState.AUTHORIZED);
    
    private volatile ActivationStateImpl currentState = UNINITIALIZED;
    
    @Inject
    public ActivationManagerImpl(@Named("fastExecutor") ScheduledExecutorService scheduler,
                                 ActivationCommunicator activationCommunicator,
                                 ActivationModel activationModel, ActivationSerializer activationSerializer,
                                 ActivationResponseFactory activationReponseFactory) {
        this.activationModel = activationModel;
        this.scheduler = scheduler;
        this.activationCommunicator = activationCommunicator;
        this.activationSerializer = activationSerializer;
        this.activationResponseFactory = activationReponseFactory;
    }
    
    @Override
    public void activateKey(final String key) {
        activateKey(key, 0);
    }

    public void activateKey(final String key, final int numberOfRetries) {

        if (!activateKeyInitAndValidate(key)) {
            return;    
        }
        ACTIVATING.enterState();
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
            if(currentState == ACTIVATING)
                return false;
            else
                activationContactor.unschedule();
        }

        if (!isValidKey(key)) {
            NOT_ACTIVATED.enterState(ActivationError.INVALID_KEY);
            return false;
        }
        refreshIntervalSeconds = 0;
        return true;
    }

    @Override
    public ActivationState getActivationState() {
        return currentState.getActivationState();
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
        return ActivationSettings.ACTIVATION_KEY.getValueAsString();
    }
    
    @Override
    public String getMCode() {
        return ActivationSettings.M_CODE.getValueAsString();
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
        BackgroundExecutorService.execute(new Runnable(){
            public void run() {
                try {
                    String jsonString = activationSerializer.readFromDisk();
                    if(jsonString != null && jsonString.length() > 0) {
                        ActivationResponse response = activationResponseFactory.createFromDiskJson(jsonString);
                        ACTIVATED_FROM_CACHE.enterState(response);
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
            activateKey(storedLicenseKey, 5);
        }
    }

    @Override
    public void stop() {
        // todo: cancel all background stuff still running
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
    
    private void setActivating(ActivationStateImpl state) {
        activationError = ActivationError.NO_ERROR;
        currentState = state;
        listeners.broadcast(new ActivationEvent(getActivationState()));
    }
    
    private void setProvisionallyActivated(final ActivationResponse response, ActivationStateImpl state) {
        setActivationItems(response.getActivationItems());
        ActivationSettings.LAST_START_WAS_PRO.set(isProActive());
        activationError = ActivationError.NO_ERROR;
        currentState = state;
        LimeWireUtils.setIsPro(isProActive());
        listeners.broadcast(new ActivationEvent(getActivationState()));
    }
    
    private void setActivated(final ActivationResponse response, ActivationStateImpl state) {
        BackgroundExecutorService.execute(new Runnable(){
            public void run() {
                try {
                    activationSerializer.writeToDisk(response.getJSONString());                        
                } catch (Exception e) {
                    if(LOG.isErrorEnabled())
                        LOG.error("Error saving json string to disk.");
                }
            }
        });

        ActivationSettings.ACTIVATION_KEY.set(response.getLid());
        ActivationSettings.M_CODE.set(response.getMCode());
        
        setProvisionallyActivated(response, state);
    }
    
    private void setNotActivated(ActivationError error, ActivationStateImpl state) {
        if(error == ActivationError.INVALID_KEY) {
            ActivationSettings.ACTIVATION_KEY.revertToDefault();
        }
        BackgroundExecutorService.execute(new Runnable(){
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
        ActivationSettings.LAST_START_WAS_PRO.set(false);
        activationError = error;
        ActivationSettings.M_CODE.set("");
        currentState = state;
        LimeWireUtils.setIsPro(isProActive());
        
        listeners.broadcast(new ActivationEvent(getActivationState(), getActivationError()));
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
                            activationContactor.rescheduleIfSooner(nextDelay);
                            return;
                        }
                    }
                }
                NOT_ACTIVATED.enterState(ActivationError.COMMUNICATION_ERROR);
                return;
            }
            if (response != null) {
                processActivationResponse(response);
            } else {
                NOT_ACTIVATED.enterState(ActivationError.COMMUNICATION_ERROR);
            }
        }
        
        private void processActivationResponse(ActivationResponse response) {
            // reset counter
            numAttempted = 0;
            
            // todo: maybe use enums to delegate logic, instead of if/else
            if (response.getResponseType() == ActivationResponse.Type.VALID) {
                ACTIVATED_FROM_SERVER.enterState(response);
            } else if (response.getResponseType() == ActivationResponse.Type.NOTFOUND) {
                NOT_ACTIVATED.enterState(ActivationError.INVALID_KEY);
            } else if (response.getResponseType() == ActivationResponse.Type.BLOCKED) {
                NOT_ACTIVATED.enterState(ActivationError.BLOCKED_KEY);
            }
            
            // reschedule next ping of activation server if necessary
            long refreshVal = response.getRefreshInterval();
            if (refreshVal > 0) {
                refreshIntervalSeconds = refreshVal;
                activationContactor.rescheduleIfSooner(refreshIntervalSeconds*1000);
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
    
    private class ActivationStateImpl {
        private ActivationState state;
        
        private ActivationStateImpl(ActivationState state) {
            this.state = state;
        }
        
        public ActivationState getActivationState() {
            return this.state;
        }
    }
    
    private class NotActivated extends ActivationStateImpl  {
        private NotActivated(ActivationState state) {
            super(state);
        }

        public void enterState(ActivationError error) {
            setNotActivated(error, this);
        }
    }

    private class Activating extends ActivationStateImpl {
        private Activating(ActivationState state) {
            super(state);
        }

        public void enterState() {
            setActivating(this);
        }
    }

    private class ActivatedFromCache extends ActivationStateImpl {
        private ActivatedFromCache(ActivationState state) {
            super(state);
        }

        public void enterState(ActivationResponse response) {
            if (currentState == ACTIVATED_FROM_SERVER) {
                return;
            } else {
                setProvisionallyActivated(response, this);
            }
        }
    }

    private class ActivatedFromServer extends ActivationStateImpl {
        private ActivatedFromServer(ActivationState state) {
            super(state);
        }

        public void enterState(final ActivationResponse response) {
            setActivated(response, this);
        }
    }
}
