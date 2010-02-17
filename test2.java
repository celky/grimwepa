// command to save as JAR file:
// jar cvfm grimwepa.jar mymanifest *.class default_pw.txt

// GUI and events, oh my!
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

// basic in out and utilities
import java.io.*;
import java.util.*;

// extracting default_pw.txt from jar file...
import java.util.jar.*;
import java.util.zip.*;

public class test2 extends Frame implements WindowListener, ActionListener, EventListener, ListSelectionListener {
	public static JCheckBox  hideWin;
	// Drivers
	public static JLabel     iface;
	public static JComboBox  drivers;
	public static Button     buttonDrivers;
	
	// Targets list, refresh button, and timeout
	public JScrollPane       tscroll;
	public static JTable     targets;
	public static DefaultTableModel targetsm;
	
	// Channel slider
	public static JLabel     chlabel;
	public static JCheckBox  chkchan;
	public static JSlider    chslider;
	
	// Refresh & timeout components 
	public static Button     buttonTargets;
	public static JLabel     ltimeout;
	public static JTextField timeout;
	
	// WEP panel
	public static JPanel     wepanel;
	public static JLabel     weplattack;
	public static JComboBox  wepattack;
	public static JCheckBox  chkclient;
	public static JComboBox  clients;
	public static Button     buttonStart;
	public static JLabel     wepivs;
	public static Button     buttonCrack;
	public static Button     buttonTest;
	public static Button     buttonMAC;
	// WPA panel
	public static JPanel     wpanel;
	public static Button     wpacrack;
	public static JTextField wpawordlist;
	public static JCheckBox  wpachkclients;
	public static JComboBox  wpaclients;
	public static Button     wpadeauth;
	public static JLabel     wpaltimeout;
	public static JTextField wpatimeout;
	
	// Status bar
	public static JLabel     status;
	
	public static Process pro1 = null; // pro1 is used by targetClass.java
				// pro1 is the process runs airodump in the background
	
	public static Process procrack = null; // procrack is used by wpacracker.java
			// procrack is the process that runs aircrack-ng in the background
	
	public static Process prowep = null; // process used for airodump with wep cracker
	
	public static Process proinj = null; // process for injection test
	
	public targetClass tc;  // thread to gather targets
	
	public static test2 myWindow = new test2("GRIM WEPA");
	
	public static String currentBSSID;
	public static String currentChannel;
	public static String oldMAC = "";
	
	public static void main(String[] args) {
		// load window
		// test2 myWindow = new test2("GRIM WEPA");
		myWindow.setSize(450,390);
		
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension w = myWindow.getSize();
		
		myWindow.setLocation((int)Math.max(0, (ss.width - w.width ) / 2), Math.max(0, ss.height - w.height) /2);
		myWindow.setVisible(true);
		
		driverCheck();
	}
	public static void driverCheck() {
		// checks if any drivers are in monitor mode
		// if not, it enumerates the wireless drivers and asks which one to put into monitor mode!
		
		if (drivers.getItemCount() == 1) {
			// no devices are in monitor mode, prompt!!!
			Process proDrv = null;
			BufferedReader res1 = null;
			try {
				proDrv = Runtime.getRuntime().exec("airmon-ng");
				res1 = new BufferedReader(new InputStreamReader(proDrv.getInputStream()));
				proDrv.waitFor(); // wait for data
				
				String line, drvs = "", drvz[], drv = "";
				while ((line = res1.readLine()) != null) {
					if ((line.indexOf("Interface") < 0) && (line.equals("") == false)) {
						drvs += "," + line.substring(0, line.indexOf("\t"));
					}
				}
				if (drvs.equals("") == false) {
					// we have drivers to prompt
					drvs = drvs.substring(1); // get rid of leading comma
					drvz = drvs.split(",");   // put list of devices into array
					
					// prompt user
					String choice = (String)JOptionPane.showInputDialog(null, 
					   "Select which wireless device you want to put into monitor mode:", 
					   "GRIM WEPA | Select device for Monitor Mode",
					   JOptionPane.INFORMATION_MESSAGE, null, drvz, drvz[0]);
					
					if (choice.equals("null") == false) {
						// if they didn't click cancel
						// put that device into monitor mode!
						stat("Putting device into monitor mode...");
						drivers.removeAllItems();
						drivers.addItem("WiFi Device:");
						proDrv = Runtime.getRuntime().exec("airmon-ng start " + choice);
						res1 = new BufferedReader(new InputStreamReader(proDrv.getInputStream()));
						proDrv.waitFor(); // wait for it to go
						while ((line = res1.readLine()) != null) {
							if (line.indexOf("monitor mode enabled") >= 0) {
								line.replaceAll("\t", "");
								stat(choice + ": " + line);
								if (line.indexOf("enabled on") >= 0) {
									choice = line.substring(line.indexOf(
									"enabled on") + 11, line.length() - 1);
								}
							} else if ((line.indexOf("Interface") < 0) 
								  && (line.indexOf("\t") >= 0)) {
								// add driver to list
								drv = line.substring(0, line.indexOf("\t"));
								if (isValidDriver(drv) == true)
									drivers.addItem(drv);
							}
						}
						// addDriversToList();
						int tempi = -1;
						for (tempi = 0; tempi < drivers.getItemCount(); tempi++) {
							if (choice.equals((String)drivers.getItemAt(tempi)) == true) {
								tempi = -1;
								break;
							}
						}
						if (tempi != -1) 
							drivers.addItem(choice);
						drivers.setSelectedItem(choice);		
					}
				}
			} catch (IOException ioe) {} catch (InterruptedException iex) {}
		}
	}
	public test2(String title) {
		super(title);
		setResizable(false);
		Font f = new Font("Default", Font.BOLD, 11);
		
		// interface & drivers components
		super.setBackground(Color.black);
		super.setForeground(Color.green);
		setLayout(new FlowLayout());
		addWindowListener(this);
		
		iface = new JLabel("Interface:");
		iface.setPreferredSize(new Dimension(65, 18));
		iface.setForeground(Color.green);
		add(iface);
		
		drivers  = new JComboBox();
		drivers.setBackground(Color.black);
		drivers.setForeground(Color.green);
		add(drivers);
		drivers.addActionListener(this);
		
		try {
			addDriversToList(); // add drivers on load
		} catch (IOException e) {}
		
		buttonDrivers = new Button("Refresh drivers");
		buttonDrivers.setFont(f);
		add(buttonDrivers);
		buttonDrivers.addActionListener(this);
		
		doLayout();
		
		// Targets list commands
		targetsm = new DefaultTableModel(null, new String[] 
							{"Network Name","Channel","Encryption","BSSID"});
		
		targets = new JTable(targetsm) { 
					public boolean isCellEditable(int rowIndex, int vColIndex) { 
						return false; 
					} };
		
		targets.setBackground(Color.black);
		targets.setForeground(Color.green);
		targets.setPreferredScrollableViewportSize(new Dimension(420,100));
		targets.setFillsViewportHeight(true);
		
		targets.getColumnModel().getColumn(0).setPreferredWidth(100);
		targets.getColumnModel().getColumn(1).setPreferredWidth(25);
		targets.getColumnModel().getColumn(2).setPreferredWidth(50);
		targets.getColumnModel().getColumn(3).setPreferredWidth(80);
		targets.setFont(f);
		
		targets.getSelectionModel().addListSelectionListener(this);
		
		tscroll = new JScrollPane(targets);
		add(tscroll);
		
		targets.doLayout();
		
		// Channel slider & options
		chlabel = new JLabel("Channel:");
		chlabel.setForeground(Color.green);
		add(chlabel);
		
		chslider = new JSlider(0, 1, 14, 6);
		chslider.setBackground(Color.black);
		chslider.setForeground(Color.green);
		chslider.setMajorTickSpacing(1);
		chslider.setMinorTickSpacing(1);
		chslider.setPaintTicks(true);
		chslider.setPaintLabels(true);
		chslider.setPaintTrack(false);
		chslider.setSnapToTicks(true);
		chslider.setFont(new Font("Serif", Font.BOLD, 10));
		chslider.setPreferredSize(new Dimension(250, 35));
		// chslider.setBorder(BorderFactory.createLineBorder(Color.green));
		add(chslider);
		
		chkchan = new JCheckBox("All Channels");
		chkchan.setBackground(Color.black);
		chkchan.setForeground(Color.green);
		add(chkchan);
		chkchan.addActionListener(this);
		
		
		hideWin = new JCheckBox("Hide XTerms");
		hideWin.setForeground(Color.green);
		hideWin.setBackground(Color.black);
		hideWin.setPreferredSize(new Dimension(135,18));
		add(hideWin);
		
		// Refresh button & timeout
		buttonTargets = new Button("Refresh Targets");
		buttonTargets.setFont(f);
		add(buttonTargets);
		buttonTargets.addActionListener(this);
		
		/*ltimeout = new JLabel("      Timeout (in seconds):");
		ltimeout.setPreferredSize(new Dimension(170, 18));
		ltimeout.setForeground(Color.green);
		add(ltimeout);
		*/
		ltimeout = new JLabel("Timeout (sec):");
		ltimeout.setPreferredSize(new Dimension(90, 18));
		ltimeout.setForeground(Color.green);
		add(ltimeout);
		timeout = new JTextField("5");
		timeout.setPreferredSize(new Dimension(30, 20));
		timeout.setHorizontalAlignment(JTextField.CENTER);
		timeout.setBackground(Color.black);
		timeout.setForeground(Color.green);
		add(timeout);
		
		// WEP PANEL
		wepanel = new JPanel();
		wepanel.setPreferredSize(new Dimension(430,110));
		
		weplattack = new JLabel("Attack method:");
		weplattack.setForeground(Color.green);
		wepanel.add(weplattack);
		
		wepattack = new JComboBox();
		
		wepattack.addItem("Select attack:"); // 0
		wepattack.addItem("ARP-Replay");        // 1
		wepattack.addItem("Chop-Chop");         // 2
		wepattack.addItem("Fragmentation");     // 3
		wepattack.addItem("Caffe-Latte");       // 4
		wepattack.addItem("p0841 Attack");      // 5
		
		// wepattack.setPreferredSize(new Dimension(200, 20));
		wepattack.setBackground(Color.black);
		wepattack.setForeground(Color.green);
		wepanel.add(wepattack);
		wepattack.addActionListener(this);
		
		buttonTest = new Button("Test Injection");
		buttonTest.setFont(f);
		wepanel.add(buttonTest);
		buttonTest.addActionListener(this);
		
		chkclient = new JCheckBox("Choose client:", false);
		// chkclient.setPreferredSize(new Dimension(200, 18));
		chkclient.setBackground(Color.black);
		chkclient.setForeground(Color.green);
		wepanel.add(chkclient);
		chkclient.addActionListener(this);
		
		clients = new JComboBox();
		clients.setBackground(Color.black);
		clients.setForeground(Color.green);
		clients.addItem("[no clients found]");
		clients.setPreferredSize(new Dimension(150, 20));
		clients.setEnabled(false);
		wepanel.add(clients);
		
		buttonMAC = new Button("Change MAC");
		buttonMAC.setFont(f);
		wepanel.add(buttonMAC);
		buttonMAC.addActionListener(this);
		
		buttonStart = new Button("Start Attack");
		buttonStart.setFont(f);
		wepanel.add(buttonStart);
		buttonStart.addActionListener(this);
		
		wepivs = new JLabel("IVs Captured: 0");
		wepivs.setPreferredSize(new Dimension(150, 18));
		wepivs.setForeground(Color.green);
		wepanel.add(wepivs);
		
		buttonCrack = new Button("Start Cracking");
		buttonCrack.setFont(f);
		wepanel.add(buttonCrack);
		wepanel.setBackground(Color.black);
		wepanel.setForeground(Color.green);
		buttonCrack.addActionListener(this);
		wepanel.setForeground(Color.green);
		wepanel.setBorder(BorderFactory.createTitledBorder(null, "WEP", 0, 0, null, Color.green));
		add(wepanel);
		// wepanel.setVisible(false);
		// END OF WEP PANEL
		
		// WPA PANEL
		wpanel = new JPanel();
		wpanel.setPreferredSize(new Dimension(430, 110));
		wpanel.setBackground(Color.black);
		wpanel.setForeground(Color.green);
		wpanel.setBorder(BorderFactory.createTitledBorder(null, "WPA", 0, 0, null, Color.green));
		
		wpachkclients = new JCheckBox("Use client in attack:");
		wpachkclients.setBackground(Color.black);
		wpachkclients.setForeground(Color.green);
		wpanel.add(wpachkclients);
		wpachkclients.addActionListener(this);
		
		wpaclients = new JComboBox();
		wpaclients.addItem("[no clients found]");
		wpaclients.setPreferredSize(new Dimension(150, 20));
		wpaclients.setBackground(Color.black);
		wpaclients.setForeground(Color.green);
		wpaclients.setEnabled(false);
		wpanel.add(wpaclients, BorderLayout.CENTER);
		
		wpadeauth = new Button("Start Deauth + Handshake Capture Attack");
		wpanel.add(wpadeauth);
		wpadeauth.addActionListener(this);
		
		wpaltimeout = new JLabel("Timeout (sec):");
		wpaltimeout.setForeground(Color.green);
		wpanel.add(wpaltimeout);
		
		wpatimeout = new JTextField("5");
		wpatimeout.setHorizontalAlignment(JTextField.CENTER);
		wpatimeout.setPreferredSize(new Dimension(30, 20));
		wpatimeout.setForeground(Color.green);
		wpatimeout.setBackground(Color.black);
		wpanel.add(wpatimeout);
		
		wpacrack = new Button("Crack WPA (Dictionary Attack)");
		wpanel.add(wpacrack);
		wpacrack.addActionListener(this);
		wpacrack.setEnabled(false);
		
		wpawordlist = new JTextField("[default wordlist]");
		wpawordlist.setForeground(Color.green);
		wpawordlist.setBackground(Color.black);
		wpawordlist.setPreferredSize(new Dimension(180, 20));
		wpanel.add(wpawordlist);
		wpawordlist.setEnabled(false);
		
		add(wpanel);
		doLayout();
		
		wpanel.setVisible(false);
		// END OF WPA PANEL
		
		status = new JLabel(" Status: [inactive]");
		status.setForeground(Color.green);
		status.setBorder(BorderFactory.createLineBorder(Color.green));
		status.setPreferredSize(new Dimension(430, 20));
		add(status);
		
		// add listener for targets table
		targets.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				
			}
		} );
		
		doLayout();
		
		wepanel.setEnabled(false);
		weplattack.setEnabled(false);
		wepattack.setEnabled(false);
		chkclient.setEnabled(false);
		clients.setEnabled(false);
		buttonStart.setEnabled(false);
		wepivs.setEnabled(false);
		buttonCrack.setEnabled(false);
		buttonTest.setEnabled(false);
		buttonMAC.setEnabled(false);
		loadSettings();
	} 
	
	public void loadSettings() {
		// loads preferences from a conf file in /etc/
		BufferedReader input = null;
		try {
			input =  new BufferedReader(new FileReader("/etc/grimwepa.conf"));
			String line = null;
			boolean showreadme = true;
			while (( line = input.readLine()) != null ) {
				if (line.length() >= 5) {
					if (line.substring(0, 5).equals("iface") == true) {
						String iface = line.substring(6);
						for (int i = 0; i < drivers.getItemCount(); i++) {
							if (iface.equals(drivers.getItemAt(i)) == true) {
								drivers.setSelectedIndex(i);
								break;
							}
						}
					} else if (line.substring(0, 5).equals("chann") == true) {
						chslider.setValue(Integer.parseInt(line.substring(8)));
					} else if (line.substring(0, 5).equals("allch") == true) {
						if (line.substring(8).equals("true") == true) {
							chkchan.setSelected(true);
							chslider.setEnabled(false);
						}
					} else if (line.substring(0, 5).equals("targe") == true) {
						timeout.setText(line.substring(14));
					} else if (line.substring(0, 5).equals("wpati") == true) {
						wpatimeout.setText(line.substring(11));
					} else if (line.substring(0, 5).equals("wepat") == true) {
						int wepind = Integer.parseInt(line.substring(10));
						wepattack.setSelectedIndex(wepind);
					} else if (line.substring(0, 5).equals("wpawo") == true) {
						wpawordlist.setText(line.substring(8));
					} else if (line.substring(0, 5).equals("shown") == true) {
						showreadme = false;
					} else if (line.substring(0, 5).equals("xterm") == true) {
						if (line.substring(6).equals("true") == true)
							hideWin.setSelected(true);
					}
				}
			}
			if (showreadme == true)
				generateReadme();
		} catch (IOException ioe) {
		} finally {
			try {
				input.close();
			} catch (IOException ioe) {} catch (NullPointerException npe) {}
		}
	}
	public void saveSettings() {
		// saves preferences to a conf file in /etc/
		Writer output = null;
		try {
			output = new BufferedWriter(new FileWriter("/etc/grimwepa.conf"));
			output.write("iface " + (String)drivers.getItemAt(drivers.getSelectedIndex()));
			output.write("\nchannel " + chslider.getValue());
			output.write("\nallchan " + chkchan.isSelected());
			output.write("\ntargettimeout " + timeout.getText());
			output.write("\nwpatimeout " + wpatimeout.getText());
			output.write("\nwepattack " + wepattack.getSelectedIndex());
			output.write("\nwpaword " + wpawordlist.getText());
			output.write("\nxterm " + hideWin.isSelected());
			output.write("\nshown");
		} catch (FileNotFoundException fnfe) {} catch (IOException ioe) {
		} finally {
			try {
				output.close();
			} catch (IOException ioe) {}
		}
	}
	
	public void valueChanged(ListSelectionEvent e) {
		// item selection in the targets list has changed
		int row = targets.getSelectedRow();
		if (row != -1) {
			String temp;
			temp = (String)targets.getValueAt(row, 2);
			if (temp.indexOf("WEP") >= 0) {
				wpanel.setVisible(false);
				wepanel.setVisible(true);
				//wepanel.setEnabled(true);
				/*
				weplattack.setEnabled(true);
				wepattack.setEnabled(true);
				chkclient.setEnabled(true);
				chkclient.setSelected(false);
				clients.setEnabled(false);
				buttonStart.setEnabled(true);
				wepivs.setEnabled(true);
				buttonCrack.setEnabled(true);
				*/
				wepanel.setBorder(BorderFactory.createTitledBorder(
					null, "WEP ; Target=" + (String)targets.getValueAt(row,3), 
					0, 0, null, Color.green));
				currentBSSID = (String)targets.getValueAt(row, 3);
				currentChannel = (String)targets.getValueAt(row, 1);
			} else if (temp.indexOf("WPA") >= 0) {
				//wpacrack.setEnabled(false);
				//wpawordlist.setEnabled(false);
				wepanel.setVisible(false);
				wpanel.setVisible(true);
				//wpanel.setEnabled(true);
				wpanel.setBorder(BorderFactory.createTitledBorder(
					null, "WPA ; Target=" + (String)targets.getValueAt(row,3), 
					0, 0, null, Color.green));
				currentBSSID = (String)targets.getValueAt(row,3);
				currentChannel = (String)targets.getValueAt(row, 1);
			}
		}
		test2.super.doLayout();
	}
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == buttonDrivers) {
			// refresh drivers button
			setEnable(false);
			try {
				addDriversToList();
			} catch (IOException err) {}
			setEnable(true);
		} else if ((event.getSource() == buttonTargets)) {
			// refresh targets button
			if (buttonTargets.getLabel().equals("Refresh Targets") == true) {
				String drv = (String)drivers.getItemAt(drivers.getSelectedIndex());
				if (drv.equals("WiFi Device:") == false) {
					addTargetsToList();
				} else
					JOptionPane.showMessageDialog(null, "Select a WiFi interface!\n" +
						"You may need to put your network card into Monitor Mode\n" +
						"i.e. airmon-ng start wlan0");
			} else {
				buttonTargets.setLabel("Refresh Targets");
				test2.setEnable(true);
			}
		} else if (event.getSource() == wepattack) {
			// combobox click / change
			
		} else if (event.getSource() == chkclient) {
			// WEP clients checkbox
			clients.setEnabled(chkclient.isSelected());
			buttonMAC.setEnabled(chkclient.isSelected());
		} else if (event.getSource() == chkchan) {
			// channel checkbox
			chslider.setEnabled(!chkchan.isSelected());
		} else if (event.getSource() == wpachkclients) {
			// wpa clients checkbox
			wpaclients.setEnabled(wpachkclients.isSelected());
		} else if (event.getSource() == wpadeauth) {
			// wpa attack
			wpaAttack();
		} else if (event.getSource() == wpacrack) {
			// wpa crack
			wpaCrack();
		} else if (event.getSource() == buttonStart) {
			// wep attack
			wepAttack();
		} else if (event.getSource() == buttonCrack) {
			// wep crack
			wepCrack();
		} else if (event.getSource() == buttonTest) {
			// test injection
			testInjection();
		} else if (event.getSource() == buttonMAC) {
			// change mac address
			changeMAC();
		}
	}
	public static void changeMAC() {
		String clientmac, thismac, iface, line;
		BufferedReader in = null;
		Process promac = null;
		// wifi card name
		iface = (String)drivers.getItemAt(drivers.getSelectedIndex());
		// current mac address
		thismac = getMac(iface);
		// client's mac address
		clientmac = (String)clients.getItemAt(clients.getSelectedIndex());
		
		// if we've already changed the mac once before...
		if (oldMAC.equals("") == false) {
			// change it back!
			stat("Changing MAC Addr of " + iface + " back to old MAC...");
			try {
				promac = Runtime.getRuntime().exec("ifconfig " + iface + " down");
				promac.waitFor();
				
				promac = Runtime.getRuntime().exec(
					"macchanger -m " + oldMAC + " " + iface);
				
				in = new BufferedReader(new InputStreamReader(promac.getInputStream()));
				promac.waitFor();
				
				while ((line = in.readLine()) != null) {
					if (line.indexOf("Faked MAC") >= 0) {
						stat("Changed MAC BACK to old address '" + oldMAC + "'");
						break;
					} else if (line.indexOf("Can't change MAC") >= 0) {
						stat("Error: Unable to change MAC address!");
						break;
					}
				}
				
				try {
					in.close();
				} catch(IOException ioe) {}
				
				promac = Runtime.getRuntime().exec("ifconfig " + iface + " up");
				promac.waitFor();
			} catch (IOException ioe) {} catch (InterruptedException iex) {}
			oldMAC = "";
		} else {
			if (clientmac.equals("[no clients found]") == true) {
				stat("Select a client to change MAC to!");
			} else {
				if (thismac.equals(clientmac) == true) {
					stat("Client MAC and " + iface + "'s MAC are Equal!");
				} else {
					stat("Changing MAC Addr of " + iface + " to '" + clientmac + "'...");
					try {
						promac = Runtime.getRuntime().exec("ifconfig " + iface + " down");
						promac.waitFor();
						
						promac = Runtime.getRuntime().exec(
							"macchanger -m " + clientmac + " " + iface);
						
						in = new BufferedReader(new InputStreamReader(promac.getInputStream()));
						promac.waitFor();
						
						while ((line = in.readLine()) != null) {
							if (line.indexOf("Faked MAC") >= 0) {
								stat(line);
								oldMAC = thismac;
								break;
							} else if (line.indexOf("Can't change MAC") >= 0) {
								stat("Error: Unable to change MAC address!");
								break;
							}
						}
						
						try {
							in.close();
						} catch(IOException ioe) {}
						
						promac = Runtime.getRuntime().exec("ifconfig " + iface + " up");
						promac.waitFor();
					} catch (IOException ioe) {} catch (InterruptedException iex) {}
				}
			}
		}
	}
	public static String getMac(String iface) {
		// finds macaddress of 'iface' device
		BufferedReader in = null;
		Process promac = null;
		String result = "", line;
		
		try {
			// start ifconfig on the interface, reading the info
			promac = Runtime.getRuntime().exec("ifconfig " + iface);
			in = new BufferedReader(new InputStreamReader(promac.getInputStream()));
			promac.waitFor();
			
			// go through everyline of ifconfig output
			while ((line = in.readLine()) != null) {
				if (line.indexOf("HWaddr ") >= 0) {
					result = line.substring(line.indexOf("HWaddr ") + 7, line.indexOf("HWaddr ") + 7 + 17);
					result = result.replace('-', ':');
					break;
				}
			}
			try {
				in.close();
			} catch(IOException ioe) {}
			
		} catch (IOException ie) {} catch (InterruptedException ie) {}
		
		return result;
	}
	public static void testInjection() {
		if (buttonTest.getLabel().equals("Test Injection") == true) {
			buttonTest.setLabel("Stop Inj. Test");
			try {
				// have to use airodump to set it on the right channel!
				String xterm = ""; //"xterm -fg green -bg black -geom 100x15+0+450 -e ";
				proinj = Runtime.getRuntime().exec(xterm + "" + 
					"airodump-ng -c " + currentChannel + " " + 
					(String)drivers.getItemAt(drivers.getSelectedIndex()));
			} catch (IOException ioe) {}
			
			injectionClass injt = new injectionClass();
			injt.t.start();
		} else {
			// buttonCrack.setLabel("Start Cracking");
			injectionClass.flag = true;
			proinj.destroy();
		}
	}
	public static void setEnable(boolean en) {
		// Drivers list
		drivers.setEnabled(en);
		buttonDrivers.setEnabled(en);
		
		// Targets list
		targets.setEnabled(en);
		
		// Refresh button & channels
		buttonTargets.setEnabled(en);
		chkchan.setEnabled(en);
		chslider.setEnabled(en);
		
		// WEP panel
		wepanel.setEnabled(en);
		wepattack.setEnabled(en); // combobox
		chkclient.setEnabled(en); // check
		clients.setEnabled(en);   // combo
		if (en == true && chkclient.isSelected() == false) {
			clients.setEnabled(false);
			buttonMAC.setEnabled(false);
		}
		buttonStart.setEnabled(en);
		buttonCrack.setEnabled(en);
		weplattack.setEnabled(en);
		wepivs.setEnabled(en);
		buttonTest.setEnabled(en);
		
		// WPA panel
		wpanel.setEnabled(en);
		// wpacrack.setEnabled(en);
		wpawordlist.setEnabled(en);   //textbox
		wpachkclients.setEnabled(en); //check
		wpaclients.setEnabled(en);    //combo
		if (en == true && wpachkclients.isSelected() == false)
			wpaclients.setEnabled(false);
		wpadeauth.setEnabled(en);     //button
		wpatimeout.setEnabled(en);    //text
		
		iface.setEnabled(en);
		chlabel.setEnabled(en);
		ltimeout.setEnabled(en);
		wpaltimeout.setEnabled(en);
		timeout.setEnabled(en);
	}
	public static void wepCrack() {
		// assuming ivs file is wep-01.ivs
		if (buttonCrack.getLabel().equals("Start Cracking") == true) {
			buttonCrack.setLabel("Stop Cracking");
			try {
				procrack = Runtime.getRuntime().exec("rm -rf wepcracked.txt");
				procrack.waitFor();
				procrack.destroy();
				
				stat("Cracking WEP...");
				String xterm;
				if (hideWin.isSelected() == true)
					xterm = "";
				else
					xterm = "xterm -fg green -bg black -geom 100x15+0+450 -iconic -e ";
				procrack = Runtime.getRuntime().exec(
					xterm + 
					"aircrack-ng -a 1 -b " + currentBSSID + " " + 
					"-l wepcracked.txt wep-01.ivs");
			} catch (IOException e) {} catch (InterruptedException ie) {}
			
			wepcracker wepc = new wepcracker();
			wepc.t.start();
		} else {
			// buttonCrack.setLabel("Start Cracking");
			wepcracker.flag = true;
		}
	}
	public void wepAttack() {
		if (buttonStart.getLabel().equals("Start Attack") == true) {
			// start the attack
			setEnable(false);
			buttonStart.setEnabled(true);
			buttonCrack.setEnabled(true);
			
			wepattack1 wa1 = new wepattack1();
			wa1.t.start();
			
			// capture!
			buttonStart.setLabel("Stop Attack");
			try {
				pro1 = Runtime.getRuntime().exec("rm -rf wep-01.ivs");
				pro1.waitFor();
				pro1.destroy();
				
				pro1 = Runtime.getRuntime().exec("rm -rf wep-01.csv");
				pro1.waitFor();
				pro1.destroy();
				
				String drv = (String)drivers.getItemAt(drivers.getSelectedIndex());
				String xterm;
				//if (hideWin.isSelected() == true)
				//	xterm = "";
				//else
					xterm = "xterm -fg green -bg black -geom 100x15+0+0 -iconic -e ";
				pro1 = Runtime.getRuntime().exec(
					xterm + 
					"airodump-ng -w wep --bssid " + currentBSSID + 
					" -c " + currentChannel + " --ivs --output-format csv " + drv);
			} catch (IOException e) {} catch (InterruptedException ex) {}
			
			// to turn off, set wepattack1.flag = true;
		} else {
			setEnable(true);
			
			buttonStart.setLabel("Start Attack");
			pro1.destroy();
			wepattack1.flag = true;
			try {
				// kill aireplay -- stuck on listening for packets!
				wepattack1.profrag.destroy(); 
			} catch(NullPointerException npe) {}
			String att = "";
			switch(wepattack.getSelectedIndex()) {
			case 1: att = "ARP-Replay";
				break;
			case 2: att = "Chop-Chop";
				break;
			case 3: att = "Fragmentation";
				break;
			case 4: att = "Cafe-Latte";
				break;
			case 5: att = "p0841";
				break;
			}
			stat(att + " Attack Stopped");
		}
	}
	public void wpaCrack() {
		// assuming handshake is in cap file: wpa-01.cap
		if (wpacrack.getLabel().equals("Crack WPA (Dictionary Attack)")) {
			wpacrack.setLabel("Stop Cracker");
			try {
				procrack = Runtime.getRuntime().exec("rm -rf wpacracked.txt");
				procrack.waitFor();
				procrack.destroy();
				
				String wordlist = wpawordlist.getText();
				if ( (new File(wordlist)).exists() == false) {
					// wordlist doesnt' exits, use this default
					wordlist = "default_pw.txt";
					if ( (new File(wordlist)).exists() == false)
						generatePasswords();
					wpawordlist.setText("default_pw.txt");
					stat("Cracking WPA, using default wordlist");
				} else
					stat("Cracking WPA, using '" + wordlist + "'");
				
				String xterm;
				if (hideWin.isSelected() == true)
					xterm = "";
				else
					xterm = "xterm -fg green -bg black -geom 80x20+0+0 -e ";
				procrack = Runtime.getRuntime().exec(
					xterm + 
					"aircrack-ng -a 2 " + 
					"-w " + wordlist + 
					" -l wpacracked.txt wpa-01.cap");
			} catch (IOException e) {} catch (InterruptedException ie) {}
			
			wpacracker wpac = new wpacracker();
			wpac.t.start();
		} else {
			wpacracker.flag = true;
		}
	}
	public void generateReadme() {
		try {
			String home = getClass().getProtectionDomain()
                   			.getCodeSource().getLocation()
                      			.getPath().replaceAll("%20", " ");
			JarFile jar = new JarFile(home);
			ZipEntry entry = jar.getEntry("README.txt");
			File efile = new File(".", entry.getName());
			
			InputStream in = 
				new BufferedInputStream(jar.getInputStream(entry));
			OutputStream out = 
				new BufferedOutputStream(new FileOutputStream(efile));
			byte[] buffer = new byte[2048];
			for (;;)  {
				int nBytes = in.read(buffer);
				if (nBytes <= 0) break;
				out.write(buffer, 0, nBytes);
			}
			out.flush();
			out.close();
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void generatePasswords() {
		try {
			String home = getClass().getProtectionDomain()
                   			.getCodeSource().getLocation()
                      			.getPath().replaceAll("%20", " ");
			JarFile jar = new JarFile(home);
			ZipEntry entry = jar.getEntry("default_pw.txt");
			File efile = new File(".", entry.getName());
			
			InputStream in = 
				new BufferedInputStream(jar.getInputStream(entry));
			OutputStream out = 
				new BufferedOutputStream(new FileOutputStream(efile));
			byte[] buffer = new byte[2048];
			for (;;)  {
				int nBytes = in.read(buffer);
				if (nBytes <= 0) break;
				out.write(buffer, 0, nBytes);
			}
			out.flush();
			out.close();
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public void wpaAttack() {
		
		if (wpadeauth.getLabel().equals("Start Deauth + Handshake Capture Attack") == true) {
			wpadeauth.setLabel("Stop Deauth + Handshake Attack");
			setEnable(false);
			wpadeauth.setEnabled(true);
			try {
				
				pro1 = Runtime.getRuntime().exec("rm -rf wpa-01.cap");
				pro1.waitFor();
				pro1.destroy();
				
				String drv = (String)drivers.getItemAt(drivers.getSelectedIndex());
				String xterm;
				//if (hideWin.isSelected() == true)
				//	xterm = "";
				//else
					xterm = "xterm -fg green -bg black -geom 100x15+0+0 -e ";
				pro1 = Runtime.getRuntime().exec(
					xterm + 
					"airodump-ng -w wpa --bssid " + currentBSSID + 
					" -c " + currentChannel + " --output-format pcap " + drv);
			} catch (IOException e) {} catch (InterruptedException ex) {}
			
			scripter s = new scripter();
			s.t.start();
			// to turn off, set scripter.flag = true;
		} else {
			setEnable(true);
			wpadeauth.setLabel("Start Deauth + Handshake Capture Attack");
			scripter.flag = true;
		}
	}
	public void addTargetsToList() {
		// uses targetClass.java to load AP's found in airodump to the list
		// the loop in targetClass waits for the button to say 'Refresh Targets' to end the loop
		setEnable(false);
		targets.setEnabled(true);
		buttonTargets.setEnabled(true);
		
		buttonTargets.setLabel("Stop scanning");
		
		tc = new targetClass();
		tc.t.start();
	}
	public static void stat(String upd8) {
		// simple statusbar updater; blank=inactive!
		if (upd8.equals("") == true)
			status.setText(" Status: [inactive]");
		else
			status.setText(" Status: [" + upd8 + "]");
	}
	
	public static boolean isValidDriver(String name) {
		// checks if 'name' is a valid wifi interface aka IS IN MONITOR MODE!!
		// program was *locking up the computer entirely* when trying
		//   to run airodump with a non-monitor mode wifi interface
		// this fixed it.
		
		Process proDrv;
		BufferedReader res1;
		boolean flag = false;
		
		try {
			proDrv = Runtime.getRuntime().exec("iwconfig " + name);
			res1 = new BufferedReader(new InputStreamReader(proDrv.getInputStream()));
			proDrv.waitFor(); // wait for data
			
			String line;
			while ((line = res1.readLine()) != null) {
				if (line.indexOf("Mode:Monitor") >= 0) {
					flag = true;
					break;
				}
			}
			proDrv.destroy();
		} catch (InterruptedException e) {} catch (IOException e) {}
		
		return flag;
	}
	
	public static void addDriversToList() throws IOException {
		// enums list of wifi interfaces into the "drivers" combobox
		// only adds wifi interfaces that are already in monitor mode.
		Process proDrv;
		BufferedReader res1;
		String drv;
		
		/*
		while (drivers.getItemCount() > 0) {
			drivers.removeItemAt(0);
			pause(0.001);
		}*/
		drivers.removeAllItems();
		
		drivers.addItem("WiFi Device:");
		try {
			proDrv = Runtime.getRuntime().exec("airmon-ng");
			res1 = new BufferedReader(new InputStreamReader(proDrv.getInputStream()));
			proDrv.waitFor(); // wait for data
			
			String line;
			while ((line = res1.readLine()) != null) {
				if ((line.indexOf("Interface") < 0) && (line.equals("") == false)) {
					drv = line.substring(0, line.indexOf("\t"));
					if (isValidDriver(drv) == true)
						drivers.addItem(drv);
				}
			}
			proDrv.destroy();
		} catch (InterruptedException e) {} catch (IOException e) {}
	}
	
	public static void pause(double dtime) {
		// pauses for selected period of time (IN SECONDS)
		try {
			Thread.currentThread().sleep((int)(dtime * 1000));
		}
			catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean fileExists(String filename) {
		File f = new File(filename);
		return f.exists();
	}
	
	public void windowClosing(WindowEvent e) {
		// event : when main window is told to close [exit]
		dispose();
		try {
			// if we're still looking for targets, stop!!!
			if (tc.t.isAlive() == true) {
				buttonTargets.setLabel("Refresh Targets");
				try {
					tc.t.join();
				} catch (InterruptedException derp) {}
			}
		} catch (NullPointerException ex) {}
		
		// save settings
		saveSettings();
		
		// gtfo
		System.exit(0);
	}
	
	// no idea, but it works!
	public void windowOpened(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}

}