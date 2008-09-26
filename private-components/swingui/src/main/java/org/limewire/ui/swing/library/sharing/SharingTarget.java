package org.limewire.ui.swing.library.sharing;

public class SharingTarget {
    
    private final String id;

    public SharingTarget(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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

        return getId().equals(((SharingTarget)obj).getId());
    }

   

}
