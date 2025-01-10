package main;

// -gui -agents BankAgent:main.BankAgent
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class BankAgent extends Agent {
	private static final String FILE_NAME = "accounts.txt";
	private static final List<String> AGENT_NAMES = new ArrayList<>(List.of("ATMAgent", "LogAgent"));
	private static final List<String> AGENT_CLASSES = new ArrayList<>(List.of("main.ATMAgent", "main.LogAgent"));
	private int atmCounter = 1;

	private HashMap<String, Account> accounts = new HashMap<>();
	private BankGui myGui;
	private LogAgent dataAgent;

	protected void setup() {
		System.out.println("Bank-agent " + getAID().getName() + " is ready.");

		// Create and show the GUI
		myGui = new BankGui(this);
		myGui.showGui();

		// Register the service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("banking-system");
		sd.setName("JADE-banking");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		accounts = loadAccounts();

		addBehaviour(new BankOperationsBehaviour());

		ContainerController container = getContainerController();

		try {
			for (int i = 0; i < AGENT_NAMES.size(); i++) {
				AgentController agent = container.createNewAgent(AGENT_NAMES.get(i), AGENT_CLASSES.get(i), null);
				agent.start();
			}
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	protected void takeDown() {
		saveAccounts();

		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Bank-agent " + getAID().getName() + " terminating.");
	}

	public void shutdownSystem() {
		System.out.println(getLocalName() + ": System shutdown...");

		ACLMessage shutdownMessage = new ACLMessage(ACLMessage.INFORM);
		shutdownMessage.setContent("SHUTDOWN");

		for (String agentName : AGENT_NAMES) {
			shutdownMessage.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}

		send(shutdownMessage);

		doDelete();
	}

	boolean createAccount(String name, String pin, double balance) {
		if (accounts.containsKey(name)) {
			return false;
		}
		accounts.put(name, new Account(name, pin, balance));
		log("Created account for user: " + name + ", Initial Balance: " + balance);
		saveAccounts();
		return true;
	}

	public HashMap<String, Account> getAccounts() {
		return accounts;
	}

	private class BankOperationsBehaviour extends CyclicBehaviour {
		public void action() {
			ACLMessage msg = receive();
			if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
				String[] content = msg.getContent().split(",");

				if (content.length != 4)
					return;

				ACLMessage reply = msg.createReply();

				switch (content[0]) {
				case "WITHDRAW":
					handleWithdraw(reply, content);
					break;
				case "DEPOSIT":
					handleDeposit(reply, content);
					break;
				case "CHECK BALANCE":
					handleCheckBalance(reply, content);
					break;
				default:
					reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
					reply.setContent("Invalid operation.");
				}
				send(reply);
			} else {
				block();
			}
		}
	}

	private void handleWithdraw(ACLMessage reply, String[] content) {
		String accountName = content[1];
		String pin = content[2];
		double amount = Double.parseDouble(content[3]);

		Account account = accounts.get(accountName);
		if (account == null) {
			reply.setPerformative(ACLMessage.REFUSE);
			reply.setContent("Account not found.");
		} else if (!account.verifyPin(pin)) {
			reply.setPerformative(ACLMessage.REFUSE);
			reply.setContent("Invalid PIN.");
		} else if (account.getBalance() < amount) {
			reply.setPerformative(ACLMessage.REFUSE);
			reply.setContent("Insufficient funds.");
		} else {
			account.withdraw(amount);
			reply.setPerformative(ACLMessage.CONFIRM);
			reply.setContent("Withdrawal successful. New balance: " + account.getBalance());
			log("Withdrawal successful. Account: " + accountName + ", Amount: " + amount);
			saveAccounts();
		}
	}

	private void handleDeposit(ACLMessage reply, String[] content) {
		String accountName = content[1];
		double amount = Double.parseDouble(content[3]);

		Account account = accounts.get(accountName);
		if (account == null) {
			reply.setPerformative(ACLMessage.REFUSE);
			reply.setContent("Account not found.");
		} else {
			account.deposit(amount);
			reply.setPerformative(ACLMessage.CONFIRM);
			reply.setContent("Deposit successful. New balance: " + account.getBalance());
			log("Deposit successful. Account: " + accountName + ", Amount: " + amount);
			saveAccounts();
		}
	}

	private void handleCheckBalance(ACLMessage reply, String[] content) {
		String accountName = content[1];

		Account account = accounts.get(accountName);
		if (account == null) {
			reply.setPerformative(ACLMessage.REFUSE);
			reply.setContent("Account not found.");
		} else {
			reply.setPerformative(ACLMessage.INFORM);
			reply.setContent("Balance: " + account.getBalance());
		}
	}

	public void saveAccounts() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
			for (Account account : accounts.values()) {
				writer.write(account.getName() + "," + account.getPin() + "," + account.getBalance());
				writer.newLine();
			}
			System.out.println("Accounts successfully saved to " + FILE_NAME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HashMap<String, Account> loadAccounts() {
		HashMap<String, Account> loadedAccounts = new HashMap<>();
		File file = new File(FILE_NAME);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",");
					if (parts.length == 3) {
						String name = parts[0];
						String pin = parts[1];
						double balance = Double.parseDouble(parts[2]);
						loadedAccounts.put(name, new Account(name, pin, balance));
					}
				}
				System.out.println("Accounts successfully loaded from " + FILE_NAME);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("No saved accounts found.");
		}
		return loadedAccounts;
	}

	public void createNewATM() {
		ContainerController container = getContainerController();
		String atmName = "ATMAgent" + (++atmCounter);

		try {
			AgentController atmAgent = container.createNewAgent(atmName, "main.ATMAgent", null);
			atmAgent.start();
			AGENT_NAMES.add(atmName);
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	private void log(String message) {
		ACLMessage logMessage = new ACLMessage(ACLMessage.INFORM);
		logMessage.setContent(message);
		logMessage.addReceiver(new AID("LogAgent", AID.ISLOCALNAME));
		send(logMessage);
	}
}