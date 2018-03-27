import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TransactionManager {
	
	AccountManager accountManager;
	int numTransactions;
	ArrayList<Transaction> transactions;

	public TransactionManager(AccountManager accountManager)
	{
		this.transactions = new ArrayList<Transaction>();
		this.accountManager = accountManager;
		this.numTransactions = 0;
	}
	
	public void run(int port) throws IOException 
	{
		@SuppressWarnings("resource")
		ServerSocket serverSock = new ServerSocket(port);
		
		while(true)
		{
			System.out.println("Listening for connections");
			Socket socket = serverSock.accept();
			Transaction incomingTransaction = new Transaction(numTransactions++);
			transactions.add(incomingTransaction);
			new Thread(new TransactionManagerWorker(socket, incomingTransaction, this.accountManager)).start();
		}		
	}	
}