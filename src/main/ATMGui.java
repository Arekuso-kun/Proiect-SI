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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

class ATMGui extends JFrame {
	private ATMAgent myAgent;

	private JTextField nameField, pinField, amountField;
	private JButton depositButton, withdrawButton, checkBalanceButton;

	ATMGui(ATMAgent agent) {
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

		p.add(new JLabel("Amount:"));
		amountField = new JTextField(15);
		amountField.addKeyListener(new KeyAdapter() {
			// Check if input if is digit
			public void keyTyped(KeyEvent e) {
				if (!Character.isDigit(e.getKeyChar())) {
					e.consume(); // Ignore
				}
			}
		});
		p.add(amountField);

		nameField.setPreferredSize(new Dimension(100, 25));
		pinField.setPreferredSize(new Dimension(100, 25));
		amountField.setPreferredSize(new Dimension(100, 25));

		getContentPane().add(p, BorderLayout.CENTER);

		ActionListener buttonListener = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String name = nameField.getText().trim();
					String pin = pinField.getText().trim();
					String amountText = amountField.getText().trim();
					double amount;
					String operation = ev.getActionCommand().toUpperCase();

					if (name.isEmpty()) {
						throw new Exception("Name field cannot be empty.");
					}

					if (pin.isEmpty()) {
						throw new Exception("PIN field cannot be empty.");
					}

					if (pin.length() < 4) {
						throw new Exception("PIN must be 4 digits.");
					}

					if (amountText.isEmpty()) {
						throw new Exception("Amount field cannot be empty.");
					}

					amount = Double.parseDouble(amountText);
					if (amount <= 0) {
						throw new Exception("Amount must be greater than zero.");
					}

					myAgent.sendRequest(operation, name, pin, amount);

					if (operation.equals("Deposit") || operation.equals("Withdraw")) {
						nameField.setText("");
						pinField.setText("");
						amountField.setText("");
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(ATMGui.this, "Invalid input: " + e.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		p = new JPanel();

		depositButton = new JButton("Deposit");
		withdrawButton = new JButton("Withdraw");
		checkBalanceButton = new JButton("Check Balance");

		p.add(depositButton);
		p.add(withdrawButton);
		p.add(checkBalanceButton);

		depositButton.addActionListener(buttonListener);
		withdrawButton.addActionListener(buttonListener);
		checkBalanceButton.addActionListener(buttonListener);

		getContentPane().add(p, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
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
