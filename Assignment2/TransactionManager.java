import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionManager {
	
	AccountManager accountManager;
	AtomicInteger numTransactions;
	Hashtable<Integer, Transaction> transactions;

	public TransactionManager(AccountManager accountManager)
	{
		this.transactions = new Hashtable<Integer, Transaction>();
		this.accountManager = accountManager;
		this.numTransactions = new AtomicInteger(0);
	}
	
	public void run(int port) throws IOException 
	{
		@SuppressWarnings("resource")
		ServerSocket serverSock = new ServerSocket(port);
		
		while(true)
		{
			System.out.println("Listening for connections");
			Socket socket = serverSock.accept();
			new Thread(new TransactionManagerWorker(socket, this)).start();
		}		
	}	
}