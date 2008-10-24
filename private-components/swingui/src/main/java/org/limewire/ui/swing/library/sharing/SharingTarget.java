package org.limewire.ui.swing.library.sharing;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.util.I18n;

public class SharingTarget {
    
    private final Friend friend;
    
    public final static SharingTarget GNUTELLA_SHARE = new SharingTarget(new Gnutella());

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

        return getFriend().getId().equals(((SharingTarget)obj).getFriend().getId());
    }
    
    public boolean isGnutellaNetwork(){
        return this == GNUTELLA_SHARE;
    }

    private static class Gnutella implements Friend {
        @Override
        public boolean isAnonymous() {
            return false;
        }
        
        @Override
        public String getId() {
            return "_@_internal_@_";
        }

        @Override
        public String getName() {
            return I18n.tr("LimeWire Network");
        }
        
        @Override
        public String getRenderName() {
            return getName();
        }

        public void setName(String name) {
            
        }
    }

}
