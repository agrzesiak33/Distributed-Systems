// Import lock package here

public class AccountManager {

	private int[] accounts;
	private LockManager lockManager;
	private boolean usesLocking;
	
	
	public AccountManager(int numAccounts, boolean usesLocking, int initialBalance) {
		
		accounts = new int[numAccounts];
		for (int i = 0; i < accounts.length; i++) {
			accounts[i] = initialBalance;
		}
		
		this.usesLocking = usesLocking;
		
		lockManager = new LockManager();
	}
	
	
	// Set the balance of an account
	public void setBalance(Transaction trans, int accountID, int newBalance) {
		
		if (usesLocking) {
			// Lock account with writing-level permissions
			lockManager.setLock(accounts[accountID], trans, new LockType(true));
		}
		
		accounts[accountID] = newBalance;
	}
	
	
	// Get the balance of an account
	public int getBalance(Transaction trans, int accountID) {
		
		if (usesLocking) {
			// Lock account with read-level permissions
			lockManager.setLock(accounts[accountID], trans, new LockType(false));
		}
		
		return accounts[accountID];
	}
	
	
	// Get the LockManager instance
	public LockManager getLockManager() {
		return lockManager;
	}
	
	public boolean getUsesLock()
	{
		return this.usesLocking;
	}
	
}
