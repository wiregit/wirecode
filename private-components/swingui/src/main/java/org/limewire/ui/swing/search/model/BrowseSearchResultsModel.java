package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

import javax.swing.event.SwingPropertyChangeSupport;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Provider;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


/**
 * The implementation of SearchResultsModel for browse hosts.  
 * This assembles search results into grouped, filtered, and sorted
 * lists, provides access to details about the search request, and handles 
 * requests to download a search result.
 */
public class BrowseSearchResultsModel extends AbstractSearchResultsModel {
    private final FriendPresence friendPresence;
    private final Friend friend;
    private final RemoteLibraryManager remoteLibraryManager;
    private final FunctionList<SearchResult, VisualSearchResult> groupedResults;
    private final EventList<SearchResult> searchResults;
    
    
    private PropertyChangeSupport changeSupport = new SwingPropertyChangeSupport(this);
    
    private LibraryState libraryState;
    
	private ListenerSupport<FriendEvent> friendEventListenerSupport;
	private EventListener<FriendEvent> friendEventListener = new EventListener<FriendEvent>(){
        @Override
        @SwingEDTEvent
        public void handleEvent(FriendEvent event) {
            switch(event.getType()) {
            case ADDED:
            case REMOVED:
                if(friend != null && event.getData().getId().equals(friend.getId()))
                    updateState();
                break;                 
            }
        }
    };
    ListEventListener<FriendLibrary> friendLibraryListener = new ListEventListener<FriendLibrary>() {
		@Override
		public void listChanged(
				ListEvent<FriendLibrary> listChanges) {
			while (listChanges.next()) {
				EventList<FriendLibrary> item = listChanges
						.getSourceList();
				for (FriendLibrary library : item) {
					// if the presence changed for the friend,
					// update the library state
					if (friend != null
							&& library.getFriend().getId().equals(friend.getId())) {
						updateState();
					}
				}
			}
		}
	};
	private boolean areListenersInitialized;

    /**
     * Constructs a BasicSearchResultsModel with the specified search details,
     * search request object, and services.
     * @param host the host to be browsed.  Can be null to show files from all known hosts.
     */
    public BrowseSearchResultsModel(FriendPresence friendPresence, final RemoteLibraryManager remoteLibraryManager,
            Provider<PropertiableHeadings> propertiableHeadings,
            DownloadListManager downloadListManager,
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler,
            ListenerSupport<FriendEvent> availListeners) {
        super(null, downloadListManager, saveLocationExceptionHandler);

        this.friendPresence = friendPresence;
        this.friend = friendPresence == null? null : friendPresence.getFriend();
        
        this.remoteLibraryManager = remoteLibraryManager;   
        this.friendEventListenerSupport = availListeners;
        

        EventList<RemoteFileItem> baseLibraryList = new BasicEventList<RemoteFileItem>();
        
        if(friendPresence != null){
            PresenceLibrary presenceLibrary = remoteLibraryManager.addPresenceLibrary(friendPresence);
            libraryState = presenceLibrary.getState();     
            baseLibraryList.addAll(presenceLibrary.getModel());
        } else {
            baseLibraryList.addAll(remoteLibraryManager.getAllFriendsFileList().getModel());
            libraryState = LibraryState.LOADED;
        }
        
       
        searchResults = GlazedListsFactory.threadSafeList(new BasicEventList<SearchResult>());
        for (RemoteFileItem item : baseLibraryList) {
            searchResults.add(item.getSearchResult());
        }
        
        // Create list of visual search results where each element represents
        // a single group.
        groupedResults = GlazedListsFactory.functionList(
                searchResults, new BrowseResultConverter(propertiableHeadings));
               
        initialize(searchResults, groupedResults);
        
        // Initialize display category and sorted list.
        setSelectedCategory(SearchCategory.ALL);        
    }     
   
    
    /**
     * Installs the specified search listener and starts the search.  The
     * search listener should handle search results by calling the 
     * <code>addSearchResult(SearchResult)</code> method.
     */
    @Override
    public void start(SearchListener searchListener) {
        throw new UnsupportedOperationException("start method not supported in BrowseSearchResultsModel");
    }
    
    public LibraryState getLibraryState(){
        return libraryState;
    }
        
    /**
     * Updates a Set of friends that are online. Currently there is no easy way
     * to lookup if a friend is online, we must keep our own Set as a result. If the
     * friend's status changes while they are in view, the message that is displayed
     * is updated appropriately. In all cases the set will be updated appropriately.
     */
    public void initializeListeners() {   
    	if (!areListenersInitialized) {
    		
			areListenersInitialized = true;
			
			friendEventListenerSupport.addListener(friendEventListener);
			remoteLibraryManager.getSwingFriendLibraryList().addListEventListener(friendLibraryListener);
							
			// start out with the current state
			if (friend != null) {
				updateState();
			}
		}
    }
    
    private void updateState() {
        if (friend != null) {
            FriendLibrary friendLibrary = remoteLibraryManager.getFriendLibrary(friend);
            if (friendLibrary != null) {
                fireStateChange(friendLibrary.getState());
            } else {
                // no library presence which means they're either offline or not
                // using LW - either way we can't load
                fireStateChange(LibraryState.FAILED_TO_LOAD);
            }
        }
    }
    
    private void fireStateChange(LibraryState state){
        LibraryState oldState = libraryState;
        libraryState = state;
        changeSupport.firePropertyChange(new PropertyChangeEvent(this, "state", oldState, libraryState));
    }
          
    /**
     * Stops the search and removes the current search listener. 
     */
    @Override
    public void dispose() { 
        if (searchResults instanceof TransformedList){
            ((TransformedList)searchResults).dispose();
        }
    
        
        //remove presence library
        if (friendPresence != null) {                
            remoteLibraryManager.removePresenceLibrary(friendPresence);
        } 
        
        if (areListenersInitialized) {
			friendEventListenerSupport.removeListener(friendEventListener);
			remoteLibraryManager.getSwingFriendLibraryList().removeListEventListener(friendLibraryListener);
        }
        
        super.dispose();
    }
    
    @Override
    public SearchCategory getSearchCategory() {
        return SearchCategory.ALL;
    }      
    
    @Override
    public SearchCategory getFilterCategory() {
        return SearchCategory.ALL;
    }

    @Override
    public int getResultCount() {
        return searchResults.size();
    }
    
    @Override
    public String getSearchTitle() {
        if (friend == null){
            return I18n.tr("Browsing all friends");
        }
        return I18n.tr("Browsing {0}", friend.getRenderName());
    }
    
    @Override
    public String getSearchQuery() {
        //no query here
        return "";
    }

    /**
     * Adds the specified search result to the results list.
     */
    @Override
    public void addSearchResult(SearchResult result) {
        throw new UnsupportedOperationException("no addSearchResult() for browses");
    }

    /**
     * Removes the specified search result from the results list.
     */
    @Override
    public void removeSearchResult(SearchResult result) {
        throw new UnsupportedOperationException("no removeSearchResult() for browses");
    }    
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    	initializeListeners();
    	changeSupport.addPropertyChangeListener(listener);        
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    
    /**
     * A GlazedList function used to transform a search result into a VisualSearchResult with no grouping.
     */
    private static class BrowseResultConverter implements Function<SearchResult, VisualSearchResult> {
        private final Provider<PropertiableHeadings> propertiableHeadings;

        public BrowseResultConverter(Provider<PropertiableHeadings> propertiableHeadings) {
            this.propertiableHeadings = propertiableHeadings;
        }
      
        @Override
        public VisualSearchResult evaluate(SearchResult sourceValue) {
            return new SearchResultAdapter(Arrays.asList(sourceValue), propertiableHeadings);            
        }

    }
}
