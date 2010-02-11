package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.limewire.core.api.Application;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.browser.HistoryEntry;
import org.limewire.ui.swing.mainframe.StoreMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class StoreMenu extends MnemonicMenu implements DelayedMenuItemCreator {
    
    @InspectablePrimitive(value = "store visited", category = DataCategory.USAGE)
    private int storeVisited;
    
    private final Application application;    
    private final Navigator navigator;
    private final StoreMediator storeMediator;
    
    @Inject
    public StoreMenu(Application application,
            Navigator navigator, 
            StoreMediator storeMediator) {
        
        super(I18n.tr("&Store"));

        this.application = application;
        this.navigator = navigator;
        this.storeMediator = storeMediator;
        
        navigator.createNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME, storeMediator);
    }

    @Override
    public void createMenuItems() {        
        add(new AbstractAction(I18n.tr("&Home")) {
            @Override
           public void actionPerformed(ActionEvent e) {
                storeVisited++;
                navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
                storeMediator.getComponent().loadDefaultUrl();
           }
        });
        
        add(new StoreUrlAction(I18n.tr("&Log In"), "https://www.store.limewire.com/store/app/pages/account/LogIn/noDest/1/"));        
        add(new StoreUrlAction(I18n.tr("&Sign Up"), "https://www.store.limewire.com/store/app/pages/register/RegisterSelection/"));
        
        JMenu genres = new MnemonicMenu(I18n.tr("&Genres"));
        genres.add(new StoreUrlAction(I18n.tr("&Alternative"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/31/"));
        genres.add(new StoreUrlAction(I18n.tr("&Country"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/14/"));
        genres.add(new StoreUrlAction(I18n.tr("&Electronica"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/20/"));
        genres.add(new StoreUrlAction(I18n.tr("&Folk"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/3/"));
        genres.add(new StoreUrlAction(I18n.tr("&Jazz"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/6/"));
        genres.add(new StoreUrlAction(I18n.tr("&Pop"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/27/"));
        genres.add(new StoreUrlAction(I18n.tr("&Rap"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/8/"));
        genres.add(new StoreUrlAction(I18n.tr("&Rhythm and Blues"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/26/"));
        genres.add(new StoreUrlAction(I18n.tr("&Rock"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/1/"));
        genres.add(new StoreUrlAction(I18n.tr("&World"), "http://www.store.limewire.com/store/app/pages/genre/GenreHome/genreId/2/"));
        add(genres);
        
//        add(new StoreUrlAction(I18n.tr("&Free Songs"), "http://www.store.limewire.com/free"));
        add(new StoreUrlAction(I18n.tr("LimeWire Store Hel&p"), "http://www.store.limewire.com/store/app/pages/help/Help/"));
        
        final JMenu history = new MnemonicMenu(I18n.tr("His&tory"));
        history.addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
                history.removeAll();
            }
            @Override
            public void menuDeselected(MenuEvent e) {
                history.removeAll();                
            }
            @Override
            public void menuSelected(MenuEvent e) {
                // On OS-X we fill the history list on a thread off of the EDT thread, because the call to storeMediator.getComponent().getHistory()
                // was going into some native mozilla code which was then deadlocking somewhere and causing LimeWire to freeze.
                if (OSUtils.isMacOSX()) {
                    new Thread(new Runnable() {
                        public void run() {
                            final AtomicReference<Integer> currentPosition = new AtomicReference<Integer>();
                            final Iterable<HistoryEntry> historyEntries = storeMediator.getComponent().getHistory(currentPosition);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    addBrowserHistoryToHistoryMenu(historyEntries, currentPosition);
                                }
                            });
                        }
                    }).start();
                } else {
                    final AtomicReference<Integer> currentPosition = new AtomicReference<Integer>();
                    final Iterable<HistoryEntry> historyEntries = storeMediator.getComponent().getHistory(currentPosition);
                    addBrowserHistoryToHistoryMenu(historyEntries, currentPosition);
                }
            }
            
            private void addBrowserHistoryToHistoryMenu(Iterable<HistoryEntry> historyEntries, AtomicReference<Integer> currentPosition) {
                for(HistoryEntry entry : historyEntries) {
                    JMenuItem item = history.add(new HistoryAction(entry));
                    if(entry.getIndex() == currentPosition.get()) {
                        FontUtils.bold(item);
                    }
                }
            }
        });
        add(history);
    }
    
    private class HistoryAction extends AbstractAction {
        private final HistoryEntry entry;
        
        public HistoryAction(HistoryEntry entry) {
            super(entry.getName());
            putValue(TOOL_TIP_TEXT_KEY, entry.getUri());
            this.entry = entry;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
            storeMediator.getComponent().loadHistoryEntry(entry);
        }
    }
    
    
    private class StoreUrlAction extends AbstractAction {
        private final String url;
        
        public StoreUrlAction(String name, String url) {
            super(name);
            this.url = application.addClientInfoToUrl(url);
        }
        
        
        @Override
        public void actionPerformed(ActionEvent e) {
            navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
            storeMediator.getComponent().load(url);
        }
    }
}
