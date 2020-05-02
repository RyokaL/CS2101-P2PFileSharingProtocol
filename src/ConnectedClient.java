import java.util.Date;

public class ConnectedClient {

	private String address;
	private String name = "";
	private Date connTime;
	
	/**
	 * @param address IP address of connected client
	 * @param connTime Time the client was last connected
	 */
	public ConnectedClient(String address, Date connTime) {
		this.address = address;
		this.connTime = connTime;
	}
	
	/**
	 * See {@link ConnectedClient(String address, Date connTime)}
	 * @param name Friendly name for client
	 */
	public ConnectedClient(String address, Date connTime, String name) {
		this(address, connTime);
		this.name = name;
	}
	
	/**
	 * @return IP address
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @return Friendly name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return Last connection time
	 */
	public Date getConnTime() {
		return connTime;
	}
	
	/**
	 * @param newTime New connection time
	 */
	public void setConnTime(Date newTime) {
		connTime = newTime;
	}
	
	/**
	 * @param newName New friendly name
	 */
	public void setNewName(String newName) {
		name = newName.replaceAll(" ", "_");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj.getClass() == getClass()) {
			ConnectedClient connCliObj = (ConnectedClient)obj;
			if(connCliObj.getAddress().equals(getAddress())) {
				return true;
			}
		}
		return false;
	}
}
