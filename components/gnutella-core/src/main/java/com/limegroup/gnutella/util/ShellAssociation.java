package com.limegroup.gnutella.util;

import javax.swing.JOptionPane;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.settings.StartupSettings;

/**
 * Sets this program as the default viewer for magnet: links and .torrent files, and determines what program is currently set.
 */
public class ShellAssociation {

	/**
	 * Registers this program to open magnet: and .torrent, asking the user permission if necessary.
	 * Only acts if the Windows launcher is running us and settings allow the associations check.
	 * If magnet: or .torrent aren't taken, silently takes them for us.
	 * If another program has either, asks the user permission with a dialog box.
	 * 
	 * TODO: Put this in the right place, not here
	 */
	public static void takeAssociations() {

		// Only do something if we're on Windows, settings allow this check, and our custom launcher is running us
		if (!CommonUtils.isWindows() || !StartupSettings.CHECK_ASSOCIATION.getValue() || !isLauncher()) return;

		// If no program has registered magnet: or .torrent, grab them for us
		if (isMagnetAvailable()) setMagnet(true);
		if (isTorrentAvailable()) setTorrent(true);

		// If we have both associations, we're done
		if (isMagnetUs() && isTorrentUs()) return;

		// TODO: Replace text with messages from the message bundles

		// Ask the user permission to get the associations we don't have
		Object[] options = {"Yes", "No", "Options..."};
		int choice = JOptionPane.showOptionDialog(
				GUIMediator.getAppFrame(),
				"LimeWire is not currently set as your default program for magnet: links and .torrent files. Would you like LimeWire to open these links and files?",
				"LimeWire",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);

		// The user clicked "Yes"
		if (choice == 0) {

			// Take both associations
			setMagnet(true);
			setTorrent(true);

		// The user clicked "Options..."
		} else if (choice == 2) {

			// TODO: Open LimeWire Options with the Player pane showing
		}

		// Otherwise, choice is 1 No or -1 the X in the corner, do nothing
	}

	/**
	 * Determines if nothing is registered to open magnet: links.
	 * Only acts if our custom launcher is running us.
	 */
	public static boolean isMagnetAvailable() {
		if (!isLauncher()) return false; // Only do something if our custom launcher is running us
		return getProtocol(magnetExtension).equals(""); // See if there is no information in the Registry
	}

	/**
	 * Determines if nothing is registered to open .torrent files.
	 * Only acts if our custom launcher is running us.
	 */
	public static boolean isTorrentAvailable() {
		if (!isLauncher()) return false; // Only do something if our custom launcher is running us
		return getFileType(torrentExtension).equals(""); // See if there is no information in the Registry
	}

	/**
	 * Determines if we are registered to open magnet: links.
	 * Only acts if our custom launcher is running us.
	 */
	public static boolean isMagnetUs() {
		if (!isLauncher()) return false; // Only do something if our custom launcher is running us
		return getProtocol(magnetExtension).equals(running); // See if the path in the Registry matches us running now
	}

	/**
	 * Determines if we are registered to open .torrent files.
	 * Only acts if our custom launcher is running us.
	 */
	public static boolean isTorrentUs() {
		if (!isLauncher()) return false; // Only do something if our custom launcher is running us
		return getFileType(torrentExtension).equals(running); // See if the path in the Registry matches us running now
	}

	/**
	 * Adds or removes us as the program that will open magnet: links.
	 * Only acts if our custom launcher is running us.
	 * Only adds our registration if we don't already have it, and removes it if we do.
	 * 
	 * @param add True to add us, false to remove us
	 */
	public static void setMagnet(boolean add) {
		if (!isLauncher()) return; // Only do something if our custom launcher is running us
		if (add && !isMagnetUs()) { // The caller requested we take magnet: links, and we don't already have them
			addProtocol(magnetExtension, magnetName, magnetType);
		} else if (!add && isMagnetUs()) { // The caller requested we remove our registration of magnet: links, and we currently have it
			removeProtocol(magnetExtension);
		}
	}

	/**
	 * Adds or removes us as the program that will open .torrent files.
	 * Only acts if our custom launcher is running us.
	 * Only adds our registration if we don't already have it, and removes it if we do.
	 * 
	 * @param add True to add us, false to remove us
	 */
	public static void setTorrent(boolean add) {
		if (!isLauncher()) return; // Only do something if our custom launcher is running us
		if (add && !isTorrentUs()) { // The caller requested we take .torrent files, and we don't already have them
			addFileType(torrentExtension, torrentName, torrentType);
		} else if (!add && isTorrentUs()) { // The caller requested we remove our registration of .torrent files, and we currently have it
			removeFileType(torrentExtension, torrentName);
		}
	}

	/** The name of this program, "LimeWire". */
	private static final String program = "LimeWire";
	/** The path to the native executable that is us running right now, like "C:\Program Files\LimeWire\LimeWire.exe". */
	private static final String running = SystemUtils.getRunningPath(); // Native code tells us this path

	/**
	 * Determines if this is the custom launcher running us right now.
	 * 
	 * @return True if we're running from our custom launcher, like LimeWire.exe.
	 *         False if it's a Java launcher running us.
	 */
	private static boolean isLauncher() {
		return running.contains(program + ".exe");
	}

	/** The extension for magnet: links, "magnet", without punctuation. */
	private static final String magnetExtension = "magnet";
	/** The name of the magnet: link protocol, "Magnet Protocol". */
	private static final String magnetName = "Magnet Protocol";
	/** The hash type magnet: links use, "urn:sha1". */
	private static final String magnetType = "urn:sha1";

	/** The extension for .torrent files, "torrent", without puncutation. */
	private static final String torrentExtension = "torrent";
	/** The name of the .torrent file format, "BitTorrent". */
	private static final String torrentName = "BitTorrent";
	/** Text that describes the type of a .torrent file, "BitTorrent File", this text will appear in the Type column in Windows Explorer. */
	private static final String torrentType = "BitTorrent File";

	/**
	 * Registers this program to open a custom kind of Web link, like magnet: links.
	 * 
	 * @param extension The protocol without punctuation, like "magnet"
	 * @param name      The common name, like "Magnet Protocol"
	 * @param type      The hash type, like "urn:sha1"
	 */
	private static void addProtocol(String extension, String name, String type) {

		// Wipe away any additional keys or values another program put where we will write
		removeProtocol(extension);

		/*
		 * Create the following Registry keys and values:
		 * 
		 * Root                Key           Variable      Value
		 * ------------------  ------------  ------------  ---------------------------------------------
		 * HKEY_CLASSES_ROOT   magnet                      URL:Magnet Protocol
		 *                                   URL Protocol
		 *                      DefaultIcon                "C:\Program Files\LimeWire\LimeWire.exe",0
		 *                      shell
		 *                       open
		 *                        command                  "C:\Program Files\LimeWire\LimeWire.exe" "%L"
		 */
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", extension,                            "",             "URL:" + name);
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", extension,                            "URL Protocol", "");
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", extension + "\\DefaultIcon",          "",             "\"" + running + "\",0");
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", extension + "\\shell\\open\\command", "",             "\"" + running + "\" \"%L\"");

		// For magnet links, also register us as a software handler
		if (extension.equals("magnet")) {

			/*
			 * Create the following Registry keys and values:
			 * 
			 * Root                Key          Variable      Value
			 * ------------------  -----------  ------------  ---------------------------------------------
			 * HKEY_LOCAL_MACHINE  SOFTWARE
			 *                      Magnet
			 *                       Handlers
			 *                        LimeWire                LimeWire
			 *                                  DefaultIcon   "C:\Program Files\LimeWire\LimeWire.exe",0
			 *                                  Description   LimeWire
			 *                                  kt            0
			 *                                  ShellExecute  "C:\Program Files\LimeWire\LimeWire.exe" "%URL"
			 *                         Type
			 *                                  urn:sha1      0
			 */
			SystemUtils.registryWriteText  ("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program,            "",             program);
			SystemUtils.registryWriteText  ("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program,            "DefaultIcon",  "\"" + running + "\",0");
			SystemUtils.registryWriteText  ("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program,            "Description",  program);
			SystemUtils.registryWriteNumber("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program,            "kt",           0);
			SystemUtils.registryWriteText  ("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program,            "ShellExecute", "\"" + running + "\" \"%URL\"");
			SystemUtils.registryWriteNumber("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program + "\\Type", "urn:sha1",     0);
		}
	}

	/**
	 * Registers this program to open a kind of file, like .torrent files.
	 * 
	 * @param extension The file name extension without puncutation, like "torrent" for .torrent files
	 * @param name      The common name of the application type, like "BitTorrent"
	 * @param type      Type text that will appear in the shell, like "BitTorrent File"
	 */
	private static void addFileType(String extension, String name, String type) {

		// Wipe away any additional keys or values another program put where we will write
		removeFileType(extension, name);

		/*
		 * Create the following Registry keys and values:
		 * 
		 * Root               Key                  Variable  Value
		 * -----------------  -------------------  --------  ---------------------------------------------
		 * HKEY_CLASSES_ROOT  .torrent                       LimeWire.BitTorrent
		 *                    LimeWire.BitTorrent            BitTorrent File
		 *                     DefaultIcon                   "C:\Program Files\LimeWire\LimeWire.exe",0
		 *                     shell
		 *                      open
		 *                       command                     "C:\Program Files\LimeWire\LimeWire.exe" "%L"
		 */
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", "." + extension,                                 "", program + "." + name);
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", program + "." + name,                            "", type);
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", program + "." + name + "\\DefaultIcon",          "", "\"" + running + "\",0");
		SystemUtils.registryWriteText("HKEY_CLASSES_ROOT", program + "." + name + "\\shell\\open\\command", "", "\"" + running + "\" \"%L\"");
	}

	/**
	 * Removes a custom kind of Web link registration, like magnet: links.
	 * 
	 * @param extension The protocol without punctuation, like "magnet"
	 */
	private static void removeProtocol(String extension) {
		SystemUtils.registryDelete("HKEY_CLASSES_ROOT", extension);
		// For magnet links, also remove our registration as a software handler
		if (extension.equals(magnetExtension))
			SystemUtils.registryDelete("HKEY_LOCAL_MACHINE", "SOFTWARE\\Magnet\\Handlers\\" + program);
	}

	/**
	 * Removes a file type registration, like for .torrent files.
	 * 
	 * @param extension The file name extension without puncutation, like "torrent" for .torrent files
	 * @param name      The common name of the application type, like "BitTorrent"
	 */
	private static void removeFileType(String extension, String name) {
		SystemUtils.registryDelete("HKEY_CLASSES_ROOT", "." + extension);
		SystemUtils.registryDelete("HKEY_CLASSES_ROOT", program + "." + name);
	}

	/**
	 * Gets the path to the program that opens a custom kind of Web links, like magnet: links.
	 * 
	 * @param extension The protocol without punctuation, like "magnet"
	 * @return          The complete path to the program, or blank if no program is registered
	 */
	private static String getProtocol(String extension) {
		String command = SystemUtils.registryReadText("HKEY_CLASSES_ROOT", extension + "\\shell\\open\\command", "");
		return parsePath(command);
	}

	/**
	 * Gets the path to the program that opens a kind of file, like .torrent files.
	 * 
	 * @param extension The file name extension without puncutation, like "torrent" for .torrent files
	 * @return          The complete path to the program, or blank if no program is registered
	 */
	private static String getFileType(String extension) {
		String name = SystemUtils.registryReadText("HKEY_CLASSES_ROOT", "." + extension, "");
		if (name.equals("")) return "";
		String command = SystemUtils.registryReadText("HKEY_CLASSES_ROOT", name + "\\shell\\open\\command", "");
		return parsePath(command);
	}
	
	/**
	 * Parses the path from a Windows Registry value.
	 * 
	 * Registry values look like this:
	 * 
	 * <pre>
	 * "C:\Program Files\Program\Program.exe" "%1"
	 * C:\PROGRA~1\Program\Program.exe %1
	 * </pre>
	 * 
	 * Additional information comes after the path.
	 * If the path at the start contains spaces, it will be in quotes.
	 * This method gets it either way.
	 * 
	 * @param value A text value from the Windows Registry that contains a path
	 * @return      The path
	 */
	private static String parsePath(String value) {
		int begin, end;
		if (value.startsWith("\"")) {
			begin = 1;
			end = value.indexOf("\"", begin);
		} else {
			begin = 0;
			end = value.indexOf(" ");
		}
		String path = "";
		try {
			path = value.substring(begin, end);
		} catch (IndexOutOfBoundsException e) {}
		return path;
	}
}
