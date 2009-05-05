package org.limewire.xmpp.client.impl.features.tictactoe;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.TicTacToeFeature;
import org.limewire.core.api.friend.feature.features.TicTacToeSupport;

/**
 * Let's the LimeWire client know you support playing Tic Tac Toe (ttt). In LimeWire you
 * challenge a friend to play ttt only if he supports also supports ttt.
 */
public class TicTacToeInitializer implements FeatureInitializer{

        @Override
        public void register(FeatureRegistry registry) {
            registry.add(TicTacToeFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new TicTacToeFeature(new TicTacToeSupportImpl()));
        }
        

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(TicTacToeFeature.ID);
        }
        private static class TicTacToeSupportImpl implements TicTacToeSupport {

            public TicTacToeSupportImpl() {
            }

        }
}
