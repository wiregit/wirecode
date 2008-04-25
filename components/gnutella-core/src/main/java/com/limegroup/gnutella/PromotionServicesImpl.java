package com.limegroup.gnutella;

import org.limewire.promotion.InitializeException;
import org.limewire.promotion.PromotionBinderRepository;
import org.limewire.promotion.PromotionSearcher;
import org.limewire.promotion.PromotionServices;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.ThirdPartySearchResultsSettings;

@Singleton
final class PromotionServicesImpl implements PromotionServices {

    private final PromotionBinderRepository promotionBinderRepository;

    private final PromotionSearcher promotionSearcher;
    
    private volatile boolean isRunning;

    @Inject
    public PromotionServicesImpl(PromotionBinderRepository promotionBinderRepository,
            PromotionSearcher promotionSearcher) {
        this.promotionBinderRepository = promotionBinderRepository;
        this.promotionSearcher = promotionSearcher;
    }

    public void init() {
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
            shutDown();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void shutDown() {
        promotionSearcher.shutDown();
        isRunning = false;
    }
}
