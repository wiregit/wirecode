/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito2.message;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.io.NetworkUtils;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.security.SecurityTokenHelper;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;

/**
 * The default implementation of the MessageFactory.
 */
public class DefaultMessageFactory implements MessageFactory {

    protected final MACCalculatorRepositoryManager calculator;
    
    protected final SecurityTokenHelper tokenHelper;
    
    protected final MessageCodec codec;
    
    public DefaultMessageFactory() {
        this(new MACCalculatorRepositoryManager());
    }
    
    public DefaultMessageFactory(MACCalculatorRepositoryManager calculator) {
        this.calculator = calculator;
        
        SecurityToken.TokenProvider tokenProvider 
            = new SecurityToken.AddressSecurityTokenProvider(calculator);
        
        this.tokenHelper = new SecurityTokenHelper(tokenProvider);
        this.codec = new DefaultMessageCodec(calculator);
    }
    
    @Override
    public MACCalculatorRepositoryManager getMACCalculatorRepositoryManager() {
        return calculator;
    }
    
    @Override
    public SecurityTokenHelper getSecurityTokenHelper() {
        return tokenHelper;
    }
    
    @Override
    public Message deserialize(SocketAddress src, byte[] message, 
            int offset, int length) throws IOException {
        return codec.decode(src, message, offset, length);
    }
    
    @Override
    public byte[] serialize(SocketAddress dst, Message message) throws IOException {
        return codec.encode(dst, message);
    }
    
    @Override
    public SecurityToken createSecurityToken(Contact dst) {
        return tokenHelper.createSecurityToken(dst);
    }
    
    @Override
    public TokenData createTokenData(Contact src) {
        return tokenHelper.createTokenData(src);
    }
    
    @Override
    public MessageID createMessageID(SocketAddress dst) {
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            throw new IllegalArgumentException(dst + " is an invalid SocketAddress");
        }
        
        return DefaultMessageID.createWithSocketAddress(dst, calculator);
    }

    @Override
    public NodeRequest createNodeRequest(Contact contact, SocketAddress dst, KUID lookupId) {
        return new DefaultNodeRequest(createMessageID(dst), contact, lookupId);
    }

    @Override
    public NodeResponse createNodeResponse(Contact contact, Contact dst, 
            MessageID messageId, Contact[] nodes) {
        return new DefaultNodeResponse(messageId, contact, createSecurityToken(dst), nodes);
    }

    @Override
    public ValueRequest createValueRequest(Contact contact, SocketAddress dst, 
            KUID lookupId, KUID[] keys, DHTValueType valueType) {
        return new DefaultValueRequest(createMessageID(dst), contact, lookupId, keys, valueType);
    }

    @Override
    public ValueResponse createValueResponse(Contact contact, Contact dst, 
            MessageID messageId, float requestLoad, 
            DHTValueEntity[] entities, KUID[] secondaryKeys) {
        return new DefaultValueResponse(messageId, contact, requestLoad, secondaryKeys, entities);
    }

    @Override
    public PingRequest createPingRequest(Contact contact, SocketAddress dst) {
        return new DefaultPingRequest(createMessageID(dst), contact);
    }

    @Override
    public PingResponse createPingResponse(Contact contact, Contact dst, 
            MessageID messageId, SocketAddress externalAddress, BigInteger estimatedSize) {
        return new DefaultPingResponse(messageId, contact, externalAddress, estimatedSize);
    }

    @Override
    public StoreRequest createStoreRequest(Contact contact, SocketAddress dst, 
            SecurityToken securityToken, DHTValueEntity[] values) {
        return new DefaultStoreRequest(createMessageID(dst), contact, securityToken, values);
    }

    @Override
    public StoreResponse createStoreResponse(Contact contact, Contact dst, 
            MessageID messageId, StoreStatusCode[] status) {
        return new DefaultStoreResponse(messageId, contact, status);
    }
}
