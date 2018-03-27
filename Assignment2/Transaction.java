import java.util.ArrayList;

public class Transaction {
	private ArrayList<Lock> heldLocks;
	public boolean open;
	private String logMessages;
	public int id;
	
	public Transaction(int id)
	{
		this.id = id;
		this.heldLocks = new ArrayList<Lock>();
		this.open = true;
		this.logMessages = new String();
		this.logMessages += "Transaction " + Integer.toString(id) + " opened\n";
		
	}
	
	public void log(String logString)
	{
		this.logMessages += logString + '\n';
	}
	
	public String getLog()
	{
		return this.logMessages;
	}
	
	public ArrayList<Lock> getHeldLocks()
	{
		return this.heldLocks;
	}

	public void dumpLog() {
		System.out.println(this.logMessages);
		
	}
}
