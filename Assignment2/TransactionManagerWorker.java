import java.net.Socket;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TransactionManagerWorker implements Runnable
{
	private Socket clientSocket;
	private TransactionManager transactionManager;
	
	public TransactionManagerWorker(Socket socket, TransactionManager parent)
	{
		this.clientSocket = socket;
		this.transactionManager = parent;
	}
	

	@Override
	public void run()
	{
		//	Get the streams to read and write to
		
		ObjectOutputStream output = null;
		ObjectInputStream input = null;
		try {
			output= new ObjectOutputStream(this.clientSocket.getOutputStream());
			input = new ObjectInputStream(this.clientSocket.getInputStream());
			
		} catch (IOException e) {
			System.err.println("Could not setup streams");
		}
		
		// Listen for messages coming from the client
		boolean running = true;
		while(running)
		{
			String incomingString = null;
			try {
				incomingString = String.valueOf(input.readObject());
				System.out.println("IncomingString: " + incomingString);
			} catch (EOFException e) {
				running = false;
				try {this.clientSocket.close();} catch (Exception e1){}
			} catch (Exception e) {
				System.err.println("Could not read string from input stream");
				e.printStackTrace();
			}
			
			
			if(incomingString != null)
			{
				String messageSplit[] = incomingString.toLowerCase().split(" ");
				String operation = messageSplit[0];
				
				if(operation.contains("start"))
				{
					Transaction newTransaction = new Transaction(this.transactionManager.numTransactions.getAndIncrement());
					
					this.transactionManager.transactions.put(newTransaction.id, newTransaction);
					
					try {
						output.writeObject(Integer.toString(newTransaction.id));
					} catch (IOException e) {}
				}
				
				
				if(operation.contains("finish") && operation.length() >= 2)
				{
					finish(Integer.parseInt(messageSplit[1].trim()));	
				}
				
				else if(operation.contains("get"))
				{
					int accountNumber = Integer.parseInt(messageSplit[1].trim());
					int transactionNumber = Integer.parseInt(messageSplit[2].trim());

					get(output, accountNumber, transactionNumber);
				}
				else if(operation.contains("transfer"))
				{
					transfer(messageSplit);
				}
			}
		}
	}


	private boolean finish(int transactionID) 
	{
		
		Transaction currentTransaction = this.transactionManager.transactions.get(transactionID);
		
		if(currentTransaction == null)
		{
			return false;
		}
		
		currentTransaction.log("Closing transaction " + Integer.toString(currentTransaction.id));
		
		if(this.transactionManager.accountManager.getUsesLock())
		{
			currentTransaction.log("Releasing all locks for " + Integer.toString(currentTransaction.id));
			this.transactionManager.accountManager.getLockManager().unLock(currentTransaction);
			currentTransaction.log("Released all locks for " + Integer.toString(currentTransaction.id));
			currentTransaction.dumpLog();			
		}
		
		currentTransaction.open = false;
		currentTransaction.dumpLog();
		
		return true;
	}


	private void get(ObjectOutputStream output, int accountNumber, int transactionNumber) 
	{
		Transaction currentTransaction = this.transactionManager.transactions.get(transactionNumber);
		
		//	Starts the request from the account manager
		if(accountNumber != -1)
		{
			currentTransaction.log("Requesting to read balance from " + Integer.toString(accountNumber));
			int balance = this.transactionManager.accountManager.getBalance(currentTransaction, accountNumber);
			currentTransaction.log("Read the balance " + balance + " from " + Integer.toString(accountNumber));
		
			// Sends the balance back
			try {
				output.writeObject(Integer.toString(balance));
			} catch (IOException e) {
				System.err.println("Couldn't send to output stream GET");
			}
		}
	}


	private void transfer(String[] messageSplit) 
	{
		// Get the accounts we are sending to and from and the amount
		int amount = Integer.parseInt(messageSplit[1].trim());
		int fromAccount = Integer.parseInt(messageSplit[2].trim());
		int toAccount = Integer.parseInt(messageSplit[3].trim());
		int transactionID = Integer.parseInt(messageSplit[4].trim());
		
		//System.out.println("-" + toAccount + " " + fromAccount + " " + amount + " " + transactionID);
		
		Transaction currentTransaction = this.transactionManager.transactions.get(transactionID);
		
		if(toAccount != -1 && fromAccount != -1 && amount != -1)
		{
			// Read the account balance of the receiver
			currentTransaction.log("Requesting to read balance from " + toAccount);
			int toAccountBalance = this.transactionManager.accountManager.getBalance(currentTransaction, toAccount);
			currentTransaction.log("Read the balance '" + toAccountBalance + "' from " + toAccount);
			System.out.println("Read toAccount");
			
			// Read the account balance from the sender
			currentTransaction.log("Requesting to read balance from " + fromAccount);
			int fromAccountBalance = this.transactionManager.accountManager.getBalance(currentTransaction, fromAccount);
			currentTransaction.log("Read the balance '" + toAccountBalance + "' from " + fromAccount);
			
			System.out.println("Read fromAccounts");
			
			// Add the money to the receiver
			currentTransaction.log("Requesting to set " + toAccount + " to " + (toAccountBalance + amount));
			this.transactionManager.accountManager.setBalance(currentTransaction, toAccount, toAccountBalance + amount);
			currentTransaction.log("Set " + toAccount + " to " + (toAccountBalance + amount));
			System.out.println("Added money");
			
			// Subtract the money from the sender
			currentTransaction.log("Requesting to set " + fromAccount + " to " + (fromAccountBalance - amount));
			this.transactionManager.accountManager.setBalance(currentTransaction, fromAccount, fromAccountBalance - amount);
			currentTransaction.log("Set " + fromAccount + " to " + (fromAccountBalance - amount));
			System.out.println("Subtracted money");
		}
	}
}
