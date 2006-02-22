
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnType;

/**
 * A HUGEExtension object represents the extended area between the two 0 bytes in a query packet or query hit result.
 * The constructor takes the bytes of the extended area, parses them into objects, and keeps them in member variables and lists.
 * 
 * A Gnutella query packet has this structure:
 * 
 *   header 23
 *   minimum speed 1
 *   search\0
 *   extended area\0
 * 
 * There are two 0 bytes after the search text, and between them is the extended area.
 * A Gnutella query hit packet has this structure:
 * 
 *   header 23
 *   payload 11
 *   results
 *   id 16
 * 
 * There can be any number of results, each of which has this structure this structure:
 * 
 *   index 4
 *   length 4
 *   file name\0
 *   extended area\0
 * 
 * There are two 0 bytes after the search text, and between them is the extended area.
 * The extended area looks like this:
 * 
 *   extension[0x1C]extension[0x1C]extension
 * 
 * There can be any number of extensions, and they are separated by 0x1C bytes.
 * There are no 0 bytes anywhere in the extended area.
 * There are 3 kinds of extensions:
 * -A GGEP block that begins with the byte 0xC3.
 * -A HUGE URN which is text like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", or just the header, like "urn:sha1:".
 * -XML, which starts "<" or "{".
 * 
 * Call getGGEP() to get a GGEP object with the GGEP extensions of all the GGEP blocks we found.
 * Call getURNS() to get a Set of URN objects with the URNs that have hashes.
 * Call getURNTypes() to get a Set of UrnType objects with the URNs that are just prefixes indicating the kinds of hashes the sender wants.
 * Call getMiscBlocks() to get a Set of String objects with the other text extensions, which are probably XML.
 * 
 * The QueryRequest constructor makes a HUGEExtension object to represent the extended area of a QueryRequest packet.
 * Response.createFromStream() makes a HUGEExtension object to represent the extended area of a result in a QueryResponse packet.
 */
public class HUGEExtension {

    /**
     * The GGEP block.
     * If more than one extension is a GGEP block, we'll add all the extensions to this single GGEP object.
     */
    private GGEP _ggep = null;

    /**
     * A list of HUGE URN Strings, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * This HashSet has URN objects in it, not strings.
     */
    private Set _urns = null; // Of URN objects

    /**
     * A list of HUGE URN types, like "urn:sha1:", "urn:", or "urn:bitprint:".
     * This HashSet has UrnType objects in it, not strings.
     */
    private Set _urnTypes = null; // Of UrnType objects

    /**
     * Other lines of text in the extended area separated by 0x1C bytes.
     * Each one is probably XML.
     */
    private Set _miscBlocks = null;

    /*
     * -----------------------------------------
     */

    /**
     * The GGEP block.
     * If this HUGE extended area had more than one GGEP block, we added the extensions from all of them to this GGEP object.
     * 
     * @return A GGEP object with all the GGEP extensions from the GGEP block extensions in the HUGE extended area
     */
    public GGEP getGGEP() {

        // Return the reference we saved when we parsed an extension into a GGEP object
        return _ggep;
    }

    /**
     * The HUGE URN extensions, like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", that we found in this HUGE extended area.
     * This is how a query hit sends the SHA1 hash of a file.
     * 
     * @return A HashSet of URN objects with the URNs.
     *         If this HUGE extended area doesn't have any URNs, returns an empty list instead of null.
     */
    public Set getURNS() {

        // Return the Set of URN objects, or an empty one if we didn't find any URNs in this extended area.
        if (_urns == null) return Collections.EMPTY_SET;
        else               return _urns;
    }

    /**
     * The HUGE URN extension types, like "urn:sha1:", "urn:", and "urn:bitprint:", that we found in this HUGE extended area.
     * This is how a query indicates what kinds of hashes it wants in query hits.
     * 
     * @return A Set of UrnType objects that represent the text prefixes we found.
     *         If this HUGE extended area doesn't have any URNs, returns an empty list instead of null.
     */
    public Set getURNTypes() {

        // Return the Set of UrnType objects, or an empty one if we didn't find text extensions like "urn:sha1:" or "urn:"
        if (_urnTypes == null) return Collections.EMPTY_SET;
        else                   return _urnTypes;
    }

    /**
     * The text extensions that aren't URNs, and are probably XML.
     * 
     * @return A set of String objects with the text extensions we parsed that aren't URNs.
     *         If this HUGE extended area only had GGEP blocks and URNs, returns an empty list instead of null.
     */
    public Set getMiscBlocks() {

        // Return a Set of String objects, or an empty one if we didn't find text extensions that weren't URNs
        if (_miscBlocks == null) return Collections.EMPTY_SET;
        else                     return _miscBlocks;
    }

    /**
     * Make a new HUGEExtension object by parsing the bytes of an extended area from a Gnutella query packet or query hit result.
     * 
     * extsBytes is the data of an extended area in a Gnutella query packet or query hit result.
     * It came from between the two 0 bytes after the search text or file name.
     * It doesn't contain any 0 bytes.
     * The extensions in it are separated by 0x1C bytes.
     * 
     * There are 3 kinds of extensions:
     * A GGEP block that begins with the byte 0xC3.
     * A HUGE URN which is text like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ", or just the header, like "urn:sha1:".
     * XML, which starts "<" or "{".
     * 
     * This constructor parses through the bytes, identifying each extension.
     * It turns the bytes of a GGEP block into a GGEP object, and saves it under _ggep.
     * If it finds a second GGEP block, it adds its extensions into that one.
     * The constructor sorts each text extension into 1 of 3 lists:
     * 
     * _urns is a list of URN objects that represent URNs with hashes like "urn:sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ".
     * _urnTypes is a list of UrnType objects that represent URN prefixes like "urn:sha1:", "urn:", or "urn:bitprint:".
     * _miscBlocks is a list of String objects that hold the other text extensions, which are probably XML.
     * 
     * The QueryRequest constructor calls this to parse the extended area of a QueryRequest packet.
     * Response.createFromStream() calls this to parse the extended area of a result in a QueryResponse packet.
     * 
     * @param extsBytes A byte array with the data of an extended area in a Gnutella packet
     */
    public HUGEExtension(byte[] extsBytes) {

        // We'll move currIndex down the length of extsBytes as we parse the individual extensions from it
        int currIndex = 0;

        // Loop until we're through the data
        while ((currIndex < extsBytes.length) && (extsBytes[currIndex] != (byte)0x00)) { // Or we hit a 0 byte, which should never happen

            // The extension at currIndex is a GGEP block
            if (extsBytes[currIndex] == GGEP.GGEP_PREFIX_MAGIC_NUMBER) { // GGEP block start with their identifying byte 0xC3

                // Prepare a byte array that the GGEP constructor can write the block length in to
                int[] endIndex = new int[1];
                endIndex[0] = currIndex + 1; // The GGEP constructor doesn't read the value endIndex[0], not sure why we're setting it (do)

                try {

                    // Turn the data of this extension into a GGEP object
                    GGEP ggep = new GGEP(extsBytes, currIndex, endIndex); // Writes the index beyond the GGEP block in endIndex[0]

                    // Save the GGEP object in this HUGEExtension one
                    if (_ggep == null) _ggep = ggep; // If this HUGEExtension object doesn't have a GGEP block yet, this is it
                    else _ggep.merge(ggep);          // We already have one, add this one's extensions into it

                // There was a problem parsing the data into a GGEP object, move on to the next extension
                } catch (BadGGEPBlockException ignored) {}

                // Move currIndex beyond the end of the GGEP block
                currIndex = endIndex[0]; // Now, it should either be at the end of the data, or on a 0x1C separator

                /*
                 * TODO:kfaaborg If there is a GGEP block before a text extension, currIndex will get stuck in front of the 0x1C separator.
                 */

            // The extension at currIndex must be a HUGE URN or XML, or we're on a 0x1C byte after just finding a GGEP block
            } else {

                // Start delimIndex at currIndex
                int delimIndex = currIndex;

                // Move delimIndex forward until it's on the end or on a separating 0x1C byte
                while ((delimIndex < extsBytes.length) && (extsBytes[delimIndex] != (byte)0x1c)) delimIndex++;

                // If delimIndex is before or on the end
                if (delimIndex <= extsBytes.length) {

                    try {

                        // Clip the text between currIndex and delimIndex into a String, this is the text of the extension
                        String curExtStr = new String(extsBytes, currIndex, delimIndex - currIndex, "UTF-8"); // UTF-8 is ASCII

                        // This text extension starts "urn:sha1:", "urn:", or "urn:bitprint:", and has 32 or 72 characters after that
                        if (URN.isUrn(curExtStr)) {

                            // Make a new URN object to hold and represent the text URN
                            URN urn = URN.createSHA1Urn(curExtStr);

                            // If we haven't made the _urns HashSet yet, do it
                            if (_urns == null) _urns = new HashSet(1); // Tell it we'll start by putting 1 object in it

                            // Add it to the list
                            _urns.add(urn);

                        // curExtStr is just "urn:sha1:", "urn:", or "urn:bitprint:", with nothing after it
                        } else if (UrnType.isSupportedUrnType(curExtStr)) {

                            // If this URN prefix is one we recognize and support
                            if (UrnType.isSupportedUrnType(curExtStr)) {

                                // IF we haven't made the _urnTypes HashSet yet, do it
                                if (_urnTypes == null) _urnTypes = new HashSet(1); // Tell it we'll start by putting 1 object in it

                                // Make a new UrnType object to represent the type of URN
                                _urnTypes.add(UrnType.createUrnType(curExtStr));
                            }

                        // This text extension doesn't look like a HUGE URN, it's probably XML
                        } else {

                            // Add the text to our list of miscellaneous extension blocks
                            if (_miscBlocks == null) _miscBlocks = new HashSet(1); // Make the _miscBlocks list if we haven't already
                            _miscBlocks.add(curExtStr); // Add the String to it
                        }

                    } catch (IOException bad) {}
                    
                } // else we've overflown and not encounted a 0x1c - discard

                // Move currIndex beyond the 0x1C separating byte delimIndex points to
                currIndex = delimIndex + 1;
            }
        }
    }
}
