package server;

import handlers.Edit;
import handlers.Edit.Type;
import handlers.Encoding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import debug.Debug;


public class OurThreadClass extends Thread {

	private static final boolean DEBUG = Debug.DEBUG;
	final Socket socket;
	private boolean alive;
	private String username;
	private final Server server;
	private final String regex = "(bye)|(new [\\w\\d]+)|(look)|(open [\\w\\d]+)|(change .+)|(name [\\w\\d]+)";
	private final String error1 = "Error: Document already exists.";
	private final String error2 = "Error: No such document.";
	private final String error3 = "Error: No documents exist yet.";
	private final String error4 = "Error: Insert at invalid position.";
	private final String error5 = "Error: You must enter a name when creating a new document.";
	private final String error6 = "Error: Invalid arguments";
	private final String error7 = "Error: Username is not available";
	private final boolean sleep = false; 

	
	public OurThreadClass(Socket socket, Server server) {
		this.socket = socket;
		this.server = server;
		this.alive = true;
	}

	
	public void run() {
		try {
			handleConnection(socket);
		} catch (IOException e) {
		}
	}

	
	
	private void handleConnection(Socket socket) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

		try {
			for (String line = in.readLine(); line != null; line = in
					.readLine()) {
				String output = handleRequest(line);
				if (sleep) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (output != null && output.equals("bye")) {
					out.close();
					in.close();
					server.removeThread(this);

				}

				
				else if (output != null && output.startsWith("change")) {
					server.returnMessageToEveryOtherClient(output, this);
				}

				
				if (output != null) {
					out.println(output);
				}
			}
		} finally {
			out.close();
			in.close();
		}
	}

	
	

	public String handleRequest(String input) {
		if (!alive) {
			throw new RuntimeException(
					"Should not get here since the client already disconnects.");
		}
		String returnMessage = "";
		input = input.trim();
		String[] tokens = input.split(" ");
		if (DEBUG) {
			System.out.println("Input message from the client is " + input);
		}
		if (!input.matches(regex)) {
			
			if (tokens.length == 1 && tokens[0].equals("new")) {
				return error5;
			} else {
				return error6;
			}
		} else {
			if (tokens[0].equals("bye")) {
				
				alive = false;
				returnMessage = "bye";

			} else if (tokens[0].equals("new")) {
				String documentName = tokens[1];
				if (DEBUG) {
					System.out.println("creating new document");
				}
				if (server.getDocumentMap().containsKey(documentName)) {
					returnMessage = error1;
				} else {
					server.addNewDocument(documentName);
					returnMessage = "new " + documentName;
				}
			}else if(tokens[0].equals("name")){
				if (DEBUG){System.out.println(tokens[1]);}
				if(server.nameIsAvailable(tokens[1])){
					this.username = tokens[1];
					server.addUsername(this, tokens[1]);
					returnMessage = "name "+tokens[1];
				}
				else{
					returnMessage = error7;
				}

			} else if (tokens[0].equals("look")) {
				
				String result = "alldocs";
				if (server.documentMapisEmpty()) {
					returnMessage = error3;
				} else {
					result = result + server.getAllDocuments();
					returnMessage = result;
				}

			} else if (tokens[0].equals("open")) {
				String documentName = tokens[1];
				if (!server.getDocumentMap().containsKey(documentName)
						|| !server.getDocumentVersionMap().containsKey(
								documentName)) {
					returnMessage = error2;
				} else {
					int version = server.getVersion(documentName);
					String documentText = Encoding.encode(server
							.getDocumentText(documentName));
					returnMessage = "open " + documentName + " " + version
							+ " " + documentText;
				}

			} else if (tokens[0].equals("change")) {
				int version = Integer.parseInt(tokens[3]);
				int offset, changeLength;
				Edit edit;
				String documentName = tokens[1];
				String editType = tokens[4];
				String username = tokens[2];
				if (!server.getDocumentMap().containsKey(documentName) || !server.getDocumentVersionMap().containsKey(
						documentName)) {
					returnMessage = error2;
				} else {
					Object lock = new Object();
					
					synchronized (lock) {
						if (server.getVersion(documentName) != version) {
							if(editType.equals("insert")){
								offset = Integer.parseInt(tokens[6]);
							}
							else{offset = Integer.parseInt(tokens[5]);}
							String updates = server.manageEdit(documentName,version, offset);
							String[] updatedTokens = updates.split(" ");
							version = Integer.parseInt(updatedTokens[1]);
							offset = Integer.parseInt(updatedTokens[2]);
						} 
						int length = server.getDocumentLength(documentName);
						if (editType.equals("remove")) {
							offset = Integer.parseInt(tokens[5]);
							int endPosition = Integer.parseInt(tokens[6]);
							server.delete(documentName, offset, endPosition);
							changeLength = offset - endPosition; 
							edit = new Edit(documentName, Type.REMOVE, "",
									version, offset, changeLength);
							server.logEdit(edit);
							server.updateVersion(documentName, version + 1);
							returnMessage = createMessage(documentName, username,
									version + 1, offset, changeLength,
									Encoding.encode(server
											.getDocumentText(documentName)));
						} else if (editType.equals("insert")) {
							Type type = Type.INSERT;
							offset = Integer.parseInt(tokens[6]);
							String text = Encoding.decode(tokens[5]);
							if (offset > length) {
								returnMessage = error4;
							} else {
								server.insert(documentName, offset, text);
								changeLength = text.length();
								edit = new Edit(documentName, type, text,
										version, offset, changeLength);
								server.logEdit(edit);
								server.updateVersion(documentName, version + 1);
								returnMessage = createMessage(documentName, username,
										version + 1, offset, changeLength,
										Encoding.encode(server.getDocumentText(documentName)));
							}
						}
					}
				}
			}
		}
		return returnMessage;
	}
	
	

	private String createMessage(String documentName, String username, int version, int offset,
			int changeLength, String documentText) {
		String message = "change " + documentName + " " +username+" "+ version + " "
				+ offset + " " + changeLength + " " + documentText;
		return message;
	}
	
	
	
	public Socket getSocket() {
		return socket;
	}
	
	
	public String getUsername() {
		return username;
	}


	public boolean getAlive() {
		return alive;
	}
}