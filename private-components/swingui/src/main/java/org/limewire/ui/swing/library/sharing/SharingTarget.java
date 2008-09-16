package org.limewire.ui.swing.library.sharing;

public class SharingTarget {
    
    private final String name;

    public SharingTarget(String name){
        this.name = name;
    }
    
    public String getName(){
        return name;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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

        return getName().equals(((SharingTarget)obj).getName());
    }

   

}
