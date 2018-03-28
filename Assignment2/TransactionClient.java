import java.util.Random;

public class TransactionClient
{
    //client
    //args contains:
    //1st host name/ip
    //2nd port
    //3rd my account number
    public static void main(String args[])
    {
        //setting up possible accounts
        int [] accountNumbers = new int[10]; //10 accounts
        for (int i = 0; i<accountNumbers.length; i++)
        {
            accountNumbers[i]= 1001+i;
        }
        String [] possibleOperations = {"read", "write"};

        //number of transactions that will be done by
        int numberOfTransactions = Integer.parseInt(args[2]);

        //clients creates TransactionServerProxy instance
        //host and port are passed as arguments to the proxy
        TransactionServerProxy TSP = new TransactionServerProxy(args[0],Integer.parseInt(args[1]));

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
                int amount = randNumber.nextInt(100)+1;
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
        }
    }
}
