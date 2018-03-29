import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class TransactionClient
{
    public static void main(String args[])
    {

        int[] properties = getProperties(args[0]);
        int port = properties[0];
        int numberOfTransactions = properties[1];
        int numbeOfAccounts = properties[2];
        
        //setting up possible accounts
        int [] accountNumbers = new int[numbeOfAccounts]; //# accounts provided by properties file
        for (int i = 0; i<accountNumbers.length; i++)
        {
            accountNumbers[i]= i;
        }
        String [] possibleOperations = {"read", "write"};

        //clients creates TransactionServerProxy instance
        //host and port are passed as arguments to the proxy
        TransactionServerProxy TSP = new TransactionServerProxy("localhost", port);

        Random randNumber = new Random();
        for (int j =0; j < numberOfTransactions; j++)
        {
            //sends start signal tooo server
            int currentTransactionID = TSP.startTransaction();
            //selects random operation
            int indexOfOp = randNumber.nextInt(2); //either 0 or 1
            String selectedOP = possibleOperations[indexOfOp];
            //selects random account



            if (selectedOP.equalsIgnoreCase("read"))
            {
                //select an account to read balance.
                int selectedAccount = accountNumbers[randNumber.nextInt(10)];
                System.out.println("\tReading account balance from " + selectedAccount + " in transaction " + currentTransactionID);
                TSP.readBalance(selectedAccount, currentTransactionID);
            }
            else if (selectedOP.equalsIgnoreCase("write"))
            {
                int senderAccount = accountNumbers[randNumber.nextInt(10)];
                int receiverAccount = accountNumbers[randNumber.nextInt(10)];
                //make sure the accounts are not the same
                while(senderAccount==receiverAccount)
                {
                    receiverAccount = accountNumbers[randNumber.nextInt(10)];
                }
                //amount of money sent from one account to another is between 1 and 100.
                int amount = randNumber.nextInt(10);
                System.out.println("\tTransfering " + amount + " from '" + senderAccount + "' to '" + receiverAccount + " in transaction " + currentTransactionID);
                TSP.writeBalance(senderAccount,amount,receiverAccount, currentTransactionID);
            }
            else
            {
                System.out.println("Error Operation not found.");
            }
            //sends end of transaction signal to the server.
            TSP.finishTransaction(currentTransactionID);
        }

        //no more transactions are send but still waiting for responses.
        //if all transactions got a message of end of transaction, then the conection is closed.
        while (!(numberOfTransactions == TSP.getTransactionsFinished() && TSP.getTransactionsStarted() == TSP.getTransactionsFinished()))
        {
            TSP.finishConnection();
            break;
        }
    }

    @SuppressWarnings("resource")
	private static int[] getProperties(String fileName)
    {
        int numTransactions = -1;
        int port = -1;
        int numAccounts = -1;

        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.err.println("File doesn't exist");
            return null;
        }
        String line;
        try {
            while((line = reader.readLine()) != null)
            {
                String parts[] = line.split("=");

                if(parts[0].toLowerCase().contains("transactions"))
                {
                    try
                    {
                        numTransactions = Integer.parseInt(parts[1].trim());
                    }catch(NumberFormatException e)
                    {
                        System.err.println("Could not parse integer " + parts[1].trim());
                        return null;
                    }

                }
                else if(parts[0].toLowerCase().contains("port"))
                {
                    try
                    {
                        port = Integer.parseInt(parts[1].trim());
                    }catch(NumberFormatException e)
                    {
                        System.err.println("Could not parse integer " + parts[1].trim());
                        return null;
                    }
                }
                else if (parts[0].toLowerCase().contains("accounts"))
                {
                    try
                    {
                        numAccounts = Integer.parseInt(parts[1].trim());
                    }catch(NumberFormatException e)
                    {
                        System.err.println("Could not parse integer " + parts[1].trim());
                        return null;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Couldn't read line");
            System.err.println(e);
            return null;
        }
        try {
            reader.close();
        } catch (IOException e) {}

        return(new int[]{port, numTransactions, numAccounts});
    }
}
