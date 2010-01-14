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
import org.limewire.collection.Periodic;
import org.limewire.concurrent.FutureEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.InvalidDataException;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.ActivationSettings;

import com.google.inject.Inject;
import com.google.inject.name.Named;

//TODO: this needs to be a service, within the service start call 
// we attempt to authenticate the key if the key exists. otherwise
// it waits for user action.
@EagerSingleton
public class ActivationManagerImpl implements ActivationManager, Service {
    private static final Log LOG = LogFactory.getLog(ActivationManagerImpl.class);

    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();

    private volatile ActivationError activationError = ActivationError.NO_ERROR;

    private final ActivationModel activationModel;
    private final ScheduledExecutorService scheduler;
    private final ActivationCommunicator activationCommunicator;
    private Periodic activationContactor = null;
    
    // interval in seconds between successive hits to the activation server
    private long refreshIntervalSeconds = 0;
    
    @Inject
    public ActivationManagerImpl(@Named("fastExecutor") ScheduledExecutorService scheduler,
                                 ActivationCommunicator activationCommunicator,
                                 ActivationModel activationModel) {
        this.activationModel = activationModel;
        this.scheduler = scheduler;
        this.activationCommunicator = activationCommunicator;
    }
    
    @Override
    public void activateKey(final String key) {

        // cancel any existing activation currently taking place.
        if (activationContactor != null) {
            if(currentState == ACTIVATING)
                return;
            else
                activationContactor.unschedule();
        }

        if (!isValidKey(key)) {
            NOT_ACTIVATED.enterState(ActivationError.INVALID_KEY);
            return;
        }

        ACTIVATING.enterState();

        activationContactor = new Periodic(new Runnable() {
            
            private int tries = 5;
            
            // todo: improve the retry stuff so it's not hard coded
            // todo: ensure thread safety (error, activation, activation items consist of object state)
            @Override
            public void run() {
                try {
                    ActivationResponse response = activationCommunicator.activate(key);
                    ACTIVATED_FROM_SERVER.enterState(response.getLid(), response.getActivationItems());
                    
                    // todo: process the contents of the ActivationResponse, esp if there are errors
                    // todo: update refreshInterval and reschedule the next one.
                    // todo: test parsing of erroneous responses (INVALID_KEY), {"lid":"DAVV-XXME-BWU3","response":"notfound","refresh":0,"message":"ID not found"}
                    // todo: add unit tests for this (mock out ActivationCommunicator)
                } catch (IOException e) {
                    if (tries-- > 0) {
                        activationContactor.rescheduleIfSooner(25000 - tries*5000);    
                    } else {
                        NOT_ACTIVATED.enterState(ActivationError.COMMUNICATION_ERROR);
                    }
                } catch (InvalidDataException e) {
                    NOT_ACTIVATED.enterState(ActivationError.COMMUNICATION_ERROR);
                } catch (Throwable e) {
                    NOT_ACTIVATED.enterState(ActivationError.COMMUNICATION_ERROR);
                }
            }
        }, scheduler);
        
        activationContactor.rescheduleIfSooner(refreshIntervalSeconds);
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
        if (key.length() != 12)
            return false;
        
        String givenChecksum = key.substring(key.length()-1, key.length());
        String keyPart = key.substring(0, key.length()-1);

        String validChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        
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
        activationModel.load().addFutureListener(new EventListener<FutureEvent<Boolean>>(){
            @Override
            public void handleEvent(FutureEvent<Boolean> event) {
                if(event.getResult() == true) {
                    //TODO: check if PKey exists, check expiration dates, check current state
                    // before setting state and error message, server may have already set
                    // these
                    if(activationModel.size() > 0) {
                        //NOTE: this is commented out because this won't update the UI if everything has already expired
//                        List<ActivationItem> items = activationModel.getActivationItems();
//                        for(ActivationItem item : items) {
//                            //need to check expiration time against system time
//                            if(item.getStatus() == Status.ACTIVE) {
//                                activationError = ActivationError.NO_ERROR;
//                                ACTIVATED.enterState(ActivationError.NO_ERROR);
//                                break;
//                            }
//                        }
                    } else {
                        NOT_ACTIVATED.enterState(ActivationError.NO_ERROR);
                    }
                }
            }
        });
    }

    @Override
    public void start() {
        // if a PKey exists, start the server and try contacting the authentication server
//         String storedLicenseKey = "L4RXLP28XVQ5";    // test working key
        String storedLicenseKey = getLicenseKey();
        if (!storedLicenseKey.isEmpty()) {
            activateKey(storedLicenseKey);
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
    
    private class ActivationStateImpl {
    
        private ActivationState state;
        
        private ActivationStateImpl(ActivationState state) {
            this.state = state;
        }
        
        public ActivationState getActivationState() {
            return this.state;
        }
    }
    
    private final ActivationStateImpl UNINITIALIZED = new ActivationStateImpl(ActivationState.UNINITIALIZED);
    
    private class NotActivated extends ActivationStateImpl 
    {
        private NotActivated(ActivationState state) {
            super(state);
        }

        public void enterState(ActivationError error) {
            if(error == ActivationError.INVALID_KEY) {
                ActivationSettings.ACTIVATION_KEY.revertToDefault();
            }
            activationError = error;
            ActivationSettings.LAST_START_WAS_PRO.set(false);
            setActivationItems(Collections.EMPTY_LIST);
            listeners.broadcast(new ActivationEvent(getActivationState(), getActivationError()));
            currentState = this;
        }
    }

    private final NotActivated NOT_ACTIVATED = new NotActivated(ActivationState.NOT_ACTIVATED);
    
    private class Activating extends ActivationStateImpl 
    {
        private Activating(ActivationState state) {
            super(state);
        }

        public void enterState() {
            activationError = ActivationError.NO_ERROR;
            listeners.broadcast(new ActivationEvent(getActivationState()));
            currentState = this;
        }
    }

    private final Activating ACTIVATING = new Activating(ActivationState.ACTIVATING);

    private class ActivatedFromCache extends ActivationStateImpl 
    {
        private ActivatedFromCache(ActivationState state) {
            super(state);
        }

        public void enterState(String key, List<ActivationItem> items) {
            if (currentState == ACTIVATED_FROM_SERVER) {
                return;
            } else {
                ActivationSettings.ACTIVATION_KEY.set(key);
                ActivationSettings.LAST_START_WAS_PRO.set(true);
                setActivationItems(items);
                activationError = ActivationError.NO_ERROR;
                listeners.broadcast(new ActivationEvent(getActivationState()));
                currentState = this;
            }
        }
    }

    private final ActivatedFromCache ACTIVATED_FROM_CACHE = new ActivatedFromCache(ActivationState.ACTIVATED);

    private class ActivatedFromServer extends ActivationStateImpl 
    {
        private ActivatedFromServer(ActivationState state) {
            super(state);
        }

        public void enterState(String key, List<ActivationItem> items) {
            ActivationSettings.ACTIVATION_KEY.set(key);
            ActivationSettings.LAST_START_WAS_PRO.set(true);
            setActivationItems(items);
            activationError = ActivationError.NO_ERROR;
            listeners.broadcast(new ActivationEvent(getActivationState()));
            currentState = this;
        }
    }
    
    private final ActivatedFromServer ACTIVATED_FROM_SERVER = new ActivatedFromServer(ActivationState.ACTIVATED);

    private volatile ActivationStateImpl currentState = UNINITIALIZED;

}
