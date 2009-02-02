package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.Presence;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class FriendLibrariesTest extends BaseTestCase {

    public FriendLibrariesTest(String name) {
        super(name);
    }

    public void testIndexing1FriendLibraryAndFileByFileNameOnly() {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();
        final EventList<RemoteFileItem> remoteFileItemList1 = new BasicEventList<RemoteFileItem>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final RemoteFileItem remoteFileItem1 = context.mock(RemoteFileItem.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;

        final Presence presence1 = context.mock(Presence.class);
        final String presenceId1 = "1";

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(presenceLibrary1).getModel();
                will(returnValue(remoteFileItemList1));

                allowing(remoteFileItem1).getName();
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    will(returnValue(null));
                }
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        remoteFileItemList1.add(remoteFileItem1);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        Collection<RemoteFileItem> matchingItems = friendLibraries.getMatchingItems("name",
                SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.ALL);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.DOCUMENT);
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems("blah", SearchCategory.AUDIO);
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.ALL);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

    }

    public void testIndexing1FriendLibraryAndMultipleFilesByFileNameOnly() {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();
        final EventList<RemoteFileItem> remoteFileItemList1 = new BasicEventList<RemoteFileItem>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final RemoteFileItem remoteFileItem1 = context.mock(RemoteFileItem.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;

        final RemoteFileItem remoteFileItem2 = context.mock(RemoteFileItem.class);
        final String name2 = "name2";
        final Category category2 = Category.DOCUMENT;

        final RemoteFileItem remoteFileItem3 = context.mock(RemoteFileItem.class);
        final String name3 = "blah1";
        final Category category3 = Category.AUDIO;

        final Presence presence1 = context.mock(Presence.class);
        final String presenceId1 = "1";

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(presenceLibrary1).getModel();
                will(returnValue(remoteFileItemList1));

                allowing(remoteFileItem1).getName();
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                allowing(remoteFileItem2).getName();
                will(returnValue(name2));
                allowing(remoteFileItem2).getCategory();
                will(returnValue(category2));

                allowing(remoteFileItem3).getName();
                will(returnValue(name3));
                allowing(remoteFileItem3).getCategory();
                will(returnValue(category3));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem2).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem3).getProperty(filePropertyKey);
                    will(returnValue(null));
                }
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        remoteFileItemList1.add(remoteFileItem1);
        remoteFileItemList1.add(remoteFileItem2);
        remoteFileItemList1.add(remoteFileItem3);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        Collection<RemoteFileItem> matchingItems = friendLibraries.getMatchingItems("name",
                SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.DOCUMENT);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.ALL);
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.DOCUMENT);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name3);

        matchingItems = friendLibraries.getMatchingItems("blah", SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem3);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.ALL);
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

    }
    
    public void testIndexingMultipleFriendLibraryAndMultipleFilesByFileNameOnly() {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();
        final EventList<RemoteFileItem> remoteFileItemList1 = new BasicEventList<RemoteFileItem>();
        
        final EventList<PresenceLibrary> presenceLibraryList2 = new BasicEventList<PresenceLibrary>();
        final EventList<RemoteFileItem> remoteFileItemList2 = new BasicEventList<RemoteFileItem>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final FriendLibrary friendLibrary2 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary2 = context.mock(PresenceLibrary.class);

        
        final RemoteFileItem remoteFileItem1 = context.mock(RemoteFileItem.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;

        final RemoteFileItem remoteFileItem2 = context.mock(RemoteFileItem.class);
        final String name2 = "name2";
        final Category category2 = Category.DOCUMENT;

        final RemoteFileItem remoteFileItem3 = context.mock(RemoteFileItem.class);
        final String name3 = "blah1";
        final Category category3 = Category.AUDIO;

        final Presence presence1 = context.mock(Presence.class);
        final String presenceId1 = "1";
        
        final Presence presence2 = context.mock(Presence.class);
        final String presenceId2 = "2";

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(presenceLibrary1).getModel();
                will(returnValue(remoteFileItemList1));

                allowing(friendLibrary2).getPresenceLibraryList();
                will(returnValue(presenceLibraryList2));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary2).getPresence();
                will(returnValue(presence2));
                allowing(presence2).getPresenceId();
                will(returnValue(presenceId2));
                allowing(presenceLibrary2).getModel();
                will(returnValue(remoteFileItemList2));

                allowing(remoteFileItem1).getName();
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                allowing(remoteFileItem2).getName();
                will(returnValue(name2));
                allowing(remoteFileItem2).getCategory();
                will(returnValue(category2));

                allowing(remoteFileItem3).getName();
                will(returnValue(name3));
                allowing(remoteFileItem3).getCategory();
                will(returnValue(category3));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem2).getProperty(filePropertyKey);
                    will(returnValue(null));
                    allowing(remoteFileItem3).getProperty(filePropertyKey);
                    will(returnValue(null));
                }
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);
        presenceLibraryList1.add(presenceLibrary1);
        
        friendLibraryList.add(friendLibrary2);
        presenceLibraryList2.add(presenceLibrary2);

        remoteFileItemList1.add(remoteFileItem1);
        remoteFileItemList2.add(remoteFileItem2);
        remoteFileItemList2.add(remoteFileItem3);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        Collection<RemoteFileItem> matchingItems = friendLibraries.getMatchingItems("name",
                SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.DOCUMENT);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.ALL);
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.DOCUMENT);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem2);

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name3);

        matchingItems = friendLibraries.getMatchingItems("blah", SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem3);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, name1);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, name2);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.ALL);
        assertEquals(2, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        assertContains(matchingItems, remoteFileItem2);

    }
    
    public void testIndexingFileMetaData() {

        Mockery context = new Mockery();

        final EventList<FriendLibrary> friendLibraryList = new BasicEventList<FriendLibrary>();
        final EventList<PresenceLibrary> presenceLibraryList1 = new BasicEventList<PresenceLibrary>();
        final EventList<RemoteFileItem> remoteFileItemList1 = new BasicEventList<RemoteFileItem>();

        final RemoteLibraryManager remoteLibraryManager = context.mock(RemoteLibraryManager.class);
        final FriendLibrary friendLibrary1 = context.mock(FriendLibrary.class);
        final PresenceLibrary presenceLibrary1 = context.mock(PresenceLibrary.class);

        final RemoteFileItem remoteFileItem1 = context.mock(RemoteFileItem.class);
        final String name1 = "name1";
        final Category category1 = Category.AUDIO;
        final Map<FilePropertyKey, Object> properties1 = new HashMap<FilePropertyKey, Object>();
        final String album1 = "nameo";
        properties1.put(FilePropertyKey.ALBUM, album1);

        final Presence presence1 = context.mock(Presence.class);
        final String presenceId1 = "1";

        context.checking(new Expectations() {
            {
                allowing(friendLibrary1).getPresenceLibraryList();
                will(returnValue(presenceLibraryList1));
                allowing(remoteLibraryManager).getFriendLibraryList();
                will(returnValue(friendLibraryList));

                allowing(presenceLibrary1).getPresence();
                will(returnValue(presence1));
                allowing(presence1).getPresenceId();
                will(returnValue(presenceId1));
                allowing(presenceLibrary1).getModel();
                will(returnValue(remoteFileItemList1));

                allowing(remoteFileItem1).getName();
                will(returnValue(name1));
                allowing(remoteFileItem1).getCategory();
                will(returnValue(category1));

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    allowing(remoteFileItem1).getProperty(filePropertyKey);
                    if(properties1.containsKey(filePropertyKey)) {
                        will(returnValue(properties1.get(filePropertyKey)));
                    } else {
                        will(returnValue(null));
                    }
                }
            }
        });

        FriendLibraries friendLibraries = new FriendLibraries();
        friendLibraries.register(remoteLibraryManager);

        friendLibraryList.add(friendLibrary1);

        presenceLibraryList1.add(presenceLibrary1);

        remoteFileItemList1.add(remoteFileItem1);

        Collection<String> suggestions = friendLibraries.getSuggestions("name",
                SearchCategory.AUDIO);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        Collection<RemoteFileItem> matchingItems = friendLibraries.getMatchingItems("name",
                SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.ALL);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("name", SearchCategory.DOCUMENT);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems("name", SearchCategory.DOCUMENT);
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("blah", SearchCategory.AUDIO);
        assertEquals(0, suggestions.size());

        matchingItems = friendLibraries.getMatchingItems("blah", SearchCategory.AUDIO);
        assertEquals(0, matchingItems.size());

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.AUDIO);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.AUDIO);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

        suggestions = friendLibraries.getSuggestions("na", SearchCategory.ALL);
        assertEquals(2, suggestions.size());
        assertContains(suggestions, name1);
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems("na", SearchCategory.ALL);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);
        
        suggestions = friendLibraries.getSuggestions("nameo", SearchCategory.ALL);
        assertEquals(1, suggestions.size());
        assertContains(suggestions, album1);

        matchingItems = friendLibraries.getMatchingItems("nameo", SearchCategory.ALL);
        assertEquals(1, matchingItems.size());
        assertContains(matchingItems, remoteFileItem1);

    }

}
