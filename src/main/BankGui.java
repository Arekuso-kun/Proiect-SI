package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

class BankGui extends JFrame {
	private BankAgent myAgent;

	private JTextField nameField, pinField, balanceField;

	BankGui(BankAgent agent) {
		super(agent.getLocalName());

		myAgent = agent;

		JPanel p = new JPanel();
		p.setLayout(new GridLayout(3, 2, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		p.add(new JLabel("Name:"));
		nameField = new JTextField(15);
		p.add(nameField);

		p.add(new JLabel("PIN:"));
		pinField = new JPasswordField(15);
		pinField.addKeyListener(new KeyAdapter() {
			// Check if input if is digit and not more than 4 digits
			public void keyTyped(KeyEvent e) {
				String currentText = new String(pinField.getText());
				if (currentText.length() >= 4 || !Character.isDigit(e.getKeyChar())) {
					e.consume(); // Ignore
				}
			}
		});
		p.add(pinField);

		p.add(new JLabel("Initial Balance:"));
		balanceField = new JTextField(15);
		balanceField.addKeyListener(new KeyAdapter() {
			// Check if input if is digit
			public void keyTyped(KeyEvent e) {
				if (!Character.isDigit(e.getKeyChar())) {
					e.consume(); // Ignore
				}
			}
		});
		p.add(balanceField);

		nameField.setPreferredSize(new Dimension(100, 25));
		pinField.setPreferredSize(new Dimension(100, 25));
		balanceField.setPreferredSize(new Dimension(100, 25));

		getContentPane().add(p, BorderLayout.CENTER);

		JButton createButton = new JButton("Create Account");
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String name = nameField.getText().trim();
					String pin = pinField.getText().trim();
					String balanceText = balanceField.getText().trim();
					double balance;

					if (name.isEmpty()) {
						throw new Exception("Name field cannot be empty.");
					}

					if (pin.isEmpty()) {
						throw new Exception("PIN field cannot be empty.");
					}

					if (pin.length() < 4) {
						throw new Exception("PIN must be 4 digits.");
					}

					if (balanceText.isEmpty()) {
						throw new Exception("Balance field cannot be empty.");
					}

					balance = Double.parseDouble(balanceText);
					if (balance < 0) {
						throw new Exception("Balance must be greater or equal to zero.");
					}

					boolean created = myAgent.createAccount(name, pin, balance);
					JOptionPane.showMessageDialog(BankGui.this,
							created ? "Account created successfully." : "Account already exists.", "Info",
							JOptionPane.INFORMATION_MESSAGE);

					if (created) {
						nameField.setText("");
						pinField.setText("");
						balanceField.setText("");
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(BankGui.this, "Invalid input: " + e.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		p = new JPanel();
		p.add(createButton);
		getContentPane().add(p, BorderLayout.SOUTH);

		JButton listUsersButton = new JButton("List Users");
		listUsersButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				HashMap<String, Account> accounts = myAgent.getAccounts();
				if (accounts.isEmpty()) {
					JOptionPane.showMessageDialog(BankGui.this, "No users available.", "Info",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					StringBuilder userList = new StringBuilder("List of Users:\n");
					for (Account account : accounts.values()) {
						userList.append(account).append("\n");
					}
					JOptionPane.showMessageDialog(BankGui.this, userList.toString(), "Users",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
		p.add(listUsersButton);
		getContentPane().add(p, BorderLayout.SOUTH);

		JButton openATMButton = new JButton("Open new ATM");
		openATMButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				myAgent.createNewATM();
			}
		});
		p.add(openATMButton);
		getContentPane().add(p, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.shutdownSystem();
				myAgent.doDelete();
			}
		});

		setResizable(false);
	}

	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int) screenSize.getWidth() / 2;
		int centerY = (int) screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}
}
