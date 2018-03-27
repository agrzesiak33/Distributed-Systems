



public class LockType {

	public boolean isWrite;
	
	public LockType(boolean isWrite) {
		this.isWrite = isWrite;
	}
	
	public void promote() {
		isWrite = true;
	}
	
	public void demote() {
		isWrite = false;
	}
	
}