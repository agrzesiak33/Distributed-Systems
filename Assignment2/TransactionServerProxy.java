import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TransactionServerProxy
{
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isConnected;
    private int transactionsStarted;
    private int transactionsFinished;


    //set up host and port.
    //connect with server
    //sets up in and out
    public TransactionServerProxy(String host, int port)
    {
        try {
            clientSocket = new Socket(host,port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnected = true;

        transactionsFinished = 0;
        transactionsStarted = 0;

        //starts a thread that reads and displays any message from server.
        //Thread t = new Thread(this);
        //t.start();
    }

    public int startTransaction()
    {
        int transactionID = -1;
        try {
            out.writeObject("-start");
            transactionID = Integer.parseInt(((String) in.readObject()).trim());
            System.out.println("Started transaction " + transactionID);
        } catch (IOException | NumberFormatException | ClassNotFoundException e) {
            System.err.println("Couldn't start a transaction\n" + e.toString());
        }
        transactionsStarted++;
		return transactionID;
    }

    public void finishTransaction(int transactionID)
    {
        System.out.println("Transaction finished");
        try {
            out.writeObject("-finish " + Integer.toString(transactionID));
            System.out.println("Closed the transaction");
        } catch (IOException e) {
        	System.err.println("Couldn't close the transaction");
            e.printStackTrace();
        }
    }

    public void readBalance(int accNumber, int transactionID)
    {
        try {
            //sends the account number from which the client wants to know the balance.
            String request = "-getBalance " + accNumber + " " + transactionID;
            out.writeObject(request);
            String incoming = (String) in.readObject();
            // TODO Not sure what the point of reading here is.
            System.out.println("Balance for account " + accNumber + " is " + incoming);
        } catch (IOException | ClassNotFoundException e) {
        	System.err.println("Couldn't read the balance");
            e.printStackTrace();
        }
    }

    //money sent from sender account to receiver
    public void writeBalance(int sender, int amount, int receiver, int transactionID)
    {
        String request = "-transferMoney " + amount +" "+ sender+ " "+ receiver + " " + transactionID;
        try {
            //sends a message with the amount of money to transfer from sender to receiver..
            out.writeObject(request);
            System.out.println("Transfer complete");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't transfer the money");
        }
    }

    //connection with server is ended
    public void finishConnection()
    {
        try {
        	Thread.sleep(500);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        isConnected = false;
        System.out.println("Connection with server closed.");
    }

    public int getTransactionsStarted() {
        return transactionsStarted;
    }

    public int getTransactionsFinished() {
        return transactionsFinished;
    }

    /*//Thread is on charge of reading and displaying any message from server.
    @Override
    public void run()
    {
        while (isConnected)
        {
            try {
                if (in.available()>0)
                {
                    String messageFromServer = String.valueOf(in.readObject());
                    if (messageFromServer.length()>0)
                    {
                        System.out.println("From server: " + messageFromServer);
                        if(messageFromServer.equalsIgnoreCase("-endOfTransaction"))
                        {
                            //if message of transaction finished is read, then counter of transactions finished sums 1.
                            transactionsFinished++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }*/
}
