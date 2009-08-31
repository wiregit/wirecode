package org.limewire.friend.impl;

import java.util.HashMap;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.inject.LazySingleton;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;

import com.google.inject.Inject;

/**
 * Friend feature uasge inspections.
 */
@LazySingleton
public class FriendInspections {
    
    
    // used for inspecting stats of most recent chat connection
    private EventBean<FriendConnectionEvent> connectionEventBean;


    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        
        // keeping previous inspection point name for backward compatibility
        @InspectionPoint(value = "xmpp service", category = DataCategory.USAGE)
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                FriendConnection conn = EventUtils.getSource(connectionEventBean);
                boolean isLoggedIn = (conn != null) && conn.isLoggedIn();
                Map<Object, Object> map = new HashMap<Object, Object>();
                map.put("loggedIn", isLoggedIn);
                if(isLoggedIn) {
                    map.put("logged in service", conn.getConfiguration().getNetworkName());
                }
                collectFriendStatistics(map, conn);    
                return map;
            }
            
            private void collectFriendStatistics(Map<Object, Object> data, FriendConnection connection) {
                int count = 0;
                InspectionHistogram<Integer> presencesHistogram = new InspectionHistogram<Integer>();
                int onlineFriends = 0;
                if (connection != null) {
                    for (Friend user : connection.getFriends()) {
                        if(user.isSignedIn()) {
                            onlineFriends++;
                        }
                        Map<String, FriendPresence> presences = user.getPresences();
                        presencesHistogram.count(presences.size());
                        for (FriendPresence presence : presences.values()) {
                            if (presence.hasFeatures(LimewireFeature.ID)) {
                                count++;
                                // break from inner presence loop, count each user only once
                                break;
                            }
                        }
                    }
                }
                data.put("limewire friends", count);
                data.put("online friends", onlineFriends);
                data.put("total friends", connection.getFriends().size());
                data.put("presences", presencesHistogram.inspect());
            }
        };
    }
    
    @Inject
    public FriendInspections(EventBean<FriendConnectionEvent> connectionEventBean) {
        this.connectionEventBean = connectionEventBean;  
    }
}