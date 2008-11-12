package org.limewire.ui.swing.sharing;

import javax.swing.JPanel;

import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel {

    private final SectionHeading title;
//
//    @Resource
//    private Color topButtonSelectionGradient;
//
//    @Resource
//    private Color bottomButtonSelectionGradient;

//    private final ShareButton gnutellaButton;
//
//    private final ShareButton friendButton;

    @Inject
    FilesSharingSummaryPanel(){//final ShareListManager shareListManager,
//            GnutellaSharePanel gnutellaSharePanel, FriendSharePanel friendSharePanel,
//            final Navigator navigator, ListenerSupport<FriendShareListEvent> shareSupport) {
        GuiUtils.assignResources(this);

        setOpaque(false);
        title = new SectionHeading(I18n.tr("Files I'm Sharing"));
        title.setName("FilesSharingSummaryPanel.title");

//        NavItem gnutellaNav = navigator.createNavItem(NavCategory.SHARING, GnutellaSharePanel.NAME,
//                gnutellaSharePanel);
//        gnutellaButton = new ShareButton(NavigatorUtils.getNavAction(gnutellaNav));
//        gnutellaButton.setName("FilesSharingSummaryPanel.gnutella");
//        gnutellaButton.setText("0");
//        gnutellaButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
//        gnutellaButton.setTransferHandler(new ShareButtonTransferHandler(navigator, gnutellaButton,
//                GnutellaSharePanel.NAME) {
//            private final SharingTransferHandler sharingTransferHandler = new SharingTransferHandler(
//                    shareListManager.getGnutellaShareList());
//
//            @Override
//            public boolean canImport(TransferSupport support) {
//                super.canImport(support);
//                return sharingTransferHandler.canImport(support);
//            }
//
//            @Override
//            public boolean importData(TransferSupport support) {
//                return sharingTransferHandler.importData(support);
//            }
//        });

//        NavItem friendNav = navigator.createNavItem(NavCategory.SHARING, FriendSharePanel.NAME,
//                friendSharePanel);
//        friendButton = new ShareButton(NavigatorUtils.getNavAction(friendNav));
//        friendButton.setName("FilesSharingSummaryPanel.friends");
//        friendButton.setText("0");
//        friendButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
//        friendButton.setTransferHandler(new ShareButtonTransferHandler(navigator, friendButton,
//                FriendSharePanel.NAME));
//
//        setLayout(new MigLayout("insets 0, gap 0", "", ""));
//
//        add(title, "span, wrap");
//        add(gnutellaButton, "alignx left");
//        add(friendButton, "alignx right");
//
//        shareListManager.getGnutellaShareList().getSwingModel().addListEventListener(
//                new ListEventListener<LocalFileItem>() {
//                    @Override
//                    public void listChanged(ListEvent<LocalFileItem> listChanges) {
//                        gnutellaButton.setText(GuiUtils.toLocalizedInteger(listChanges
//                                .getSourceList().size()));
//                    }
//                });
//
//        shareListManager.getCombinedFriendShareLists().getSwingModel().addListEventListener(
//                new ListEventListener<LocalFileItem>() {
//                    @Override
//                    public void listChanged(ListEvent<LocalFileItem> listChanges) {
//                        friendButton.setText(GuiUtils.toLocalizedInteger(listChanges
//                                .getSourceList().size()));
//                    }
//                });
    }

//    /**
//     * When an item is dragged onto the button, the button flashes while waiting
//     * for 750ms. If the mouse is still over the component at the end of the
//     * time then the navigator switches the view to the given Sharing Panel
//     * named by the panelName parameter.
//     */
//    private class ShareButtonTransferHandler extends TransferHandler {
//        private final Navigator navigator;
//
//        private Timer timer = null;
//
//        private Timer flashTimer = null;
//
//        private ShareButton shareButton = null;
//
//        private String panelName = null;
//
//        private ShareButtonTransferHandler(Navigator navigator, ShareButton shareButton,
//                String panelName) {
//            this.navigator = navigator;
//            this.shareButton = shareButton;
//            this.panelName = panelName;
//        }
//
//        @Override
//        public boolean canImport(TransferSupport support) {
//
//            if (timer == null || !timer.isRunning()) {
//                timer = new ComponentHoverTimer(750, new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
////                        NavItem navItem = navigator.getNavItem(NavCategory.SHARING, panelName);
////                        navItem.select();
//                    }
//                }, shareButton);
//
//                timer.setRepeats(false);
//                timer.start();
//            }
//
//            if (flashTimer == null || !flashTimer.isRunning()) {
//                flashTimer = new FlashTimer(250, navigator, shareButton);
//                flashTimer.setInitialDelay(250);
//                flashTimer.start();
//            }
//            return super.canImport(support);
//        }
//    }

//    /**
//     * A button that uses a painter to draw the background if its
//     * Action.SELECTED_KEY property is true.
//     */
//    private static class ShareButton extends IconButton {
//
//        private Boolean flash = Boolean.FALSE;
//
//        public ShareButton(Action navAction) {
//            super(navAction);
//        }
//
//        public void setFlash(Boolean flash) {
//            this.flash = flash;
//            repaint();
//        }
//
//        public void setGradients(Color topGradient, Color bottomGradient) {
//            getAction().addPropertyChangeListener(new PropertyChangeListener() {
//                @Override
//                public void propertyChange(PropertyChangeEvent evt) {
//                    if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
//                        repaint();
//                    }
//                }
//            });
//
//            final Painter<JXButton> oldPainter = getBackgroundPainter();
//            setBackgroundPainter(new MattePainter<JXButton>(new GradientPaint(new Point2D.Double(0,
//                    0), topGradient, new Point2D.Double(0, 1), bottomGradient, false), true) {
//                @Override
//                public void doPaint(Graphics2D g, JXButton component, int width, int height) {
//                    // while flashing we simulate the button being selected.
//                    if (Boolean.TRUE.equals(flash)
//                            || Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
//                        super.doPaint(g, component, width, height);
//                    } else {
//                        oldPainter.paint(g, component, width, height);
//                    }
//                }
//            });
//        }
//    }

//    /**
//     * While the mouse is hovering over the component is highlighted then the
//     * component will flash 3 times to signal that something will soon happen
//     * with the component.
//     */
//    private final class FlashTimer extends Timer {
//
//        private FlashTimer(int delay, final Navigator navigator, final ShareButton shareButton) {
//            super(delay, null);
//            final FlashTimer flashTimer = this;
//            shareButton.setFlash(Boolean.TRUE);
//            addActionListener(new ActionListener() {
//                int count = 0;
//
//                public void actionPerformed(ActionEvent e) {
//                    Point point = MouseInfo.getPointerInfo().getLocation();
//                    SwingUtilities.convertPointFromScreen(point, shareButton);
//                    if (shareButton.contains(point)) {
//                        if (count % 2 == 1) {
//                            shareButton.setFlash(Boolean.TRUE);
//                        } else {
//                            shareButton.setFlash(Boolean.FALSE);
//                        }
//                    } else {
//                        //mouse no longer on the component, unset the flash
//                        shareButton.setFlash(Boolean.FALSE);
//                    }
//                    if (count == 2) {
//                        flashTimer.stop();
//                    }
//                    count++;
//                }
//            });
//        }
//    }

//    /**
//     * Helper class that given a component will execute the given listener if
//     * the mouse is hovering over the component at the time the timer fires.
//     */
//    private class ComponentHoverTimer extends Timer {
//        public ComponentHoverTimer(int delay, final ActionListener listener,
//                final Component component) {
//            super(delay, new ActionListener() {
//
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    Point point = MouseInfo.getPointerInfo().getLocation();
//                    SwingUtilities.convertPointFromScreen(point, component);
//                    if (component.contains(point)) {
//                        listener.actionPerformed(e);
//                    }
//                }
//
//            });
//        }
//    }
}
