import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class LitClientMain {

	private static volatile ArrayList<ConnectedClient> multiClients = new ArrayList<ConnectedClient>();
	private static LitServerMulti castServer;
	private static LitClientMain lit;
	private static LitServerMain startServer;
	
	private static String downloadDirectory = "Downloads/";
	private static String sharedDirectory = "shared/";
	
	public static void main(String[] args) {
		System.out.println("Welcome to LitTorrent v1.0 my dude. Please wait whilst we light it up... 👌\u001B[31m🔥\u001B[32m🍁\u001B[0m");
		lit = new LitClientMain();
		
		try {
			sharedDirectory = args[0];
			downloadDirectory = args[1];
			
			if(!(sharedDirectory.endsWith("/"))) {
				sharedDirectory = sharedDirectory + "/";
			}
			if(!(downloadDirectory.endsWith("/"))) {
				downloadDirectory = downloadDirectory + "/";
			}
			
			File sharedDir = new File(sharedDirectory);
			File downDir = new File(downloadDirectory);
			
			if(!(sharedDir.exists())) {
				sharedDir.mkdirs();
			}
			
			if(!(downDir.exists())) {
				downDir.mkdirs();
			}
		} 
		catch (IndexOutOfBoundsException e) {
			System.out.println("Correct Usage: java -jar LitTorrent.jar <shared> <downloads>");
			return;
		}
		
		try {
			castServer = new LitServerMulti();
			castServer.start();
			multiClients = castServer.getMultiClients();
			
			startServer = new LitServerMain(sharedDirectory);
			startServer.start();
			System.out.println("For a list of commands type 'help'");
			lit.getCommands();
			return;
		} 
		catch (IOException e) {
			System.out.println("Unable to establish LitServer! Exiting LitTorrent...");
			lit.cleanExit();
		}
	}
	
	/**
	 * Command line interface
	 */
	public void getCommands() {
		BufferedReader litCommands = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			try {
				if(litCommands.ready()) {
					String command = litCommands.readLine();
					String[] commandArgs = command.split(" ");
					String friendlyName = null;
					switch (commandArgs[0].toLowerCase()) {
					case "names":
						getNames();
						break;
					case "list":
						friendlyName = commandArgs[1];
						createRequest(friendlyName, "list");
						break;
					case "get":
						friendlyName = commandArgs[1];
						createRequest(friendlyName, "get", command.substring(commandArgs[0].length() + friendlyName.length() + 2));
						break;
					case "send":
						friendlyName = commandArgs[1];
						createRequest(friendlyName, "send", command.substring(commandArgs[0].length() + friendlyName.length() + 2));
						break;
					case "delete":
						friendlyName = commandArgs[1];
						createRequest(friendlyName, "delet", command.substring(commandArgs[0].length() + friendlyName.length() + 2));
						break;
					case "help":
						litHelpMan();
						break;
					case "exit":
						cleanExit();
						System.exit(0);
					case "wiseau":
						System.out.println("\u001B[7mOh, hi Mark");
						break;
					default:
						System.out.println("Not a valid command. Type 'help' for a list of commands");
						break;
					}
				}
			} catch (IOException e) {
				System.out.println("Not a valid command. Type 'help' for a list of commands");
			} 
			catch (IndexOutOfBoundsException e) {
				System.out.println("Not a valid command. Type 'help' for a list of commands");
			}
		}
	}
	
	/**
	 * Get's the host's ip address from their friendly name
	 * @param hostName Friendly name of host
	 * @return Host's IP address
	 */
	public String getAddressFromName(String hostName) {
		for(ConnectedClient client : multiClients) {
			if(client.getName().equals(hostName)) {
				return client.getAddress();
			}
		}
		System.out.println("Not a valid name! Type 'names' for a list of names");
		return "dummy";
	}
	
	/**
	 * Formats the request specified by the user
	 * @param hostName The friendly name of the host
	 * @param type The request type
	 */
	public void createRequest(String hostName, String type) {
		LitResponseHandler sendRequest;
		try {
			sendRequest = new LitResponseHandler(type, getAddressFromName(hostName), downloadDirectory);
			sendRequest.start();
		} catch (UnknownHostException e) {
			System.out.println("Unable to resolve host! Make sure the name is spelt correctly, otherwise they may have disconnected!");
		}
		catch (IOException e) {
			System.out.println("Unable to establish connection with host!");
			return;
		}
	}
	
	/**
	 * See {@link createRequest(String hostName, String type)}
	 * @param filePath The requested file
	 */
	public void createRequest(String hostName, String type, String filePath) {
		LitResponseHandler sendRequest;
		try {
			sendRequest = new LitResponseHandler(type, getAddressFromName(hostName), filePath, downloadDirectory);
			sendRequest.start();
		} catch (UnknownHostException e) {
			System.out.println("Unable to resolve host! Make sure the name is spelt correctly, otherwise they may have disconnected!");
		}
			catch (IOException e) {
				System.out.println("Unable to establish connection with host!");
				return;
			}
	}
	
	/**
	 * Gets the friendly names for all connected clients
	 */
	public void getNames() {
		if(multiClients.size() == 0) {
			System.out.println("There are no clients connected!");
		}
		for(ConnectedClient client : multiClients) {
			if(client.getName().isEmpty()) {
				LitResponseHandler sendRequest;
				try {
					sendRequest = new LitResponseHandler("alias", client.getAddress(), multiClients, downloadDirectory);
					sendRequest.start();
				}
				catch (Exception e) {
					System.err.println("Unable to get name for host");
				}
			} 
			else {
				System.out.println(client.getName() + "     " + client.getAddress());
			}
		}
	}
	
	/**
	 * Semi-cleanly closes open threads to shutdown the program
	 */
	public void cleanExit() {
		int safeCount = 0;
		castServer.closeDown();
		while(startServer.isAlive() && castServer.isAlive() && safeCount < 500000) {
			castServer.interrupt();
			startServer.interrupt();
			safeCount += 1;
		}
		System.out.println("Bye bye!");
	}
	
	/**
	 * Prints the list of commands
	 */
	public void litHelpMan() {
		System.out.println("Names: Shows the names of connected users");
		System.out.println("List <Name>: Lists the files of the connected user");
		System.out.println("Get <Name> <File>: Downloads the file specified from the user and saves it in the downloads directory");
		System.out.println("Send <Name> <File>: Requests the user to send a get request for the specified file from this client");
		System.out.println("Delete <Name> <File>: Requests the user to delete a shared file");
		System.out.println("Help: Displays this super helpful prompt");
		System.out.println("Exit: Closes LitTorrent");
	}
}
