package org.limewire.util;

import java.io.Serializable;


@Deprecated
class MediaType implements Serializable {
    private static final long serialVersionUID = 3999062781289258389L;

    @SuppressWarnings("unused") // exists for serialization compatability.
    private static final MediaType TYPE_ANY = new MediaType() {
        // required SVUID because we're constructing an anonymous class.
        // the id is taken from old limewire builds, versions 4.4 to 4.12
        private static final long serialVersionUID = 8621997774686329539L; //3728385699213635375L;
    }; 
}