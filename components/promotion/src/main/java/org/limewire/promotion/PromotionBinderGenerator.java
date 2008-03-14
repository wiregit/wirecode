package org.limewire.promotion;

import java.beans.XMLEncoder;

import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Generates PromotionBinders.
 */
public class PromotionBinderGenerator {

    /**
     * @param args
     */
    public static void main(String[] args) {
        PromotionMessageContainer promo = new PromotionMessageContainer();
        promo.setDescription("I'm a description");

        XMLEncoder encoder = new XMLEncoder(System.out);
        encoder.writeObject(promo);

    }

}
