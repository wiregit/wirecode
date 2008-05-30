package org.jivesoftware.smackx.provider.file;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.xmlpull.v1.XmlPullParser;

public class FileProvider implements IQProvider {

	public IQ parseIQ(final XmlPullParser parser) throws Exception {
		boolean done = false;

		StreamInitiation initiation = new StreamInitiation();

		// file
		String name = null;
		String size = null;
		String hash = null;
		String date = null;
		String desc = null;
		boolean isRanged = false;

		int eventType;
		String elementName;
		while (!done) {
			eventType = parser.next();
			elementName = parser.getName();
			if (eventType == XmlPullParser.START_TAG) {
				if (elementName.equals("file")) {
					name = parser.getAttributeValue("", "name");
					size = parser.getAttributeValue("", "size");
					hash = parser.getAttributeValue("", "hash");
					date = parser.getAttributeValue("", "date");
				} else if (elementName.equals("desc")) {
					desc = parser.nextText();
				} else if (elementName.equals("range")) {
					isRanged = true;
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (elementName.equals("file")) {
                    done = true;
                    long fileSize = 0;
                    if(size != null && size.trim().length() !=0){
                        try {
                            fileSize = Long.parseLong(size);
                        }
                        catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    StreamInitiation.File file = new StreamInitiation.File(name, fileSize);
					file.setHash(hash);
					if (date != null)
						file.setDate(DelayInformation.UTC_FORMAT.parse(date));
					file.setDesc(desc);
					file.setRanged(isRanged);
					initiation.setFile(file);
				}
			}
		}

		return initiation;
	}

}