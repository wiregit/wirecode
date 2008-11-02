package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertiableHeadings;

abstract class AbstractFileItemDialog extends Dialog {
    private final PropertiableHeadings propertiableHeadings;
    private final MagnetLinkFactory magnetLinkFactory;
    
    public AbstractFileItemDialog(PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory) {
        this.propertiableHeadings = propertiableHeadings;
        this.magnetLinkFactory = magnetLinkFactory;
    }

    protected void populateCommonFields(final FileItem propertiable) {
        heading.setText(propertiableHeadings.getHeading(propertiable));
        filename.setText(propertiable.getFileName());
        subheading.setText(propertiableHeadings.getSubHeading(propertiable));
        fileSize.setText(str(propertiable.getProperty(FilePropertyKey.FILE_SIZE)));
        genre.setModel(new DefaultComboBoxModel(new Object[]{ propertiable.getProperty(FilePropertyKey.GENRE) }));
        rating.setModel(new DefaultComboBoxModel(new Object[]{ propertiable.getProperty(FilePropertyKey.RATING) }));
        platform.setModel(new DefaultComboBoxModel(new Object[]{ propertiable.getProperty(FilePropertyKey.PLATFORM) }));
        populateMetadata(propertiable);
        copyToClipboard.setAction(new AbstractAction(I18n.tr("Copy Link to clipboard")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MagnetLinkCopier().copyLinkToClipBoard(propertiable, magnetLinkFactory);
            }
        });
        title.setText(str(propertiable.getProperty(FilePropertyKey.TITLE)));
        artist.setText(str(propertiable.getProperty(FilePropertyKey.AUTHOR)));
        author.setText(str(propertiable.getProperty(FilePropertyKey.AUTHOR)));
        company.setText(str(propertiable.getProperty(FilePropertyKey.COMPANY)));
        //TODO - Which FilePropertyKey to use for 'album' field?
        year.setText(str(propertiable.getProperty(FilePropertyKey.YEAR)));
        track.setText(str(propertiable.getProperty(FilePropertyKey.TRACK_NUMBER)));
        description.setText(str(propertiable.getProperty(FilePropertyKey.COMMENTS)));
    }
}
