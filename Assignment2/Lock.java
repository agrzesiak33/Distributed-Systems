



import java.util.HashSet;
import java.util.Vector;

public class Lock {
	private Object object;
	private Vector<Transaction> holders;
	private LockType lockType;
	private HashSet<Transaction> lockRequesters;
	
	public Lock(Object object) {
		this.object = object;
		holders = new Vector<Transaction>();
		lockType = null;
		lockRequesters = new HashSet<Transaction>();
	}


	// Acquires a lock
	public synchronized void acquire(Transaction trans, LockType aLockType) {
		while(isConflicting(aLockType)) {
			lockRequesters.add(trans);
			try {
				wait();
			} catch(InterruptedException e) {}
		}
		lockRequesters.remove(trans);
		if (holders.isEmpty()) {
			holders.addElement(trans);
			lockType = aLockType;
		} else if (!holders.isEmpty()) {
			if (!holders.contains(trans)) {
				holders.addElement(trans);
			}
		} else if (aLockType.isWrite && !lockType.isWrite) { // Why is this necessary? Isn't a write-lock caught in  if(holders.isEmpty())?
			lockType.promote();
		}
	}
	
	
	// Releases a lock
	public synchronized void release(Transaction trans) {
		holders.removeElement(trans);
		if (holders.isEmpty()) {
			lockType = null; // ?
		}
		notifyAll();
	}
	
	
	// Helper method for acquire()
	private boolean isConflicting(LockType requestedLockType) {
		// No one holds the lock
		if (lockType == null || holders.isEmpty()) {
			return false;
		}
		
		// Both want to read
		else if (!lockType.isWrite && !requestedLockType.isWrite) {
			return false;
		}
		
		// Either the current holder of the lock or the lock requester wants to write
		else {
			return true;
		}
	}
	
	// Gets the locked object
	public Object getObject() {
		return object;
	}
	
	// Gets the lock requesters
	public HashSet<Transaction> getLockRequesters() {
		return lockRequesters;
	}	
	
	// Gets the lock holders
	public Vector<Transaction> getHolders() {
		return holders;
	}
}
