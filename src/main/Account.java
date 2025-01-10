package main;

public class Account {
	private final String name;
	private final String pin;
	private double balance;

	public Account(String name, String pin, double balance) {
		this.name = name;
		this.pin = pin;
		this.balance = balance;
	}

	public boolean verifyPin(String pin) {
		return this.pin.equals(pin);
	}

	public String getName() {
		return name;
	}

	public String getPin() {
		return pin;
	}

	public double getBalance() {
		return balance;
	}

	public void withdraw(double amount) {
		balance -= amount;
	}

	public void deposit(double amount) {
		balance += amount;
	}

	public String toString() {
		return "Name: " + name + ", balance: " + balance;
	}
}