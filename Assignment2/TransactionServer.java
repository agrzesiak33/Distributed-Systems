import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TransactionServer {
	public static void main(String args[])
	{
		boolean isLocking = false;
		int numAccounts = 0;
		int port = 0;
		
		// Get all the necessary information from the file
		if(args.length != 1)
		{
			return;
		}
		else
		{
			int[] temp = getProperties(args[0]);
			System.out.println("Imported Properties File");
			if(temp != null)
			{
				isLocking = (temp[0] == 1) ? true : false;
				numAccounts = temp[1];
				port = temp[2];
			}
			else
			{
				return;
			}
		}
		
		// Create all the managers
		AccountManager accountManager = new AccountManager(numAccounts, isLocking);
		TransactionManager transactionManager = new TransactionManager(accountManager);
		System.out.println("Created all managers");
		
		try {
			transactionManager.run(port);
		} catch (IOException e) {
			System.err.println("Error while running transaction server");
			System.err.println(e);
		}	
	}
	
	@SuppressWarnings("resource")
	public static int[] getProperties(String fileName)
	{
		int isLocking = 0;
		int numAccounts = 0;
		int port = 0;
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
				String parts[] = line.split(":");
				
				if(parts[0].toLowerCase().contains("lock"))
				{
					if(parts[1].toLowerCase().contains("fasle"))
					{
						isLocking = 0;
					}
					else if(parts[1].toLowerCase().contains("true"))
					{
						isLocking = 1;
					}
				}
				
				else if(parts[0].toLowerCase().contains("account"))
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
			}
		} catch (IOException e) {
			System.err.println("Couldn't read line");
			System.err.println(e);
			return null;
		}
		try {
			reader.close();
		} catch (IOException e) {}
		
		return(new int[]{isLocking, numAccounts, port});
	}
}
