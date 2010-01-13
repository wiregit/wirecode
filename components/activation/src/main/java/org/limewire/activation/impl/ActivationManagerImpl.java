package org.limewire.activation.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.concurrent.FutureEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.setting.ActivationSettings;

import com.google.inject.Inject;

//TODO: this needs to be a service, within the service start call 
// we attempt to authenticate the key if the key exists. otherwise
// it waits for user action.
@EagerSingleton
public class ActivationManagerImpl implements ActivationManager, Service {

    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();

    private volatile ActivationState currentState = ActivationState.UNINITIALIZED;
    private volatile ActivationError activationError = ActivationError.NO_ERROR;

    private final ActivationModel activationModel;
    private final ActivationItemFactory activationItemFactory;
    
    @Inject
    public ActivationManagerImpl(ActivationModel activationModel, ActivationItemFactory activationItemFactory) {
        this.activationModel = activationModel;
        this.activationItemFactory = activationItemFactory;
    }
    
    @Override
    public void activateKey(final String key) {
        /** Some keys that are valid
         *  ADXU-8ZND-JGU8: 198 => 6 => 8
            J8SV-KC4Y-FLBE: 172 => 12 => E
            4PVV-CDDA-8T8U: 154 => 26 => U
            Q9DP-UGFY-CRTC: 202 => 10 => C
            MQDH-SC24-C6NB: 137 => 9 => B
            QT5J-KANU-PJ7M: 179 => 19 => M
            RZCL-CZPX-PFVC: 234 => 10 => C
            CT3Y-NPVM-8MN8: 198 => 6 => 8
            DDHN-SUN9-GV4K: 177 => 17 => K
            ZATY-QEWF-4RQS: 216 => 24 => S
            6JZY-DN7V-PX79: 199 => 7 => 9
            4E2C-QF7S-4X4T: 121 => 25 => T
            K4W5-K3BL-WBZ5: 163 => 3 => 5
            NJ26-KPWW-HQAM: 179 => 19 => M
            GN2G-32U2-N827: 101 => 5 => 7
            S3H6-QCSN-UGQQ: 182 => 22 => Q
            J8Q4-UJNK-LFVR: 183 => 23 => R
            8M5W-YHKA-NKEH: 175 => 15 => H
            9GX5-F2B3-782R: 87 => 23 => R
            SFG6-P97K-9Q8E: 140 => 12 => E
         */

        if (!isValidKey(key)) {
            setActivationFailed(ActivationError.INVALID_KEY);
            return;
        }

        
        if (currentState == ActivationState.ACTIVATING) {
            return;
        }
        
        //TODO: this sould hit the real server
        Thread t = new Thread(new Runnable(){
            public void run() {
                startActivating();

                try {
                    Thread.sleep(2000);
                } catch(InterruptedException e) {
                    
                }
                if(key.equals("ADXU8ZNDJGU8")) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

                    try {
                        List<ActivationItem> list = new ArrayList<ActivationItem>();
                        list.add(activationItemFactory.createActivationItem(1, "Test Active", new Date(1), formatter.parse("20100218"), Status.ACTIVE));
                        list.add(activationItemFactory.createActivationItem(2, "Test Removed", new Date(1), formatter.parse("20100218"), Status.UNAVAILABLE));
                        list.add(activationItemFactory.createActivationItem(3, "Test Expired", new Date(1), formatter.parse("20090218"), Status.EXPIRED));
                        list.add(activationItemFactory.createActivationItem(4, "Test Wrong LW", new Date(1), formatter.parse("20100218"), Status.UNUSEABLE_LW));
                        list.add(activationItemFactory.createActivationItem(5, "Test Wrong OS", new Date(1), formatter.parse("20100218"), Status.UNUSEABLE_OS));
                        setActivationItems(list);
                    } catch(ParseException e) {
                        
                    }
                    
                    setActivated(key);
                } else if(key.equals("J8SVKC4YFLBE")) {                        
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

                    try {
                        List<ActivationItem> list = new ArrayList<ActivationItem>();
                        list.add(activationItemFactory.createActivationItem(1, "Turbo Charged Downloads", new Date(1), formatter.parse("20200218"), Status.ACTIVE));
                        list.add(activationItemFactory.createActivationItem(2, "Optimized Search Results", new Date(1), formatter.parse("20200218"), Status.ACTIVE));
                        list.add(activationItemFactory.createActivationItem(3, "Tech Support", new Date(1), formatter.parse("20200218"), Status.ACTIVE));
                        list.add(activationItemFactory.createActivationItem(4, "AVG", new Date(1), formatter.parse("20200218"), Status.ACTIVE));
                        setActivationItems(list);
                    } catch(ParseException e) {
                    }
                    
                    setActivated(key);
             } else if (key.equals("4PVVCDDA8T8U")) {                   
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

                    try {
                        // this is temporary
                        List<ActivationItem> list = new ArrayList<ActivationItem>();
                        list.add(activationItemFactory.createActivationItem(1, "LimeWire PRO Extended", new Date(), formatter.parse("20100218"), Status.ACTIVE));
                        setActivationItems(list);
                    } catch (ParseException e) {
                    }
                    
                    setActivated(key);
                } else if (key.equals("54321")) {
                    setActivationFailed(ActivationError.BLOCKED_KEY);
                } else {
                    setActivationFailed(ActivationError.INVALID_KEY);
                }
            }
        });
        t.start();
    }
    
    private void startActivating() {
        currentState = ActivationState.ACTIVATING;
        activationError = ActivationError.NO_ERROR;
        listeners.broadcast(new ActivationEvent(ActivationState.ACTIVATING));
    }
    
    private void setActivated(String key) {
        ActivationSettings.ACTIVATION_KEY.set(key);
        currentState = ActivationState.ACTIVATED;
        activationError = ActivationError.NO_ERROR;
        listeners.broadcast(new ActivationEvent(ActivationState.ACTIVATED));
    }
    
    private void setActivationFailed(ActivationError error) {
        if(error == ActivationError.INVALID_KEY) {
            ActivationSettings.ACTIVATION_KEY.revertToDefault();
        }
        ActivationSettings.LAST_START_WAS_PRO.set(false);
        currentState = ActivationState.NOT_ACTIVATED;
        activationError = error;
        setActivationItems(Collections.EMPTY_LIST);
        listeners.broadcast(new ActivationEvent(ActivationState.NOT_ACTIVATED, error));
    }
    
//    private void updateState(ActivationState state, ActivationError error, List<ActivationItem> items) {
//        
//    }

    @Override
    public ActivationState getActivationState() {
        return currentState;
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
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
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
                                currentState = ActivationState.ACTIVATED;
                                activationError = ActivationError.NO_ERROR;
//                                break;
//                            }
//                        }
                    } else {
                        currentState = ActivationState.NOT_ACTIVATED;
                        activationError = ActivationError.NO_ERROR;
                    }
                }
            }
        });
    }

    @Override
    public void start() {
        //TODO: if a PKey exists, start the server and try contacting the authentication server
    }

    @Override
    public void stop() {
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
}
