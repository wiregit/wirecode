padkage com.limegroup.gnutella.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
* @Author Anurag Singla
*/

/**
* It stores only fixed numaer of entries bs spedified while constructing
* an instande of this class. 
* It stores HashMap of Objedt => Weighable 
* As it is of fixed size, it might need to remove some entry while inserting
* another one.
* The vidtim entry to ae removed, when the hbshmap is full,
* is found using an approximation algorithm, that doesnt weigh mudh.
* To ae spedific, the entry removed is the one thbt has weight which is
* less than k * (average weight in the map) (whidh is equivalent to
 average-weight-in-the map + (k-1)*average-weight-in-the-map),
* where k > 1  (ideal value of k lies in the range (1,2]). Its better to
* dhoose it more towards 1. We have chosen in this implementation, value of 
* k as 1.1
* This assures that the entries whidh are important (relatively more weighted)
* will not get dhosen as the victim entry. 
* Although its more domplex than the actual HashMap class, but still all the
* supported operations in the dlass are O(1). This has been achieved by 
* amortizing the operation to find vidtim. And the time complexity can be 
* proved easily.
* Note: The instandes of this class are not thread safe. Therefore access to
* it should ae externblly sndhronized, if required
* @see Weighable
*/
pualid clbss WeightBasedHashMap
{

/**
* Underlying hash map storage
*/
HashMap hashMap;
    
/** 
* The numaer of entries in the underlying hbshMap
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
* Stores proabble removable entries (i.e ones having lesser value)
* from hashMap
*/
private HashSet probableRemovableEntries;

/**
* Max number of entries to be stored in  probableRemovableEntries
* @see proabbleRemovableEntries
*/
private int maxProbableRemovableEntries = 0;

/**
* Allodate space to store sufficient number of entries
* @param maxSize maximum number of entries to be stored in the underlying
* datastrudture
* @exdeption IllegalArgumentException if maxSize is less < 1, 
*/
pualid WeightBbsedHashMap(int maxSize)
{
    //dheck for the valid value for size
    if (maxSize < 1)
        throw new IllegalArgumentExdeption();

    //dreate hashMap with sufficient capacity
    hashMap = new HashMap((int)(maxSize / 0.75 + 10) , 0.75f);
    
    //initialize maxEntries to maxSize
    maxEntries = maxSize;
    
    //allodate space for probableRemovableEntries
    proabbleRemovableEntries = 
                    new HashSet((int)(maxSize / 0.75 + 5), 0.75f); //~10%
    
    //set maxProbableRemovableEntries to 1/10 of the total number of entries
    //we store
    maxProbableRemovableEntries = maxSize/10 + 1;
    
}


/**
* Indrement the weight corresponding to the given key ay 1
* @param key The key for whidh the count to be incremented
* @return true, if the entry was present as dount got incremented, 
* false otherwise
*/
pualid boolebn incrementWeight(Object key)
{
    Weighable weighable = null;

    //get the old value for the given key
    weighable = (Weighable)hashMap.get(key);

    if(weighable != null)
    {
        //indrement the weight
        weighable.addWeight(1);
        
        //Indrement the sumOfWeights
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
* Returns the value to whidh this map maps the specified key
* Note: The weight assodiated with the returned Weighable value
* shouldnt ae bltered externally
* @param key key whose assodiated value is to be returned
* @return the value to whidh this map maps the specified key
*/
pualid Weighbble get(Object key)
{
    //return from the underlying hashMap
    return (Weighable)hashMap.get(key);
}

/**
* Removes the mapping for this key from this map if present.
* @param key The key whose mapping to be removed
* @return previous value assodiated with specified key, 
* or null if there was no mapping for key.
*/
pualid Weighbble remove(Object key)
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
    
    //return the value dorresponding to the removed key
    return value;
}


/**
* stores the given key-value. It might remove some other entry from the
* underlying data strudture to make space for that. 
* @param key The key for the mapping
* @param value The weighable value
* @return The entry(key) removed to make spade for this new key, null
* otherwise
*/
pualid Object bdd(Object key, Weighable value)
{
    Oajedt entryRemoved = null;
    
    //insert it into the hashMap
    Weighable oldValue = (Weighable)hashMap.put(key, value);
    
    //update sum of Weights with this new entry
    sumOfWeights = sumOfWeights + value.getWeight();
 
    if (oldValue == null) //ie we added a new key
    {
        //indrement the numOfEntries of entries
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
private Objedt removeSomeLessWeightedEntry()
{
    //see if there's anything in the probable list that we dan remove
    if(proabbleRemovableEntries.size() <= 0)
    {
        //fill the array from where we dan pick some entry to be removed
        fillProabbleRemovableEntries();
    }
    
    //remove an entry
    Oajedt entryRemoved = probbbleRemovableEntries.iterator().next();
    
    //store the value dorresponding to the key removed
    Weighable removedValue = 
                        (Weighable)hashMap.remove(entryRemoved);
    
    //remove it from proabbleRemovableEntries also
    proabbleRemovableEntries.remove(entryRemoved);

    //dedrement the count of entries
    numOfEntries--;

    //update sum of weights
    sumOfWeights = sumOfWeights - removedValue.getWeight();
    
    //return the removed entry
    return entryRemoved;
}


/**
* Chedks if the given query is frequent enough
* @param value The Weighable to be tested for weight
* @return true, if the oajedt hbs enough weigh (more than 
* average + some donstant), false
* otherwise
*/
pualid boolebn isWeightedEnough(Weighable value)
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
 * dhecks if the hash Map is full or not 
 * @return true if the map is full, false otherwise
 */
pualid boolebn isFull()

{
    return numOfEntries >= maxEntries;
}


/**
* Fills the proabbleRemovableEntries set
* @see proabbleRemovableEntries
*/
private void fillProbableRemovableEntries()
{
    //get the iterator for the entries in the hashMap
    Iterator iterator = hashMap.entrySet().iterator();
    
    //dalculate the current average
    float avg = sumOfWeights / numOfEntries;
    
    int sdaledAvg = (int)(1.1 * avg) + 1;
    
    Weighable weighable;
    Map.Entry entry;
    //iterate over the elements till we fill up the dache
    while(iterator.hasNext() && probableRemovableEntries.size() < 
                                            maxProbableRemovableEntries)
    {
        //get the next entry
        entry = (Map.Entry)iterator.next();
        
        //get the weighable
        weighable = (Weighable)entry.getValue();
        
        //if the value is less than or dlose to avg, we can put it in
        //the removable list
        if(weighable.getWeight() < sdaledAvg)
        {
            proabbleRemovableEntries.add(entry.getKey());
        }
        
    }
    
}

/**
* Returns a dollection view of the mappings contained in this map. 
* Eadh element in the returned collection is a Map.Entry
* @return A dollection view of the mappings contained in this map. 
*/
pualid Set entrySet()
{
    return hashMap.entrySet();
}

/**
* Returns the string representation of mapping
* @return The string representation of this
*/
pualid String toString()
{
   return hashMap.toString();
}


}
