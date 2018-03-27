import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TransactionManager {
	
	AccountManager accountManager;
	LockManager lockManager;
	int numTransactions;
	ArrayList<Transaction> transactions;

	public TransactionManager(AccountManager accountManager, LockManager lockManager)
	{
		this.transactions = new ArrayList<Transaction>();
		this.accountManager = accountManager;
		this.lockManager = lockManager;
		this.numTransactions = 0;
	}
	
	public void run(int port) throws IOException 
	{
		ServerSocket serverSock = new ServerSocket(port);
		
		while(true)
		{
			Socket socket = serverSock.accept();
			Transaction incomingTransaction = new Transaction(numTransactions++);
			transactions.add(incomingTransaction);
			new Thread(new TransactionManagerWorker(socket, incomingTransaction, this.accountManager)).start();
		}
	}
	
	
}
