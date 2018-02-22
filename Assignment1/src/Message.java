import java.io.Serializable;

class Message implements Serializable
	{
		/**
	 * 
	 */
	private static final long serialVersionUID = 705521131425461556L;
		int typeFlag;
		String message;
		int id;
		
		public Message(int typeFlag, String message, int id)
		{
			this.typeFlag = typeFlag;
			this.message = message;
			this.id = id;
		}
		public Message(int typeFlag, int port, int id)
		{
			this.typeFlag = typeFlag;
			this.message = Integer.toString(port);
			this.id = id;
		}
		@Override
		public String toString()
		{
			return("Flag: " + Integer.toString(this.typeFlag) + " message: " + this.message);
		}
		
	}