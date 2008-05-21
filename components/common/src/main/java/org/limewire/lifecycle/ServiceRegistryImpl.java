package org.limewire.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;

@Singleton
class ServiceRegistryImpl implements ServiceRegistry {
    
    private final List<StagedRegisterBuilderImpl> builders
        = new ArrayList<StagedRegisterBuilderImpl>();

    private final Map<Object, List<ServiceHolder>> services
        = new HashMap<Object, List<ServiceHolder>>();
    
    private final List<ServiceHolder> startedServices
        = new ArrayList<ServiceHolder>();
        
    public void initialize() {
        // Remove builders & assign services.
        for(Iterator<StagedRegisterBuilderImpl> iter = builders.iterator(); iter.hasNext(); ) {
            StagedRegisterBuilderImpl builder = iter.next();
            Object stage = builder.getCustomStage();
            if(stage == null)
                stage = builder.getStage();
            List<ServiceHolder> servicesInStage = services.get(stage);
            if(servicesInStage == null) {
                servicesInStage = new ArrayList<ServiceHolder>();
                services.put(stage, servicesInStage);
            }
            servicesInStage.add(new ServiceHolder(builder.getService()));
            iter.remove();
        }
        
        // Do the actual initialization.
        
        // First go through built-in stages.
        for(ServiceStage stage : getStagesInOrder()) {
            if(services.get(stage) != null) {
                for(ServiceHolder service : services.get(stage)) {
                    service.init();
                }
            }
        }
        // Then go through custom stages.
        for(Map.Entry<Object, List<ServiceHolder>> entry : services.entrySet()) {
            if(entry.getKey().getClass() != ServiceStage.class) {
                if(entry.getValue() != null) {
                    for(ServiceHolder service : entry.getValue()) {
                        service.init();
                    }
                }
            }
        }
    }
    
    public void start(Object stage) {
        initialize();        
        startStage(stage);
    }
    
    public void start() {
        initialize();        
        for(ServiceStage stage : getStagesInOrder()) {
            startStage(stage);            
        }
    }
    
    private void startStage(Object stage) {
        List<ServiceHolder> servicedStages = services.get(stage);
        if(servicedStages != null) {
            for(Iterator<ServiceHolder> iter = servicedStages.iterator(); iter.hasNext(); ) {
                ServiceHolder service = iter.next();
                service.start();
                startedServices.add(service);
                iter.remove();
            }
        }
    }
    
    public void stop() {
        for(int i = startedServices.size()-1; i >= 0; i--) {
            startedServices.get(i).stop();
            startedServices.remove(i);
        }
    }
    
    public StagedRegisterBuilder register(Service service) {
        StagedRegisterBuilderImpl builder = new StagedRegisterBuilderImpl(service);
        builders.add(builder);
        return builder;
    }

    ServiceStage[] getStagesInOrder() {
        return new ServiceStage[] { ServiceStage.EARLY, ServiceStage.NORMAL, ServiceStage.LATE };
    }
    
    private static class ServiceHolder {
        private final Service service;
        private boolean initted;
        private boolean started;
        private boolean stopped;
        
        public ServiceHolder(Service service) {
            this.service = service;
        }
        
        void init() {
            if(!initted) {
                initted = true;
                service.initialize();
            }
        }

        void start() {
            if(!started) {
                started = true;
                service.start();
            }
        }

        void stop() {
            if(!stopped) {
                stopped = true;
                service.stop();
            }
        }
    }
}
