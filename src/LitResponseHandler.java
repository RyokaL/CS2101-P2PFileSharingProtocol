import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.imageio.ImageIO;

public class LitResponseHandler extends Thread {

	private String requestType;
	private Socket conn;
	private DataInputStream connIn;
	private DataOutputStream connOut;
	private DateFormat modFormat = new SimpleDateFormat("HH:mm:ss E dd/LL/yyyy");
	private String filePath;
	private String downloadDir;
	
	private static volatile ArrayList<ConnectedClient> multiClients = new ArrayList<ConnectedClient>();
	
	private static final String VERSION = "🥇";
	private static final int PORT = 42069;
	
	/**
	 * Initialises connection to the client
	 * @param requestType Type of request requested
	 * @param hostName The client to connect to
	 * @param downloadDir Directory to save files into
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public LitResponseHandler(String requestType, String hostName, String downloadDir) throws UnknownHostException, IOException {
		this.requestType = requestType;
		this.downloadDir = downloadDir;
		conn = new Socket(hostName, PORT);
		
	
		connIn = new DataInputStream(conn.getInputStream());
		connOut = new DataOutputStream(conn.getOutputStream());
	}
	
	/**
	 * See {@link LitResponseHandler(String requestType, String hostName, String downloadDir)}
	 * @param multiClients Reference to list of clients to populate names
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public LitResponseHandler(String requestType, String hostName, ArrayList<ConnectedClient> multiClients, String downloadDir) throws UnknownHostException, IOException {
		this(requestType, hostName, downloadDir);
		LitResponseHandler.multiClients = multiClients;
	}
	
	/**
	 * See {@link LitResponseHandler(String requestType, String hostName, String downloadDir)}
	 * @param filePath File requested
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public LitResponseHandler(String requestType, String hostName, String filePath, String downloadDir) throws UnknownHostException, IOException {
		this(requestType, hostName, downloadDir);
		this.filePath = filePath;
	}
	
	public void run() {
		switch (requestType) {
		case "list":
			getFileList();
			break;
		case "get":
			getFile(filePath);
			break;
		case "thumb":
			requestThumbnail();
			break;
		case "send":
			requestSendPerm(filePath);
			break;
		case "delet":
			deleteFile(filePath);
			break;
		case "alias":
			getAlias();
			break;
		}
		cleanUp();
		return;
	}
	
	/**
	 * Closes input/output streams and closes the connection
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
	 * Sends request header to client
	 * @param request Request header
	 */
	public void sendRequest(String request) {
		byte[] buf = request.getBytes();
		try {
			connOut.write(buf, 0, buf.length);
		} catch (IOException e) {
			//Logging
		}
	}
	
	/**
	 * Sends a request to list the files of the server
	 */
	public void getFileList() {
		sendRequest(VERSION + "📁\r\n\r\n");
			if(validHeader() != -1) {
				String nextFile;
				System.out.format("%32s%16s%32s%n", "Name", "Size", "Last Modified");
				while (((nextFile = streamReadLine()) != null) && !(nextFile.isEmpty())) {
					String[] fileOutput = nextFile.split("\0");
					System.out.format("%32s%16s%32s%n", fileOutput[0], fileOutput[1] + "B", modFormat.format(new Date(Long.parseLong(fileOutput[2]))));
				}
			}
			else {
				System.out.println("No files available!");
			}
	}
	
	/**
	 * Checks if the header is valid
	 * @return Size of data if valid, -1 if invalid. 0 is special case if header is only informational
	 */
	public int validHeader() {
		String size;
		try {
		if(streamReadLine().contains("👌")) {
			if((size = streamReadLine()).contains("⚖⚖")) {
				if(streamReadLine().isEmpty()) {
					try {
						return Integer.parseInt(size.substring(2));	
					} 
					catch (NumberFormatException e) {
						return 0;
					}
				}
			}
		}
		} catch (Exception e) {
			//Logging
		}
		return -1;
	}
	
	/**
	 * Reads a line of the input stream (used for reading the header and file list). This consumes the input stream.
	 * @return Next line of input stream
	 */
	public String streamReadLine() {
		String newLine = "";
		byte[] buf = new byte[250];
		int bufLength = 0;
		
		try {
			while((buf[bufLength] = connIn.readByte()) != 10) {
				bufLength++;
			}
			newLine = new String(buf, 0, bufLength - 1);
			return newLine;
			
		} catch (EOFException e) {
			return null;
		}
		catch (IOException e) {
			//Logging
		}
		return null;
	}
	
	/**
	 * Requests to download file from server and saves in specified download directory
	 * @param filePath The file to download from remote server
	 */
	public void getFile(String filePath) {
		sendRequest(VERSION + "📥" + filePath + "\r\n\r\n");
		File download = new File(downloadDir + filePath);
		int fileSize;
		fileSize = validHeader();
		if(fileSize != -1) {
			byte[] fileBuf = new byte[fileSize];
			try {
				connIn.readFully(fileBuf, 0, fileBuf.length);
				FileOutputStream downOut = new FileOutputStream(download);
				downOut.write(fileBuf, 0, fileBuf.length);
				downOut.close();
				System.out.println("File downloaded successfully!");
			} catch (IOException e) {
				System.out.println("Error downloading file!");
			}
		}
		else {
			System.out.println("Error downloading file! Make sure the file name is correct! Type 'list' <name> for a list of files");
		}
	}
	
	/**
	 * Not implemented
	 */
	public void requestThumbnail() {
		//Not implemented for CLI
	}
	
	/**
	 * Request the server to request this server for the specified file
	 * @param filePath File on this server to send
	 */
	public void requestSendPerm(String filePath) {
		sendRequest(VERSION + "📤" + filePath + "\r\n\r\n");
	}
	
	/**
	 * Deletes a file from the remote server
	 * @param filePath File to delete
	 */
	public void deleteFile(String filePath) {
		sendRequest(VERSION + "🗑" + filePath +"\r\n\r\n");
		if(validHeader() == -1) {
			System.out.println("WARNING: File may not have been deleted!");
		} else {
			System.out.println("File deleted successfully!");
		}
	}
	
	/**
	 * Gets the friendly name for all clients connected
	 */
	public void getAlias() {
		sendRequest(VERSION + "🖐" +"\r\n\r\n");
		int nameSize;
		if((nameSize = validHeader()) != -1) {
			byte[] nameBuf = new byte[nameSize];
			for(int i = 0; i < nameSize; i++) {
				try {
					nameBuf[i] = connIn.readByte();
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
			}
			String name = new String(nameBuf);
			for(ConnectedClient client : multiClients) {
				if(client.getAddress().equals(conn.getInetAddress().getHostAddress())) {
					client.setNewName(name);
					System.out.println(client.getName() + "     " + client.getAddress());
					break;
				}
			}
		} else {
			sendRequest(VERSION + "😠");
		}
	}
}