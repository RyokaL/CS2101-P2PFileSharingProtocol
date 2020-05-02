import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class LitServerMain extends Thread {
	
	private static final int PORT = 42069;
	
	private ServerSocket litRequestListen;
	private String sharedDirectory;
	
	/**
	 * @param shared The specified shared directory
	 * @throws IOException If the server cannot be bound to port
	 */
	public LitServerMain(String shared) throws IOException {
		litRequestListen = new ServerSocket(PORT);
		this.sharedDirectory = shared;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (!(Thread.currentThread().isInterrupted())) {
			Socket nextRequest = new Socket();
			try {
				nextRequest = litRequestListen.accept();
				LitRequestHandler requestHandler = new LitRequestHandler(nextRequest, sharedDirectory);
				requestHandler.start();
			} catch (IOException e) {
				//Logging?
				//Number of attempts & break?
			}
		}
	}
}
