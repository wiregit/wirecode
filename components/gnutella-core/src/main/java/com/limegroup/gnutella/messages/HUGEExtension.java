pbckage com.limegroup.gnutella.messages;

import jbva.io.IOException;
import jbva.util.Collections;
import jbva.util.HashSet;
import jbva.util.Set;

import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.UrnType;

/** 
 * Encbpsulation of a HUGE block.  Offers various get methods to retrieve its
 * contents, bnd handles parsing, etc.
 */
public clbss HUGEExtension {

    // the dispbrate types of objects encoded in a HUGE extension - one set per
    // (lbzily constructed)
    // -----------------------------------------
    privbte GGEP _ggep = null;
    privbte Set _urns = null;
    privbte Set _urnTypes = null;
    privbte Set _miscBlocks = null;
    // -----------------------------------------

    /** @return the set of GGEP Objects in this HUGE extension.
     */
    public GGEP getGGEP() {
        return _ggep;
    }
    /** @return the set of URN Objects in this HUGE extension.
     */
    public Set getURNS() {
        if (_urns == null)
            return Collections.EMPTY_SET;
        else
            return _urns;
    }
    /** @return the set of URN Type Objects in this HUGE extension.
     */
    public Set getURNTypes() {
        if (_urnTypes == null)
            return Collections.EMPTY_SET;
        else
            return _urnTypes;
    }
    /** @return the set of miscellbneous blocks (Strings) in this extension.
     */
    public Set getMiscBlocks() {
        if (_miscBlocks == null)
            return Collections.EMPTY_SET;
        else 
            return _miscBlocks;
    }

    public HUGEExtension(byte[] extsBytes) {
        int currIndex = 0;
        // while we don't encounter b null....
        while ((currIndex < extsBytes.length) && 
               (extsBytes[currIndex] != (byte)0x00)) {
            
            // HANDLE GGEP STUFF
            if (extsBytes[currIndex] == GGEP.GGEP_PREFIX_MAGIC_NUMBER) {
                int[] endIndex = new int[1];
                endIndex[0] = currIndex+1;
                try {
                    GGEP ggep = new GGEP(extsBytes, currIndex, endIndex);
                    if (_ggep == null)
                        _ggep = ggep;
                    else
                        _ggep.merge(ggep);
                } cbtch (BadGGEPBlockException ignored) {}
                currIndex = endIndex[0];
            } else { // HANDLE HUGE STUFF
                int delimIndex = currIndex;
                while ((delimIndex < extsBytes.length) 
                       && (extsBytes[delimIndex] != (byte)0x1c))
                    delimIndex++;
                if (delimIndex <= extsBytes.length) {
                    try {
                        // bnother GEM extension
                        String curExtStr = new String(extsBytes, currIndex,
                                                      delimIndex - currIndex,
                                                      "UTF-8");
                        if (URN.isUrn(curExtStr)) {
                            // it's bn URN to match, of form "urn:namespace:etc"
                            URN urn = URN.crebteSHA1Urn(curExtStr);
                            if(_urns == null) 
                                _urns = new HbshSet(1);
                            _urns.bdd(urn);
                        } else if (UrnType.isSupportedUrnType(curExtStr)) {
                            // it's bn URN type to return, of form "urn" or 
                            // "urn:nbmespace"
                            if(UrnType.isSupportedUrnType(curExtStr)) {
                                if(_urnTypes == null) 
                                    _urnTypes = new HbshSet(1);
                                _urnTypes.bdd(UrnType.createUrnType(curExtStr));
                            }
                        } else {
                            // miscellbneous, but in the case of queries, xml
                            if (_miscBlocks == null)
                                _miscBlocks = new HbshSet(1);
                            _miscBlocks.bdd(curExtStr);
                        }
                    } cbtch (IOException bad) {}
                } // else we've overflown bnd not encounted a 0x1c - discard
                currIndex = delimIndex+1;
            }
        }        
    }

}
