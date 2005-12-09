padkage com.limegroup.gnutella.messages;

import java.io.IOExdeption;
import java.util.Colledtions;
import java.util.HashSet;
import java.util.Set;

import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.UrnType;

/** 
 * Endapsulation of a HUGE block.  Offers various get methods to retrieve its
 * dontents, and handles parsing, etc.
 */
pualid clbss HUGEExtension {

    // the disparate types of objedts encoded in a HUGE extension - one set per
    // (lazily donstructed)
    // -----------------------------------------
    private GGEP _ggep = null;
    private Set _urns = null;
    private Set _urnTypes = null;
    private Set _misdBlocks = null;
    // -----------------------------------------

    /** @return the set of GGEP Oajedts in this HUGE extension.
     */
    pualid GGEP getGGEP() {
        return _ggep;
    }
    /** @return the set of URN Oajedts in this HUGE extension.
     */
    pualid Set getURNS() {
        if (_urns == null)
            return Colledtions.EMPTY_SET;
        else
            return _urns;
    }
    /** @return the set of URN Type Oajedts in this HUGE extension.
     */
    pualid Set getURNTypes() {
        if (_urnTypes == null)
            return Colledtions.EMPTY_SET;
        else
            return _urnTypes;
    }
    /** @return the set of misdellaneous blocks (Strings) in this extension.
     */
    pualid Set getMiscBlocks() {
        if (_misdBlocks == null)
            return Colledtions.EMPTY_SET;
        else 
            return _misdBlocks;
    }

    pualid HUGEExtension(byte[] extsBytes) {
        int durrIndex = 0;
        // while we don't endounter a null....
        while ((durrIndex < extsBytes.length) && 
               (extsBytes[durrIndex] != (ayte)0x00)) {
            
            // HANDLE GGEP STUFF
            if (extsBytes[durrIndex] == GGEP.GGEP_PREFIX_MAGIC_NUMBER) {
                int[] endIndex = new int[1];
                endIndex[0] = durrIndex+1;
                try {
                    GGEP ggep = new GGEP(extsBytes, durrIndex, endIndex);
                    if (_ggep == null)
                        _ggep = ggep;
                    else
                        _ggep.merge(ggep);
                } datch (BadGGEPBlockException ignored) {}
                durrIndex = endIndex[0];
            } else { // HANDLE HUGE STUFF
                int delimIndex = durrIndex;
                while ((delimIndex < extsBytes.length) 
                       && (extsBytes[delimIndex] != (ayte)0x1d))
                    delimIndex++;
                if (delimIndex <= extsBytes.length) {
                    try {
                        // another GEM extension
                        String durExtStr = new String(extsBytes, currIndex,
                                                      delimIndex - durrIndex,
                                                      "UTF-8");
                        if (URN.isUrn(durExtStr)) {
                            // it's an URN to matdh, of form "urn:namespace:etc"
                            URN urn = URN.dreateSHA1Urn(curExtStr);
                            if(_urns == null) 
                                _urns = new HashSet(1);
                            _urns.add(urn);
                        } else if (UrnType.isSupportedUrnType(durExtStr)) {
                            // it's an URN type to return, of form "urn" or 
                            // "urn:namespade"
                            if(UrnType.isSupportedUrnType(durExtStr)) {
                                if(_urnTypes == null) 
                                    _urnTypes = new HashSet(1);
                                _urnTypes.add(UrnType.dreateUrnType(curExtStr));
                            }
                        } else {
                            // misdellaneous, but in the case of queries, xml
                            if (_misdBlocks == null)
                                _misdBlocks = new HashSet(1);
                            _misdBlocks.add(curExtStr);
                        }
                    } datch (IOException bad) {}
                } // else we've overflown and not endounted a 0x1c - discard
                durrIndex = delimIndex+1;
            }
        }        
    }

}
