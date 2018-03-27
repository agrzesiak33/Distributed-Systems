import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TransactionManagerWorker implements Runnable
{
	private Socket clientSocket;
	private Transaction transaction;
	private AccountManager accountManager;
	
	public TransactionManagerWorker(Socket socket, Transaction transaction, AccountManager am)
	{
		this.clientSocket = socket;
		this.transaction = transaction;
		this.accountManager = am;
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
				incomingString = (String) input.readObject();
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Could not read string from input stream");
			}
			
			
			if(incomingString != null)
			{
				String messageSplit[] = incomingString.toLowerCase().split(" ");
				String operation = messageSplit[0];
				
				// When the connection is made, the transaction is started so this is redundant
				//if(operation.contains("start"))
				
				
				if(operation.contains("finish"))
				{
					running = finish(output, input);	
				}
				
				else if(operation.contains("get"))
				{
					get(output, messageSplit);
				}
				else if(operation.contains("transfer"))
				{
					transfer(messageSplit);
				}
			}
		}
	}


	private boolean finish(ObjectOutputStream output, ObjectInputStream input) 
	{
		boolean running = false;
		
		this.transaction.log("Closing transaction " + Integer.toString(this.transaction.id));
		
		if(this.accountManager.getUsesLock())
		{
			this.transaction.log("Releasing all locks for " + Integer.toString(this.transaction.id));
			this.accountManager.getLockManager().unLock(this.transaction);
			this.transaction.log("Released all locks for " + Integer.toString(this.transaction.id));
			this.transaction.dumpLog();
		}
		
		this.transaction.open = false;
		
		try
		{
			output.close();
			input.close();
			this.clientSocket.close();
		}catch(Exception e)
		{
			System.err.println("Couldn't close socket and streams gracefully");
		}
		
		return running;
	}


	private void get(ObjectOutputStream output, String[] messageSplit) 
	{
		//	Get the account number from the stream 
		int accountNumber = -1;
		try
		{
			accountNumber = Integer.parseInt(messageSplit[1]);
		}
		catch(Exception e)
		{
			System.err.println("Couldn't parse the account number from the stream");
		}
		
		//	Starts the request from the account manager
		if(accountNumber != -1)
		{
			this.transaction.log("Requesting to read balance from " + Integer.toString(accountNumber));
			int balance = this.accountManager.getBalance(this.transaction, accountNumber);
			this.transaction.log("Read the balance from " + Integer.toString(accountNumber));
		
			// Sends the balance back
			try {
				output.writeObject(Integer.toString(balance));
			} catch (IOException e) {
				System.err.println("Coulnd't send to output stream");
			}
		}
	}


	private void transfer(String[] messageSplit) 
	{
		// Get the accounts we are sending to and from and the amount
		int toAccount = -1;
		int fromAccount = -1;
		int amount = -1;
		try
		{
			amount = Integer.parseInt(messageSplit[1]);
			fromAccount = Integer.parseInt(messageSplit[2]);
			toAccount = Integer.parseInt(messageSplit[3]);
		}
		catch(Exception e)
		{
			System.err.println("Couldn't parse the account number from the stream");
		}
		
		if(toAccount != -1 && fromAccount != -1 && amount != -1)
		{
			// Read the account balance of the receiver
			this.transaction.log("Requesting to read balance from " + toAccount);
			int toAccountBalance = this.accountManager.getBalance(this.transaction, toAccount);
			this.transaction.log("Read the balance from " + toAccount);
			
			// Read the account balance from the sender
			this.transaction.log("Requesting to read balance from " + fromAccount);
			int fromAccountBalance = this.accountManager.getBalance(this.transaction, fromAccount);
			this.transaction.log("Read the balance from " + fromAccount);
			
			// Add the money to the receiver
			this.transaction.log("Requesting to set " + toAccount + " to " + (toAccountBalance + amount));
			this.accountManager.setBalance(this.transaction, toAccount, toAccountBalance + amount);
			this.transaction.log("Set " + toAccount + " to " + (toAccountBalance + amount));
			
			// Subtract the money from the sender
			this.transaction.log("Requesting to set " + fromAccount + " to " + (fromAccountBalance - amount));
			this.accountManager.setBalance(this.transaction, fromAccount, fromAccountBalance - amount);
			this.transaction.log("Set " + fromAccount + " to " + (fromAccountBalance - amount));
		}
	}
}
