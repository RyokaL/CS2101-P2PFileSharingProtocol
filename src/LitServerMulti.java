import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;

public class LitServerMulti extends Thread{
	private MulticastSocket multiLits;
	private InetAddress litGroup;
	private volatile ArrayList<ConnectedClient> multiClients = new ArrayList<ConnectedClient>();
	
	private Thread discThread;
	private Thread disconnectClients;
	
	private static final String AERIAL = "📡";
	private static final int MULTIPORT = 42070;
	private static final String MULTIADDRESS = "224.69.69.69";
	
	/**
	 * Sets up the discovery and disconnect handling threads.
	 */
	public LitServerMulti() {
		try {
			litGroup  = InetAddress.getByName(MULTIADDRESS);
			multiLits = new MulticastSocket(MULTIPORT);
			multiLits.joinGroup(litGroup);
		} catch (IOException e) {
			//Logging
			return;
		}
		
		discThread = new Thread("Discovery") {
			public void run() {
				while(true) {
					DatagramPacket discover = new DatagramPacket(AERIAL.getBytes(), AERIAL.getBytes().length, litGroup, MULTIPORT);
					try {
						multiLits.send(discover);
					} catch (IOException e) {
						continue; //TODO Break if occurs number of times in succession?
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		};
		discThread.start();
		
		disconnectClients = new Thread("Disconnect") {
			public void run() {
				while (true) {
					Iterator<ConnectedClient> iter = multiClients.iterator();
					while (iter.hasNext()){
						ConnectedClient client = iter.next();
						if((new Date().getTime() - client.getConnTime().getTime()) > 10000) {
							iter.remove();
							break;
						}
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		};
		disconnectClients.start();
	}
	@Override
	public void run() {
		while(!(Thread.currentThread().isInterrupted())) {
			byte[] buf = new byte[AERIAL.getBytes().length];
			DatagramPacket getResponse = new DatagramPacket(buf, buf.length);
			try {
				multiLits.receive(getResponse);
				if(getResponse.getLength() == 0 || getResponse.getAddress().equals(InetAddress.getLocalHost())) {
					continue;
				} else {
					String response = new String(getResponse.getData());
					if(response.equals(AERIAL)) {
						addToIp(getResponse.getAddress().getHostAddress(), new Date());
					}
				}
			} 
			catch (IOException e) {
				//Logging?
				//TODO Break if occurs number of times in succession?
			}
		}
	}
	
	/**
	 * Adds a new ConnectedClient object to the array or updates the last known connection time
	 * @param newIp IP of the new client
	 * @param connTime Time of received packet
	 */
	public synchronized void addToIp(String newIp, Date connTime) {
			for(ConnectedClient client : multiClients) {
				if(client.getAddress().equals(newIp)) {
					client.setConnTime(connTime);
					return;
				}
			}
			multiClients.add(new ConnectedClient(newIp, connTime));
	}
	
	/**
	 * @return The reference to the list of connected clients
	 */
	public synchronized ArrayList<ConnectedClient> getMultiClients() {
		return multiClients;
	}
	
	/**
	 * Closes the threads when exiting the application
	 */
	public void closeDown() {
		while(discThread.isAlive()) {
			discThread.interrupt();
		}
		while(disconnectClients.isAlive()) {
			disconnectClients.interrupt();
		}
	}
}
