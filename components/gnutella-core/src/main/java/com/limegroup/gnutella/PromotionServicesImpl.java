package com.limegroup.gnutella;

import org.limewire.lifecycle.Service;
import org.limewire.promotion.InitializeException;
import org.limewire.promotion.PromotionBinderRepository;
import org.limewire.promotion.PromotionSearcher;
import org.limewire.promotion.PromotionServices;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.ThirdPartySearchResultsSettings;

@Singleton
final class PromotionServicesImpl implements PromotionServices, Service {

    private final PromotionBinderRepository promotionBinderRepository;

    private final PromotionSearcher promotionSearcher;
    
    private volatile boolean isRunning;

    @Inject
    public PromotionServicesImpl(PromotionBinderRepository promotionBinderRepository,
            PromotionSearcher promotionSearcher) {
        this.promotionBinderRepository = promotionBinderRepository;
        this.promotionSearcher = promotionSearcher;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Promotion System");
    }
    
    public void initialize() {
    }
    
    public void start() {
        try {
            promotionBinderRepository.init(
                        ThirdPartySearchResultsSettings.SEARCH_URL.getValue(),
                        ThirdPartySearchResultsSettings.BUCKET_ID_MODULUS.getValue()
                    );
            
            promotionSearcher.init(
                        ThirdPartySearchResultsSettings.MAX_NUMBER_OF_SEARCH_RESULTS.getValue()
                    );

            isRunning = true;
        } catch(InitializeException initializeException) {
            stop();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        promotionSearcher.shutDown();
        isRunning = false;
    }
}
