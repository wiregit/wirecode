package org.limewire.promotion;

public class SimplePromotionServicesImpl implements PromotionServices {

    private boolean isRunning = false;

    public void init() {
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void shutDown() {
        isRunning = false;
    }
}
