package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.mainframe.ChangeLanguageAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ViewMenu extends DelayedMnemonicMenu {

    private final Provider<HideTransferTrayAction> hideTransferTrayTrayActionProvider;
    private final Provider<ShowDownloadsTrayAction> showDownloadsTrayActionProvider;
    private final Provider<ShowUploadsTrayAction> showUploadsTrayActionProvider;
    private final Provider<ChangeLanguageAction> changeLanguageActionProvider;
    private final Provider<TransferTrayNavigator> transferTrayNavigator;
    private final Provider<HomeMediator> homeMediatorProvider;
    private final Provider<Navigator> navigatorProvider;

    @Inject
    public ViewMenu(
            Provider<HideTransferTrayAction> hideTransferTrayTrayActionProvider,
            Provider<ShowDownloadsTrayAction> showHideDownloadTrayAction,
            Provider<ShowUploadsTrayAction> uploadTrayActionProvider,
            Provider<ChangeLanguageAction> changeLanguageActionProvider,
            Provider<TransferTrayNavigator> transferTrayNavigator,
            Provider<HomeMediator> homeMediatorProvider,
            Provider<Navigator> navigatorProvider) {

        super(I18n.tr("&View"));

        this.hideTransferTrayTrayActionProvider = hideTransferTrayTrayActionProvider;
        this.showDownloadsTrayActionProvider = showHideDownloadTrayAction;
        this.showUploadsTrayActionProvider = uploadTrayActionProvider;
        this.changeLanguageActionProvider = changeLanguageActionProvider;
        this.transferTrayNavigator = transferTrayNavigator;
        this.homeMediatorProvider = homeMediatorProvider;
        this.navigatorProvider = navigatorProvider;
    }

    @Override
    public void createMenuItems() {
        JCheckBoxMenuItem hideTransferTray = new JCheckBoxMenuItem(
                hideTransferTrayTrayActionProvider.get());
        JCheckBoxMenuItem showDownloads = new JCheckBoxMenuItem(showDownloadsTrayActionProvider
                .get());
        JCheckBoxMenuItem showUploads = new JCheckBoxMenuItem(showUploadsTrayActionProvider.get());

        boolean showTransfers = transferTrayNavigator.get().isTrayShowing();
        
        hideTransferTray.setSelected(!showTransfers);
        showDownloads.setSelected(showTransfers
                && transferTrayNavigator.get().isDownloadsSelected());
        showUploads.setSelected(showTransfers && transferTrayNavigator.get().isUploadsSelected());

        ButtonGroup group = new ButtonGroup();
        group.add(hideTransferTray);
        group.add(showDownloads);
        group.add(showUploads);
        
        add(buildShowHomeScreenAction());
        add(hideTransferTray);
        add(showDownloads);
        add(showUploads);
        addSeparator();
        add(changeLanguageActionProvider.get());
    }

    private Action buildShowHomeScreenAction(){
        return new AbstractAction(I18n.tr("&Home Screen")) {
            @Override
           public void actionPerformed(ActionEvent e) {
                navigatorProvider.get().getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                homeMediatorProvider.get().getComponent().loadDefaultUrl();
           }
        };
    }
}
