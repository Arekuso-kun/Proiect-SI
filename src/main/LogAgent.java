package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class LogAgent extends Agent {
	private static final String LOG_FILE_NAME = "logs.txt";

	protected void setup() {
		System.out.println("Log-agent " + getAID().getName() + " is ready.");

		addBehaviour(new LogMessageBehaviour());
	}

	protected void takeDown() {
		System.out.println("Log-agent " + getAID().getName() + " terminating.");
	}

	private class LogMessageBehaviour extends CyclicBehaviour {
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().equals("SHUTDOWN")) {
					doDelete();
				} else if (msg.getPerformative() == ACLMessage.INFORM) {
					String logEntry = createLogEntry(msg.getContent());
					logToFile(logEntry);
				}
			} else {
				block();
			}
		}

		private String createLogEntry(String content) {
			String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			return ("[" + timestamp + "] " + content);
		}

		private void logToFile(String logEntry) {

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME, true))) {
				writer.write(logEntry);
				writer.newLine();
				System.out.println("Logged: " + logEntry);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
