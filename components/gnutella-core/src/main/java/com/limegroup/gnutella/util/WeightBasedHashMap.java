package com.limegroup.gnutella.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
* @Author Anurag Singla
*/

/**
* It stores only fixed number of entries as specified while constructing
* an instance of this class. 
* It stores HashMap of Object => Weighable 
* As it is of fixed size, it might need to remove some entry while inserting
* another one.
* The victim entry to be removed, when the hashmap is full,
* is found using an approximation algorithm, that doesnt weigh much.
* To be specific, the entry removed is the one that has weight which is
* less than k * (average weight in the map) (which is equivalent to
 average-weight-in-the map + (k-1)*average-weight-in-the-map),
* where k > 1  (ideal value of k lies in the range (1,2]). Its better to
* choose it more towards 1. We have chosen in this implementation, value of 
* k as 1.1
* This assures that the entries which are important (relatively more weighted)
* will not get chosen as the victim entry. 
* Although its more complex than the actual HashMap class, but still all the
* supported operations in the class are O(1). This has been achieved by 
* amortizing the operation to find victim. And the time complexity can be 
* proved easily.
* Note: The instances of this class are not thread safe. Therefore access to
* it should be externally snchronized, if required
* @see Weighable
*/
public class WeightBasedHashMap
{

/**
* Underlying hash map storage
*/
HashMap hashMap;
    
/** 
* The number of entries in the underlying hashMap
*/
private int numOfEntries = 0;

/** 
* The max number of entries we should store
*/
private int maxEntries = 0;

/**
* Sum of the weights of all the entries in the underlying hashMap
*/
private long sumOfWeights = 0;


/**
* Stores probable removable entries (i.e ones having lesser value)
* from hashMap
*/
private HashSet probableRemovableEntries;

/**
* Max number of entries to be stored in  probableRemovableEntries
* @see probableRemovableEntries
*/
private int maxProbableRemovableEntries = 0;

/**
* Allocate space to store sufficient number of entries
* @param maxSize maximum number of entries to be stored in the underlying
* datastructure
* @exception IllegalArgumentException if maxSize is less < 1, 
*/
public WeightBasedHashMap(int maxSize)
{
    //check for the valid value for size
    if (maxSize < 1)
        throw new IllegalArgumentException();

    //create hashMap with sufficient capacity
    hashMap = new HashMap((int)(maxSize / 0.75 + 10) , 0.75f);
    
    //initialize maxEntries to maxSize
    maxEntries = maxSize;
    
    //allocate space for probableRemovableEntries
    probableRemovableEntries = 
                    new HashSet((int)(maxSize / 0.75 + 5), 0.75f); //~10%
    
    //set maxProbableRemovableEntries to 1/10 of the total number of entries
    //we store
    maxProbableRemovableEntries = maxSize/10 + 1;
    
}


/**
* Increment the weight corresponding to the given key by 1
* @param key The key for which the count to be incremented
* @return true, if the entry was present as count got incremented, 
* false otherwise
*/
public boolean incrementWeight(Object key)
{
    Weighable weighable = null;

    //get the old value for the given key
    weighable = (Weighable)hashMap.get(key);

    if(weighable != null)
    {
        //increment the weight
        weighable.addWeight(1);
        
        //Increment the sumOfWeights
        sumOfWeights++;
        
        //return true
        return true;
    }
    else //if the mapping doesnt exist
    {
        //return false;
        return false;
    }    
}


/**
* Returns the value to which this map maps the specified key
* Note: The weight associated with the returned Weighable value
* shouldnt be altered externally
* @param key key whose associated value is to be returned
* @return the value to which this map maps the specified key
*/
public Weighable get(Object key)
{
    //return from the underlying hashMap
    return (Weighable)hashMap.get(key);
}

/**
* Removes the mapping for this key from this map if present.
* @param key The key whose mapping to be removed
* @return previous value associated with specified key, 
* or null if there was no mapping for key.
*/
public Weighable remove(Object key)
{
    //remove the entry and store the value the removed key mapped to
    Weighable value = (Weighable)hashMap.remove(key);
    
    if(value != null)
    {
        //adjust sum of weights
        sumOfWeights -= value.getWeight();
        
        //adjust num of entries
        numOfEntries--;
    }
    
    //return the value corresponding to the removed key
    return value;
}


/**
* stores the given key-value. It might remove some other entry from the
* underlying data structure to make space for that. 
* @param key The key for the mapping
* @param value The weighable value
* @return The entry(key) removed to make space for this new key, null
* otherwise
*/
public Object add(Object key, Weighable value)
{
    Object entryRemoved = null;
    
    //insert it into the hashMap
    Weighable oldValue = (Weighable)hashMap.put(key, value);
    
    //update sum of Weights with this new entry
    sumOfWeights = sumOfWeights + value.getWeight();
 
    if (oldValue == null) //ie we added a new key
    {
        //increment the numOfEntries of entries
        numOfEntries++;
        
        //if the numOfEntries is more than the maxEntries,
        //we should delete some entry
        if(numOfEntries > maxEntries)
        {
            //remove some less weighted entry
            //it also adjustes sumOfWeights as well as numEntries
            entryRemoved = removeSomeLessWeightedEntry();
        }
    }
    else //we didnt add anything new, but updated the old mapping
    {
        //Adjust sum of Weights
        sumOfWeights = sumOfWeights - oldValue.getWeight();
        
        //no need to update the numOfEntries as we didnt add anything new
    }
    
    //return the removed entry
    return entryRemoved;
    
}

/**
* It removes a low weigt entry
* @modifies sumOfWeights, numOfEntries
*/ 
private Object removeSomeLessWeightedEntry()
{
    //see if there's anything in the probable list that we can remove
    if(probableRemovableEntries.size() <= 0)
    {
        //fill the array from where we can pick some entry to be removed
        fillProbableRemovableEntries();
    }
    
    //remove an entry
    Object entryRemoved = probableRemovableEntries.iterator().next();
    
    //store the value corresponding to the key removed
    Weighable removedValue = 
                        (Weighable)hashMap.remove(entryRemoved);
    
    //remove it from probableRemovableEntries also
    probableRemovableEntries.remove(entryRemoved);

    //decrement the count of entries
    numOfEntries--;

    //update sum of weights
    sumOfWeights = sumOfWeights - removedValue.getWeight();
    
    //return the removed entry
    return entryRemoved;
}


/**
* Checks if the given query is frequent enough
* @param value The Weighable to be tested for weight
* @return true, if the object has enough weigh (more than 
* average + some constant), false
* otherwise
*/
public boolean isWeightedEnough(Weighable value)
{
    //get the average
    int average = (int)( sumOfWeights / numOfEntries) ;
    
    //give some margin over average
    if(value.getWeight() > average + 5)
    {
        return true;
    }
    else
    {
        return false;
    }
}

/**
 * checks if the hash Map is full or not 
 * @return true if the map is full, false otherwise
 */
public boolean isFull()

{
    return numOfEntries >= maxEntries;
}


/**
* Fills the probableRemovableEntries set
* @see probableRemovableEntries
*/
private void fillProbableRemovableEntries()
{
    //get the iterator for the entries in the hashMap
    Iterator iterator = hashMap.entrySet().iterator();
    
    //calculate the current average
    float avg = sumOfWeights / numOfEntries;
    
    int scaledAvg = (int)(1.1 * avg) + 1;
    
    Weighable weighable;
    Map.Entry entry;
    //iterate over the elements till we fill up the cache
    while(iterator.hasNext() && probableRemovableEntries.size() < 
                                            maxProbableRemovableEntries)
    {
        //get the next entry
        entry = (Map.Entry)iterator.next();
        
        //get the weighable
        weighable = (Weighable)entry.getValue();
        
        //if the value is less than or close to avg, we can put it in
        //the removable list
        if(weighable.getWeight() < scaledAvg)
        {
            probableRemovableEntries.add(entry.getKey());
        }
        
    }
    
}

/**
* Returns a collection view of the mappings contained in this map. 
* Each element in the returned collection is a Map.Entry
* @return A collection view of the mappings contained in this map. 
*/
public Set entrySet()
{
    return hashMap.entrySet();
}

/**
* Returns the string representation of mapping
* @return The string representation of this
*/
public String toString()
{
   return hashMap.toString();
}


}
