package org.limewire.activation.impl;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Collections;

import org.limewire.activation.api.ActivationItem;
import org.limewire.io.InvalidDataException;
import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import com.google.inject.Singleton;
import com.google.inject.Inject;

@Singleton
public class ActivationResponseFactoryImpl implements ActivationResponseFactory {
    
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
    
    private final ActivationItemFactory activationItemFactory;
    
    private final Map<ActivationResponse.Type,
            ActivationResponseFactoryHelper> factoryByType = new EnumMap<ActivationResponse.Type,
            ActivationResponseFactoryHelper>(ActivationResponse.Type.class);
    
    @Inject
    public ActivationResponseFactoryImpl(ActivationItemFactory activationItemFactory) {
        this.activationItemFactory = activationItemFactory;
        initActivationResponseFactoryMap();
    }

    private void initActivationResponseFactoryMap() {
        factoryByType.put(ActivationResponse.Type.VALID, new ValidResponseFactory());
        factoryByType.put(ActivationResponse.Type.NOTFOUND, new NotFoundResponseFactory());
        factoryByType.put(ActivationResponse.Type.BLOCKED, new BlockedResponseFactory());    
    }

    @Override
    public ActivationResponse createFromJson(String json) throws InvalidDataException {
        return createJson(json, false);
    }

    @Override
    public ActivationResponse createFromDiskJson(String json) throws InvalidDataException {
        return createJson(json, true);
    }
    
    private ActivationResponse createJson(String json, boolean loadedFromDisk) throws InvalidDataException {
        
        JSONTokener tokener = new JSONTokener(json);
        try {
            JSONObject parentObj = new JSONObject(tokener);
            
            // parse values which are in all responses
            String response = parentObj.getString("response");
            ActivationResponse.Type type = ActivationResponse.Type.valueOf(response.toUpperCase());

            ActivationResponseFactoryHelper factory = factoryByType.get(type);
            return factory.createFromJsonObject(json, parentObj, loadedFromDisk);
        } catch (JSONException e) {
            throw new InvalidDataException("Error parsing JSON String " + json, e);
        } catch (Throwable e) {
            throw new InvalidDataException("Error parsing JSON String " + json, e);
        }
    }
    
    
    
    interface ActivationResponseFactoryHelper {
        ActivationResponse createFromJsonObject(String originalJson, 
                                                JSONObject parentObj,
                                                boolean loadedFromDisk) throws InvalidDataException;    
    }
    
    class ValidResponseFactory implements ActivationResponseFactoryHelper {
        @Override
        public ActivationResponse createFromJsonObject(String originalJson,
                                                       JSONObject parentObj,
                                                       boolean loadedFromDisk) throws InvalidDataException {
            try {            
                String lid = parentObj.getString("lid");
                ActivationResponse.Type type = ActivationResponse.Type.VALID;
                String mcode = parentObj.getString("mcode");
                int refresh = parentObj.getInt("refresh");
                List<ActivationItem> items = new ArrayList<ActivationItem>();
                
                JSONArray array = parentObj.getJSONArray("modules");
                for (int i=0; i<array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int moduleId = Integer.parseInt(obj.getString("id"));
                    String moduleName = obj.getString("name");
                    String purchaseDate = obj.getString("pur");
                    String expDate = obj.getString("exp");
                    ActivationItem.Status status = ActivationItem.Status.valueOf(obj.getString("status").toUpperCase());
                    
                    Date pur = formatter.parse(purchaseDate);
                    Date exp = formatter.parse(expDate);
                    ActivationItem item = null;
                    if(loadedFromDisk) {
                        item = activationItemFactory.createActivationItem(moduleId, moduleName, pur, exp, status);
                    } else {
                        item = activationItemFactory.createActivationItemFromDisk(moduleId, moduleName, pur, exp, status);
                    }
                    items.add(item);
                }
                return new ActivationResponse(originalJson, lid, type, mcode, refresh, items, null);
            } catch (JSONException e) {
                throw new InvalidDataException("Error parsing JSON String " + originalJson, e);
            } catch (ParseException e) {
                throw new InvalidDataException("Invalid date in JSON String " + originalJson, e);
            }
        }
    }
    
    class NotFoundResponseFactory implements ActivationResponseFactoryHelper {
        @Override
        public ActivationResponse createFromJsonObject(String originalJson,
                                                       JSONObject parentObj,
                                                       boolean loadedFromDisk) throws InvalidDataException {
            try {            
                String lid = parentObj.getString("lid");
                ActivationResponse.Type type = ActivationResponse.Type.NOTFOUND;
                String message = parentObj.has("message") ? parentObj.getString("message") : null;
                String mcode = parentObj.has("mcode") ? parentObj.getString("mcode") : null;
                int refresh = parentObj.has("refresh") ? parentObj.getInt("refresh") : 0;
                
                List<ActivationItem> items = Collections.emptyList();

                return new ActivationResponse(originalJson, lid, type, mcode, refresh, items, message);
            } catch (JSONException e) {
                throw new InvalidDataException("Error parsing JSON String " + originalJson, e);
            }
        }
    }
    
    class BlockedResponseFactory implements ActivationResponseFactoryHelper {
        @Override
        public ActivationResponse createFromJsonObject(String originalJson,
                                                       JSONObject parentObj,
                                                       boolean loadedFromDisk) throws InvalidDataException {
            try {            
                String lid = parentObj.getString("lid");
                ActivationResponse.Type type = ActivationResponse.Type.BLOCKED;
                String message = parentObj.has("message") ? parentObj.getString("message") : null;
                List<ActivationItem> items = Collections.emptyList();

                return new ActivationResponse(originalJson, lid, type, null, 0, items, message);
            } catch (JSONException e) {
                throw new InvalidDataException("Error parsing JSON String " + originalJson, e);
            }
        }
    }    
}