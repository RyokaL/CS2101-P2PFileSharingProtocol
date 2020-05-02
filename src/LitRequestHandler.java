import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LitRequestHandler extends Thread {
//For incoming requests
	private String litVersion;
	private String sharedDirectory;
	private File share;
	private String fileList;
	private Socket conn;
	private BufferedReader connIn;
	private DataOutputStream connOut;
	private static final String FRIENDLY_NAME = "Bubsy";
	
	/**
	 * @param conn Currently connected socket
	 * @param sharedDirectory The directory allowed for requests
	 */
	public LitRequestHandler(Socket conn, String sharedDirectory) {
		this.conn = conn;
		this.sharedDirectory = sharedDirectory;
		share = new File(this.sharedDirectory);
		try {
			connIn = new BufferedReader(new InputStreamReader(this.conn.getInputStream()));
			connOut = new DataOutputStream(this.conn.getOutputStream());
		} catch (IOException e) {
			//Throw to GUI
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			String request = connIn.readLine();
			switch (request.substring(0, 2)) {			//For future use
			case "🥇":
				litVersion = "V1";
				break;
			case "🥈":
				litVersion = "V2";
				break;
			case "🥉":
				litVersion = "V3";
				break;
			default:
				protocolNotSupported();
				cleanUp();
			}
			
			switch (request.substring(2, 4)) {
			case "📁":
				listFiles();
				break;
			case "📥":
				sendFile(request.substring(4));
				break;
			case "💅":
				sendThumbnail(request.substring(4));
				break;
			case "📤":
				recieveFile(request.substring(4));
				break;
			case "🗑":
				deleteFile(request.substring(4));
				break;
			case "🖐":
				sendFriendlyName();
				break;
			default:
				requestNotUnderstood();
			}
		} 
		catch (IOException e) {
			protocolNotSupported();
		}
		catch (IndexOutOfBoundsException e) {
		}
		finally {
			cleanUp();
		}
	}
	
	/**
	 * Closes all open streams and the socket when finished with the request
	 */
	public void cleanUp() {
		while(!conn.isClosed()) {
			try {
				connIn.close();
				connOut.close();
				conn.close();
			} catch (IOException e) {
				continue;
			}
		}
	}
	
	/**
	 * Constructs and sends the header
	 * @param response First line of the header - protocol version and status code 
	 * @param bodySize The size in bytes of the following data
	 */
	public void responseHeader(String response, int bodySize) {
		String responseHead = response + "\r\n";
		String responseSize = "⚖⚖" + bodySize + "\r\n\r\n";
		try {
			byte[] buf = responseHead.getBytes();
			byte[] bufSize = responseSize.getBytes();
			connOut.write(buf, 0, buf.length);
			connOut.write(bufSize, 0, bufSize.length);
		} catch (IOException e) {
			//Logging
		}
	}
	
	/**
	 * Begins constructing a list of files and sends the completed list
	 */
	public void listFiles() {
		fileList = "";
		File[] dirList = share.listFiles();
		if(dirList == null) {
			requestDenied("No files");
		} 
		else {
			listDir(dirList);
			byte[] buf = fileList.getBytes();
			sendOkResponse(buf.length);
			try {
				connOut.write(buf, 0 , buf.length);
			} catch (IOException e) {
				//Logging
			}
		}
	}
	
	/**
	 * Adds to the list of files, files within directories
	 * @param dir The current parent directory
	 */
	public void listDir(File[] dir) {
		for(File f : dir) {
			if(f.isFile()) {
				fileList += f.getPath() + "\0" + f.length() + "\0" + f.lastModified() + "\r\n";
			}
			if(f.isDirectory()) {
				listDir(f.listFiles());
			}
		}
	}
	
	/**
	 * Sends the requested file back
	 * @param filePath The requested file
	 */
	public void sendFile(String filePath) {
		if(!(filePath.startsWith(sharedDirectory))) {
			filePath = sharedDirectory + filePath;
		}
		File toGet = new File(filePath);
		if(!(toGet.exists())) {
			requestDenied("File not Found!");
		} else {
			try {
				FileInputStream intoBytes = new FileInputStream(toGet);
				int byteSize = (int)toGet.length();
				byte[] fileToBytes = new byte[byteSize];
				intoBytes.read(fileToBytes);
				sendOkResponse(byteSize);
				connOut.write(fileToBytes);
				intoBytes.close();
			} catch (FileNotFoundException e) {
				requestDenied("File not Found!");
			} catch (IOException e) {
				requestDenied("Unable to access file!");
			}
		}
	}
	
	/**
	 * Not implemented - would return thumbnail for image files. Sends an angry status code
	 * @param filePath File to create thumbnail
	 */
	public void sendThumbnail(String filePath) {
		sendAngryResponse();
	}
	
	/**
	 * Creates a new response handler to send a get request for a given file
	 * @param filePath
	 */
	public void recieveFile(String filePath) {
		try {
			LitResponseHandler send = new LitResponseHandler("get", conn.getInetAddress().getHostAddress(), filePath);
			send.start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}
	
	/**
	 * Deletes the requested file
	 * @param filePath File to be deleted
	 */
	public void deleteFile(String filePath) {
		if(!(filePath.startsWith(sharedDirectory))) {
			filePath = sharedDirectory + filePath;
		}
			File toDelete = new File(filePath);
			if(!(toDelete.exists())) {
				requestDenied("File not Found!");
			} else {
				if(toDelete.delete()) {
					sendOkResponse(0);
				}
				else {
					requestDenied("Unable to delete file!");
				}
			}
		}
	
	/**
	 * Sends an OK status code
	 * @param size Size of data
	 */
	public void sendOkResponse(int size) {
		responseHeader("🥇👌", size);
	}
	
	/**
	 * Sends the identifier for this server
	 */
	public void sendFriendlyName() {
		byte[] nameBuf = FRIENDLY_NAME.getBytes();
		
		responseHeader("🥇👌", nameBuf.length);
		try {
			connOut.write(nameBuf, 0, nameBuf.length);
		} catch (IOException e) {
			//TODO
		}
	}
	
	/**
	 * Sends an angry response code
	 */
	public void sendAngryResponse() {
		responseHeader("🥇😠", 0);
	}
	
	/**
	 * Sends a request not understood response code
	 */
	public void requestNotUnderstood() {
		responseHeader("🥇🤷", 0);
	}
	
	/**
	 * Sends a protocol not supported response code
	 */
	public void protocolNotSupported() {
		responseHeader("🥇🚧", 0);
	}
	
	/**
	 * Sends a request denied as well as a reason (reason is not specified as required)
	 * @param reason Reason for denying the request
	 */
	public void requestDenied(String reason) {
		responseHeader("🥇🚫" + reason, 0);
	}
}