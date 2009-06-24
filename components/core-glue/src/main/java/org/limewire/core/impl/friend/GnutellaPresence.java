package org.limewire.core.impl.friend;

import java.net.InetAddress;

import org.limewire.core.settings.SearchSettings;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.impl.AbstractFriendPresence;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.PushEndpoint;

/**
 * An implementation of FriendPresence for a Gnutella address.  For example,
 * a GnutellaPresence can be created for a Connection, which is supplied to
 * the RemoteLibraryManager to add and browse the presence.
 */
public class GnutellaPresence extends AbstractFriendPresence implements FriendPresence {
    
    private final Friend friend;
    private final String id;
    
    private static final String UNKNOWN_ADDRESS_DESCRIPTION = "Unknown";
    
    private static final String[] adjs = { "Almond", "Brass", "Apricot", "Aqua", "Asparagus", "Tangerine",
            "Awesome", "Banana", "Bear", "Bittersweet", "Fast", "Blue", "Bell", "Gray", "Green",
            "Violet", "Red", "Pink", "Orange", "Sienna", "Cool", "Earthy", "Caribbean", "Elder",
            "Pink", "Cerise", "Cerulean", "Chestnut", "Copper", "Better", "Candy", "Cranberry",
            "Dandelion", "Denim", "Gray", "Sand", "Desert", "Eggplant", "Lime", "Electric",
            "Famous", "Fern", "Forest", "Fuchsia", "Fuzzy", "Tree", "Gold", "Apple", "Smith",
            "Magenta", "Indigo", "Jazz", "Berry", "Jam", "Jungle", "Lemon", "Cold", "Lavender",
            "Hot", "New", "Ordinary", "Magenta", "Frowning", "Mint", "Mahogany", "Pretty", "Strange",
            "Grumpy", "Itchy", "Maroon", "Melon", "Midnight", "Clumsy", "Better", "Smiling",
            "Navy", "Neon", "Olive", "Orchid", "Outer", "Tame", "Cheerful", "Peach", "Periwinkle",
            "Pig", "Pine", "Nutty", "Plum", "Purple", "Rose", "Salmon", "Scarlet", "Nice", "Jolly",
            "Great", "Silver", "Sky", "Spring", "Long", "Glow", "Set", "Happy", "Tan", "Thistle",
            "Timber", "Tough", "Torch", "Smart", "Funny", "Tropical", "Tumble", "Ultra", "White",
            "Wild", "Yellow", "Eager", "Joyous", "Jumpy", "Kind", "Lucky", "Meek", "Nifty",
            "Adorable", "Aggressive", "Alert", "Attractive", "Average", "Bright", "Fragile",
            "Graceful", "Handsome", "Light", "Long", "Misty", "Muddy", "Plain", "Poised",
            "Precious", "Shiny", "Sparkling", "Stormy", "Wide", "Alive", "Annoying", "Better",
            "Brainy", "Busy", "Clever", "Clumsy", "Crazy", "Curious", "Easy", "Famous", "Frail",
            "Gifted", "Important", "Innocent", "Modern", "Mushy", "Odd", "Open", "Powerful",
            "Real", "Shy", "Sleepy", "Super", "Tame", "Tough", "Vast", "Wild", "Wrong", "Annoyed",
            "Anxious", "Crazy", "Dizzy", "Dull", "Evil", "Foolish", "Frantic", "Grieving",
            "Grumpy", "Helpful", "Hungry", "Lazy", "Lonely", "Scary", "Tense", "Weary", "Worried",
            "Brave", "Calm", "Charming", "Magic", "Easer", "Elated", "Enchanting", "Excited",
            "Fair", "Fine", "Friendly", "Funny", "Gentle", "Good", "Happy", "Healthy", "Jolly",
            "Kind", "Lovely", "Nice", "Perfect", "Proud", "Silly", "Smiling", "Thankful", "Witty",
            "Zany", "Big", "Fat", "Great", "Huge", "Immense", "Puny", "Scrawny", "Short", "Small",
            "Tall", "Teeny", "Tiny", "Faint", "Harsh", "Loud", "Melodic", "Mute", "Noisy", "Quiet",
            "Raspy", "Soft", "Whispering", "Ancient", "Fast", "Late", "Long", "Modern", "Old",
            "Quick", "Rapid", "Short", "Slow", "Swift", "Bitter", "Fresh", "Ripe", "Rotten",
            "Salty", "Sour", "Spicy" };

    private static final String[] nouns = { "Alligator", "Alpaca", "Antelope", "Badger", "Armadillo",
            "Bat", "Bear", "Bee", "Bird", "Bison", "Buffalo", "Boar", "Butterfly", "Camel", "Cat",
            "Cattle", "Cow", "Chicken", "Clam", "Cockroach", "Codfish", "Coyote", "Crane", "Crow",
            "Deer", "Dinosaur", "Velociraptor", "Dog", "Dolphin", "Donkey", "Dove", "Duck",
            "Eagle", "Eel", "Elephant", "Elk", "Emu", "Falcon", "Ferret", "Fish", "Finch", "Fly",
            "Fox", "Frog", "Gerbil", "Giraffe", "Gnat", "Gnu", "Goat", "Goose", "Gorilla",
            "Grasshopper", "Grouse", "Gull", "Hamster", "Hare", "Hawk", "Hedgehog", "Heron",
            "Hornet", "Hog", "Horse", "Hound", "Hummingbird", "Hyena", "Jay", "Jellyfish",
            "Kangaroo", "Koala", "Lark", "Leopard", "Lion", "Llama", "Mallard", "Mole", "Monkey",
            "Moose", "Mosquito", "Mouse", "Mule", "Nightingale", "Opossum", "Ostrich", "Otter",
            "Owl", "Ox", "Oyster", "Panda", "Parrot", "Peafowl", "Penguin", "Pheasant", "Pig",
            "Pigeon", "Platypus", "Porpoise", "PrarieDog", "Pronghorn", "Quail", "Rabbit",
            "Raccoon", "Rat", "Raven", "Reindeer", "Rhinoceros", "Seal", "Seastar", "Serval",
            "Shark", "Sheep", "Skunk", "Snake", "Snipe", "Sparrow", "Spider", "Squirrel", "Swallow",
            "Swan", "Termite", "Tiger", "Toad", "Trout", "Turkey", "Turtle", "Wallaby", "Walrus",
            "Wasp", "Weasel", "Whale", "Wolf", "Wombat", "Woodpecker", "Wren", "Yak", "Zebra",
            "Ball", "Bed", "Book", "Bun", "Can", "Cake", "Cap", "Car", "Cat", "Day", "Fan", "Feet",
            "Hall", "Hat", "Hen", "Jar", "Kite", "Man", "Map", "Men", "Panda", "Pet", "Pie", "Pig",
            "Pot", "Sun", "Toe", "Apple", "Armadillo", "Banana", "Bike", "Book", "Clam", "Mushroom",
            "Clover", "Club", "Corn", "Crayon", "Crown", "Crib", "Desk", "Dress", "Flower", "Fog",
            "Game", "Hill", "Home", "Hornet", "Hose", "Joke", "Juice", "Mask", "Mice", "Alarm",
            "Bath", "Bean", "Beam", "Camp", "Crook", "Deer", "Dock", "Doctor", "Frog", "Good",
            "Jam", "Face", "Honey", "Kitten", "Fruit", "Fuel", "Cable", "Calculator", "Circle",
            "Guitar", "Bomb", "Border", "Apparel", "Activity", "Desk", "Art", "Colt", "Cyclist",
            "Biker", "Blogger", "Anchoby", "Carp", "Glassfish", "Clownfish", "Barracuda", "Eel",
            "Moray", "Stingray", "Flounder", "Swordfish", "Marlin", "Pipefish", "Grunter",
            "Grunion", "Grouper", "Guppy", "Gulper", "Crab", "Lobster", "Halibut", "Hagfish",
            "Horsefish", "Seahorse", "Jellyfish", "Killifish", "Trout", "Pike", "Ray", "Razorfish",
            "Ragfish", "Hamster", "Gerbil", "Mouse", "Gnome", "Shark", "Snail", "Skilfish" };
    
    /**
     * Constructs a GnutellaPresence with the specified address and id.
     */
    public GnutellaPresence(Address address, String id) {
        this.id = id;
        addFeature(new AddressFeature(address));
        this.friend = new GnutellaFriend(describe(address),
                describeFriendly(address), id, this);
    }
    
    private String describe(Address address) {
        if(address instanceof Connectable || address instanceof PushEndpoint) {
            IpPort ipp = (IpPort)address;
            InetAddress inetAddr = ipp.getInetAddress();
            return inetAddr == null ? null : inetAddr.getHostAddress();
        } else {
            return address.getAddressDescription();
        }
    }
    
    private String describeFriendly(Address address) {
        if(!SearchSettings.FRIENDLY_ADDRESS_DESCRIPTIONS.getValue())
            return describe(address);
        if(address instanceof Connectable || address instanceof PushEndpoint) {
            // Convert IP addr into a #.
            IpPort ipp = (IpPort)address;
            InetAddress inetAddr = ipp.getInetAddress();
            if(inetAddr == null)
                return null;
            byte[] addr = inetAddr.getAddress();
            
            if (addr.length != 4) {
                return UNKNOWN_ADDRESS_DESCRIPTION;
            }

            //create a fake name
            int i1 = ByteUtils.ubyte2int(addr[0]);
            int i2 = ByteUtils.ubyte2int(addr[1]);
            int i3 = ByteUtils.ubyte2int(addr[2]);
            int i4 = ByteUtils.ubyte2int(addr[3]);
            return adjs[i1] + nouns[i2] + "-" + i3 + "-" + i4;
        } else {
            return address.getAddressDescription();
        }
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return id;
    }

    @Override
    public Type getType() {
        return Type.available;
        }

    @Override
    public String getStatus() {
        return "";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Mode getMode() {
        return Mode.available;
    }
}
