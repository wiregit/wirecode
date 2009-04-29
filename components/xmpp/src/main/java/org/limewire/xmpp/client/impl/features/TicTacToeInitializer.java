package org.limewire.xmpp.client.impl.features;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.TicTacToeFeature;
import org.limewire.core.api.friend.feature.features.TicTacToeSupport;

public class TicTacToeInitializer implements FeatureInitializer{
//        private final XMPPConnection connection;

//    public TicTacToeInitializer() {
//        //        public TicTacToeInitializer(XMPPConnection connection) {
////            this.connection = connection;
//        }

        @Override
        public void register(FeatureRegistry registry) {
            registry.add(TicTacToeFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new TicTacToeFeature(new TicTacToeSupportImpl()));
//            friendPresence.addFeature(new TicTacToeFeature(new TicTacToeSupportImpl(friendPresence.getPresenceId(), connection)));
        }
        

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(TicTacToeFeature.ID);
        }
        private static class TicTacToeSupportImpl implements TicTacToeSupport {

//            private String presenceID;
//            private final XMPPConnection connection;

            public TicTacToeSupportImpl() {
//            public TicTacToeSupportImpl(String presenceId, XMPPConnection connection) {
//                this.presenceID = presenceId;
//                this.connection = connection;
            }

        }
}
