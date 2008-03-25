package com.limegroup.gnutella;

import org.limewire.promotion.PromotionBinderRepository;
import org.limewire.promotion.PromotionSearcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.ThirdPartySearchResultsSettings;

@Singleton
final class PromotionServicesImpl implements PromotionServices {

    private final PromotionBinderRepository promotionBinderRepository;

    private final PromotionSearcher promotionSearcher;

    @Inject
    public PromotionServicesImpl(PromotionBinderRepository promotionBinderRepository,
            PromotionSearcher promotionSearcher) {
        this.promotionBinderRepository = promotionBinderRepository;
        this.promotionSearcher = promotionSearcher;
    }

    public void init() {
        promotionBinderRepository.init(
                    ThirdPartySearchResultsSettings.SEARCH_URL.getValue(),
                    ThirdPartySearchResultsSettings.BUCKET_ID_MODULUS.getValue()
                );
        promotionSearcher.init(
                    ThirdPartySearchResultsSettings.MAX_NUMBER_OF_SEARCH_RESULTS.getValue()
                );
    }
}
