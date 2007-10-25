package com.limegroup.gnutella;

import com.google.inject.Guice;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;

/**
 * A collection of Providers that are hacks during the interim change towards
 * dependency injection.
 */
// DPINJ: REMOVE THIS CLASS!!!
public class ProviderHacks {
    
    private static volatile boolean unusable = false;
    private static volatile boolean used = false;
    
    private static volatile boolean initialized = false;
    private static volatile boolean initializing = false;
    
    private static volatile Throwable initializedSource;
    
    private static LimeWireCore aReallyLongNameThatYouDontWantToTypeALot;
    
    public static void markUnusable() {
        if(used)
            throw new IllegalStateException("already used", initializedSource);
        unusable = true;
        initializedSource = new Exception();
    }
    
    private static LimeWireCore use() {
        if(unusable)
            throw new IllegalStateException("marked unusable", initializedSource);        
        used = true;
        
        if(initialized)
            return aReallyLongNameThatYouDontWantToTypeALot;        
        if(initializing)
            throw new IllegalStateException("already initializing!");
        initializing = true;
        aReallyLongNameThatYouDontWantToTypeALot = Guice.createInjector(new LimeWireCoreModule(ActivityCallbackAdapter.class), new LimeTestUtils.BlockingConnectionFactoryModule()).getInstance(LimeWireCore.class);
        initialized = true;
        initializedSource = new Exception();
        initializing = false;
        return aReallyLongNameThatYouDontWantToTypeALot;
    }
    
  
    public static MessageFactory getMessageFactory() { return use().getMessageFactory(); }
    public static PingReplyFactory getPingReplyFactory() { return use().getPingReplyFactory(); }
    public static QueryReplyFactory getQueryReplyFactory() { return use().getQueryReplyFactory(); } 
    public static QueryRequestFactory getQueryRequestFactory() { return use().getQueryRequestFactory(); }    
    public static PingRequestFactory getPingRequestFactory() { return use().getPingRequestFactory(); }
    
  
    
}
