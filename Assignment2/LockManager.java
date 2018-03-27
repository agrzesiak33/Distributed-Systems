



import java.util.HashSet;
import java.util.Iterator;

public class LockManager {
	private HashSet<Lock> theLocks; // Why would you use a HashTable in this situation? What would the keys and values be?
	
	public LockManager() {
		theLocks = new HashSet<Lock>();
	}
	
	// Sets a lock on object. Creates new lock if no lock exists.
	public void setLock(Object object, Transaction trans, LockType lockType) {
		Lock foundLock;
		synchronized(this) {
			foundLock = getLockIfExists(object);
			
			// Create new lock if a lock doesn't already exist
			if (foundLock == null) {
				foundLock = new Lock(object);
				theLocks.add(foundLock);
			}
		}
		
		trans.getHeldLocks().add(foundLock);
		
		// Attempt to acquire the lock
		foundLock.acquire(trans, lockType);
	}
	
	
	// Removes all locks that a TransactionManagerWorker has
	public synchronized void unLock(Transaction trans) {
		
		for (Iterator<Lock> iterator = trans.getHeldLocks().iterator(); iterator.hasNext();) {
			Lock lock = iterator.next();
			lock.release(trans);	// Release the lock
			iterator.remove();		// Remove the lock from TransactionManagerWorker.heldLocks
			
			// Remove lock from object if there are no holders and no requesters
			if (lock.getHolders().isEmpty() && lock.getLockRequesters().isEmpty()) {
				theLocks.remove(lock);
			}
		}
	}
	
	
	// Helper method for setLock()
	private Lock getLockIfExists(Object object) {
		for (Lock lock : theLocks) {
			if (object == lock.getObject()) {
				return lock;
			}
		}
		return null;
	}
	
}
