package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.InspectionResponse;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * Handles an incoming InspectionRequest, sending a response
 * if not empty and forwarding the request to leaves if it has a 
 * return address.
 */
public class InspectionRequestHandler extends RestrictedResponder {

    private static SecureMessageVerifier inspectionVerifier =
        new SecureMessageVerifier("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA" +
                "7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WW" +
                "J5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7" +
                "O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37" +
                "SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655" +
                "S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2" +
                "ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7" +
                "IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76" +
                "Z5ESUA4BQUAAFAMBACDJO4PTIV3332EWTALOMF5V3RO5BVEMHPVD4INLMQRIZ5" +
                "PW5RS7QJUGSINVNG4OTDO4FWJY5C3MQBQP7DXNOPQFJAVBCUE2VG3HWA34FPSLRIYBBGQVSQDQTQUS4" +
                "T6HW3OQNG2DPVGCIIWTCK6XMW3SK6PEQBWH6MIAL4FX3OYVWRG2ZKVBHBMJ564CKEPYDW3" +
                "TJRPIU4UA24I", null);
    
    private final Provider<MessageRouter> router;
    
    @Inject
    public InspectionRequestHandler(Provider<MessageRouter> router, NetworkManager networkManager, 
            SimppManager simppManager, 
            UDPReplyHandlerFactory udpReplyHandlerFactory, UDPReplyHandlerCache udpReplyHandlerCache) {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES, 
                inspectionVerifier,
                MessageSettings.INSPECTION_VERSION, networkManager, simppManager, udpReplyHandlerFactory, udpReplyHandlerCache);
        this.router = router;
    }
    
    @Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        assert msg instanceof InspectionRequest;
        InspectionRequest ir = (InspectionRequest)msg;
        InspectionResponse r = new InspectionResponse(ir);
        if (r.shouldBeSent())
            handler.reply(r);
        router.get().forwardInspectionRequestToLeaves(ir);
    }
}
