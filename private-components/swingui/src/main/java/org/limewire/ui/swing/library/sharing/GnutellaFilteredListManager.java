package org.limewire.ui.swing.library.sharing;

import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.ui.swing.components.Disposable;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.impl.ThreadSafeList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;

/**
 * Thread safe
 */
class GnutellaFilteredListManager implements Disposable {
    private ThreadSafeList<SharingTarget> noShareFriendList;
    private FilterList<SharingTarget> filteredNoShareFriendList;
    private ThreadSafeList<SharingTarget> noShareGnutellaList;
    private FilterList<SharingTarget> filteredNoShareGnutellaList;
    private TextMatcherEditor<SharingTarget> textMatcher;
    private Matcher<SharingTarget> gnutellaMatcher;
    
    private ThreadSafeList<SharingTarget> compositeListThreadSafe;
    
    
    
    public GnutellaFilteredListManager() {
        setupSubLists();
    }
    
    private void setupSubLists() {
        CompositeList<SharingTarget> compositeList = new CompositeList<SharingTarget>();
        compositeListThreadSafe = GlazedListsFactory.threadSafeList(compositeList);
        noShareFriendList = GlazedListsFactory.threadSafeList(compositeList.createMemberList());

        //using TextComponentMatcherEditor would cause problems because it also uses DocumentListener so we 
        //have no guarantee about the order of sorting and selecting
        TextFilterator<SharingTarget> textFilter = new TextFilterator<SharingTarget>() {
            @Override
            public void getFilterStrings(List<String> baseList, SharingTarget element) {
                baseList.add(element.getFriend().getName());
                baseList.add(element.getFriend().getId());
            }
        };
        textMatcher = new TextMatcherEditor<SharingTarget>(textFilter);
        filteredNoShareFriendList = GlazedListsFactory.filterList(noShareFriendList, textMatcher);   
        
        
        noShareGnutellaList = GlazedListsFactory.threadSafeList(compositeList.createMemberList());
        gnutellaMatcher = new Matcher<SharingTarget>() {
            @Override
            public  boolean matches(SharingTarget item) {
                return filteredNoShareFriendList.size() > 0 || textMatcher.getMatcher().matches(item);
            }
        };
        filteredNoShareGnutellaList = GlazedListsFactory.filterList(noShareGnutellaList, gnutellaMatcher);            
        
        compositeList.addMemberList(filteredNoShareFriendList);
        compositeList.addMemberList(filteredNoShareGnutellaList);
    }

    public void addMatcherEditorListener(MatcherEditor.Listener<SharingTarget> listener) {
        textMatcher.addMatcherEditorListener(listener);
    }

    public void setFilterText(String[] strings){
        textMatcher.setFilterText(strings);
        //re-setting the matcher forces the list to update.  doing this from
        //a listener on textMatcher works one key stroke late.
        filteredNoShareGnutellaList.setMatcher(gnutellaMatcher);
    }
    
    public int getUnfilteredSize(){
        return noShareFriendList.size() + noShareGnutellaList.size();
    }
    
    
    public boolean add(SharingTarget friend){
        if(friend.isGnutellaNetwork()){
            return noShareGnutellaList.add(friend);
        } else {
            return noShareFriendList.add(friend);
        }
    }
    
    public boolean remove(SharingTarget friend){
        return compositeListThreadSafe.remove(friend);
    }
    
    public void clear(){
        compositeListThreadSafe.clear();
    }
    
    public void dispose(){
         compositeListThreadSafe .dispose();
         filteredNoShareFriendList.dispose();
         noShareFriendList.dispose();
         filteredNoShareGnutellaList.dispose();
         noShareGnutellaList.dispose();
    }
    
    public ThreadSafeList<SharingTarget> getThreadSafeList(){
        return compositeListThreadSafe;
    }
    
}
