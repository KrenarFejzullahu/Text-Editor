package gui;

import handlers.ConnectViewThread;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import client.Client;

@SuppressWarnings("all")

public class ConnectView extends JPanel implements ActionListener {

	private final MainWindow frame;
	
	private final JLabel serverAddressLabel;
	private final JLabel hostLabel;
	private final JTextField host;
	private final JLabel portLabel;
	private final JTextField port;
	private final JButton connectButton;
	private Client client;
	private boolean DEBUG;

	
	public ConnectView(MainWindow frame) {
		this.frame = frame;
		serverAddressLabel = new JLabel("Enter the server address:");
		hostLabel = new JLabel("Host:");
		host = new JTextField();
		host.addActionListener(this);
		portLabel = new JLabel("Port:");
		port = new JTextField();
		port.addActionListener(this);
		connectButton = new JButton("Connect");
		connectButton.addActionListener(this);
		this.client = frame.getClient();

		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);

		layout.setHorizontalGroup(layout
				.createParallelGroup()
				.addComponent(serverAddressLabel)
				.addGroup(
						layout.createSequentialGroup()
								.addGroup(
										layout.createParallelGroup()
												.addComponent(hostLabel)
												.addComponent(portLabel))
								.addGroup(
										layout.createParallelGroup()
												.addComponent(host, 100, 150,
														Short.MAX_VALUE)
												.addComponent(port, 100, 150,
														Short.MAX_VALUE)))
				.addComponent(connectButton));
		layout.setVerticalGroup(layout
				.createSequentialGroup()
				.addComponent(serverAddressLabel)
				.addGroup(
						layout.createParallelGroup()
								.addComponent(hostLabel)
								.addComponent(host, GroupLayout.PREFERRED_SIZE,
										25, GroupLayout.PREFERRED_SIZE))
				.addGroup(
						layout.createParallelGroup()
								.addComponent(portLabel)
								.addComponent(port, GroupLayout.PREFERRED_SIZE,
										25, GroupLayout.PREFERRED_SIZE))
				.addComponent(connectButton));

	}

	
	public void actionPerformed(ActionEvent e) {
		String hostInput = host.getText().trim();
		String portInput = port.getText().trim();
		String portRegex = "\\d\\d?\\d?\\d?\\d?";
		if (hostInput.length() != 0 && portInput.matches(portRegex)) {
			try {
				client = new Client(Integer.parseInt(portInput), hostInput,
						frame);
				frame.setClient(client);
			} catch (NumberFormatException e1) {
				JOptionPane.showMessageDialog(null, "Invalid arguments",
						"Error", JOptionPane.ERROR_MESSAGE);

			}
			client.setMainWindow(frame);
			if (DEBUG) {
				System.out
						.println("I am here, the client setMainWindow. ConnectView");
			}
			
			ConnectViewThread thread = new ConnectViewThread(this);
			
			thread.start();

		} else {
			JOptionPane.showMessageDialog(null, "Invalid arguments", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	
	public Client getClient() {
		return client;
	}
}