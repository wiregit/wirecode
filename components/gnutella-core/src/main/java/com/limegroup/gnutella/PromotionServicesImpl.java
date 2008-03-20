package com.limegroup.gnutella;

import org.limewire.promotion.PromotionBinderRepository;
import org.limewire.promotion.PromotionBinderRequestor;
import org.limewire.promotion.PromotionSearcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.ThirdPartySearchResultsSettings;

@Singleton
final class PromotionServicesImpl implements PromotionServices {

    private final PromotionBinderRepository promotionBinderRepository;

    private final PromotionSearcher promotionSearcher;

    private final PromotionBinderRequestor promotionBinderRequestor;

    @Inject
    public PromotionServicesImpl(PromotionBinderRepository promotionBinderRepository,
            PromotionSearcher promotionSearcher, PromotionBinderRequestor promotionBinderRequestor) {
        this.promotionBinderRepository = promotionBinderRepository;
        this.promotionSearcher = promotionSearcher;
        this.promotionBinderRequestor = promotionBinderRequestor;
    }

    public void init() {
        promotionBinderRepository.init(
                    ThirdPartySearchResultsSettings.SEARCH_URL.getValue(),
                    ThirdPartySearchResultsSettings.BUCKET_ID_MODULOUS.getValue()
                );
        promotionSearcher.init(
                    ThirdPartySearchResultsSettings.MAX_NUMBER_OF_SEARCH_RESULTS.getValue()
                );
        int timeout = ThirdPartySearchResultsSettings.NETWORK_TIMEOUT_MILLIS_FOR_REQUESTING_BUCKETS.getValue();
        if (timeout >= 0) {
            promotionBinderRequestor.setNetworkTimeout(timeout);
        }
    }
}
