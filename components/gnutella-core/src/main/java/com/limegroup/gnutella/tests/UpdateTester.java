/**
 * file: UpdateTester.java
 * auth: afisk
 * desc: This class constructs a small gui to test updates.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella.tests;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import com.limegroup.gnutella.update.*;
import com.limegroup.gnutella.util.CommonUtils;

public class UpdateTester extends JFrame {

	private UpdateManager _manager;

	private JComboBox _versionBox;

	private JComboBox _osBox;

	public UpdateTester() {
		super("Update Tester");
		_manager = UpdateManager.instance();
		Dimension dim = new Dimension(400, 200);
		setSize(dim);
		JPanel mainPanel = new JPanel();
		JPanel boxPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.X_AXIS));
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		String[] versionArray = {"1.1", "1.2","1.2b","1.3","1.4","1.4b"}; 
		String[] osArray = {"Windows NT", "Windows 95", "Windows 98", "Mac OS",
							"Mac OS X", "Linux", "Solaris", "Other"};

		_versionBox = new JComboBox(versionArray);
		_osBox = new JComboBox(osArray);

		JButton startTestButton = new JButton("Start Test");
		startTestButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startTest();
			}
		});

		buttonPanel.add(startTestButton);
		boxPanel.add(_versionBox);
		boxPanel.add(Box.createHorizontalStrut(6));
		boxPanel.add(_osBox);
		
		mainPanel.add(boxPanel);
		mainPanel.add(Box.createVerticalStrut(6));
		mainPanel.add(buttonPanel);
		getContentPane().add(mainPanel);
	}

	/**
	 * starts the test.
	 */
	private void startTest() {
		String version = (String)_versionBox.getSelectedItem();
		String os = (String)_osBox.getSelectedItem();
		_manager.startTest(version,os);
	}

	/**
	 * main method to kick off the test.
	 */
	public static void main(String args[]) {
		CommonUtils.initialize();
		UpdateTester tester = new UpdateTester();
		tester.setVisible(true);
	}
}
