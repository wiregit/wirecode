package com.limegroup.gnutella.messages;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.DataUtils;
import java.io.IOException;

/** 
 * Encapsulation of a HUGE block.  Offers various get methods to retrieve its
 * contents, and handles parsing, etc.
 */
public class HUGEExtension {

    // the disparate types of objects encoded in a HUGE extension - one set per
    // (lazily constructed)
    // -----------------------------------------
    private Set _ggeps = null;
    private Set _urns = null;
    private Set _urnTypes = null;
    private Set _xmlBlocks = null;
    // -----------------------------------------

    /** @return the set GGEP Objects in this HUGE extension, may return null.
     */
    public Set getGGEPBlocks() {
        if (_ggeps == null) return DataUtils.EMPTY_SET;
        return _ggeps;
    }
    /** @return the set URN Objects in this HUGE extension, may return null.
     */
    public Set getURNS() {
        if (_urns == null) return DataUtils.EMPTY_SET;
        return _urns;
    }
    /** @return the set URN Type Objects in this HUGE extension, may return
     *  null.
     */
    public Set getURNTypes() {
        if (_urnTypes == null) return DataUtils.EMPTY_SET;
        return _urnTypes;
    }
    /** @return the set XML blocks (Strings) in this HUGE extension, may return
     *  null.
     */
    public Set getXMLBlocks() {
        if (_xmlBlocks == null) return DataUtils.EMPTY_SET;
        return _xmlBlocks;
    }

    public HUGEExtension(byte[] extsBytes) {
        int currIndex = 0;
        // while we don't encounter a null....
        while ((currIndex < extsBytes.length) && 
               (extsBytes[currIndex] != (byte)0x00)) {
            
            // HANDLE GGEP STUFF
            if (extsBytes[currIndex] == GGEP.GGEP_PREFIX_MAGIC_NUMBER) {
                int[] endIndex = new int[1];
                endIndex[0] = currIndex+1;
                try {
                    GGEP ggep = new GGEP(extsBytes, currIndex, endIndex);
                    if (_ggeps == null)
                        _ggeps = new HashSet();
                    _ggeps.add(ggep);
                }
                catch (BadGGEPBlockException ignored) {}
                    
                currIndex = endIndex[0];
            }
            else { // HANDLE HUGE STUFF
                int delimIndex = currIndex;
                while ((delimIndex < extsBytes.length) 
                       && (extsBytes[delimIndex] != (byte)0x1c))
                    delimIndex++;
                if (delimIndex > extsBytes.length) 
                    ; // we've overflown and not encounted a 0x1c - discard
                else {
                    try {
                        // another GEM extension
                        String curExtStr = new String(extsBytes, currIndex,
                                                      delimIndex - currIndex,
                                                      "UTF-8");
                        if (URN.isUrn(curExtStr)) {
                            // it's an URN to match, of form "urn:namespace:etc"
                            URN urn = URN.createSHA1Urn(curExtStr);
                            if(_urns == null) 
                                _urns = new HashSet();
                            _urns.add(urn);
                        } 
                        else if (UrnType.isSupportedUrnType(curExtStr)) {
                            // it's an URN type to return, of form "urn" or 
                            // "urn:namespace"
                            if(UrnType.isSupportedUrnType(curExtStr)) {
                                if(_urnTypes == null) 
                                    _urnTypes = new HashSet();
                                _urnTypes.add(UrnType.createUrnType(curExtStr));
                            }
                        } 
                        else {
                            // miscellaneous, but in the case of queries, xml
                            if (_xmlBlocks == null)
                                _xmlBlocks = new HashSet();
                            _xmlBlocks.add(curExtStr);
                        }
                    }
                    catch (IOException bad) {}
                }
                currIndex = delimIndex+1;
            }
        }        
    }

}
