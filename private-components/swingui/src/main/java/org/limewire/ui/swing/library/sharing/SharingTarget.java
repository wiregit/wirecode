package org.limewire.ui.swing.library.sharing;

import org.limewire.core.api.friend.Friend;

public class SharingTarget {
    
    private final Friend friend;

    public SharingTarget(Friend friend){
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((friend == null) ? 0 : friend.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        
        if (!(obj instanceof SharingTarget)) {            
                return false;
        }

        return getFriend().equals(((SharingTarget)obj).getFriend());
    }

   

}
