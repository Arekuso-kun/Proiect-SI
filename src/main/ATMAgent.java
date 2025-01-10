package main;

import javax.swing.JOptionPane;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ATMAgent extends Agent {
	private ATMGui myGui;
	private AID bankAgent;

	protected void setup() {
		System.out.println("ATM-agent " + getAID().getName() + " is ready.");

		myGui = new ATMGui(this);
		myGui.showGui();

		bankAgent = findBankAgent();

		addBehaviour(new HandleBankResponsesBehaviour());
	}

	protected void takeDown() {
		myGui.dispose();
		System.out.println("ATM-agent " + getAID().getName() + " terminating.");
	}

	public void sendRequest(String operation, String name, String pin, double amount) {
		if (bankAgent == null) {
			bankAgent = findBankAgent();
			if (bankAgent == null) {
				JOptionPane.showMessageDialog(myGui, "Banking service not found.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(bankAgent);
		msg.setContent(operation + "," + name + "," + pin + "," + amount);
		send(msg);
	}

	private AID findBankAgent() {
		try {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("banking-system");
			template.addServices(sd);

			DFAgentDescription[] results = DFService.search(this, template);
			if (results.length > 0) {
				return results[0].getName();
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return null;
	}

	private class HandleBankResponsesBehaviour extends CyclicBehaviour {
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().equals("SHUTDOWN")) {
					doDelete();
					return;
				}

				switch (msg.getPerformative()) {
				case ACLMessage.CONFIRM:
				case ACLMessage.INFORM:
					JOptionPane.showMessageDialog(myGui, msg.getContent(), "Success", JOptionPane.INFORMATION_MESSAGE);
					break;
				case ACLMessage.REFUSE:
				case ACLMessage.NOT_UNDERSTOOD:
					JOptionPane.showMessageDialog(myGui, msg.getContent(), "Error", JOptionPane.ERROR_MESSAGE);
					break;
				default:
					JOptionPane.showMessageDialog(myGui, "Unexpected response: " + msg.getContent(), "Warning",
							JOptionPane.WARNING_MESSAGE);
					break;
				}
			} else {
				block();
			}
		}
	}
}