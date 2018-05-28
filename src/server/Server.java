package server;

import handlers.Edit;
import handlers.EditManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import debug.Debug;



public class Server {
	private static final boolean DEBUG = Debug.DEBUG;
	private final Map<String, StringBuffer> documentMap;
	private final Map<String, Integer> documentVersionMap;
	private ServerSocket serverSocket;
	private ArrayList<OurThreadClass> threadList;
	private ArrayList<String> usernameList;
	private final EditManager editManager;

	
	public Server(int port, Map<String, StringBuffer> documents,
			Map<String, Integer> version) {
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Server created. Listening on port " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		documentMap = Collections.synchronizedMap(documents);
		threadList = new ArrayList<OurThreadClass>();
		documentVersionMap = Collections.synchronizedMap(version);
		usernameList = new ArrayList<String>();
		editManager = new EditManager();
	}


	public void serve() {
		while (true) {
			try {
				
				Socket socket = serverSocket.accept();
				
				OurThreadClass t = new OurThreadClass(socket, this);
				threadList.add(t);
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	public synchronized Map<String, StringBuffer> getDocumentMap() {
		return documentMap;

	}
	
	public synchronized boolean nameIsAvailable(String name){
		return !usernameList.contains(name);
	}
	
	public synchronized void addUsername(OurThreadClass t, String name){
		usernameList.add(name);
	}

	
	public synchronized Map<String, Integer> getDocumentVersionMap() {
		return documentVersionMap;

	}


	public synchronized String getAllDocuments() {
		String documentNames = "";
		for (String key : documentMap.keySet()) {
			documentNames += " " + key;
		}
		return documentNames;
	}

	public synchronized String manageEdit(String documentName, int version,
			int offset) {
		return editManager.manageEdit(documentName, version, offset);
	}

	
	public synchronized boolean documentMapisEmpty() {
		return documentMap.isEmpty();
	}

	
	public synchronized boolean versionMapisEmpty() {
		return documentVersionMap.isEmpty();
	}

	
	public synchronized void logEdit(Edit edit) {
		editManager.logEdit(edit);
	}

	
	public synchronized void removeThread(OurThreadClass t) {
		if (DEBUG) {
			System.out.println("removing thread from threadlist");
		}
		usernameList.remove(t.getUsername());
		threadList.remove(t);
	}

	
	public synchronized void addNewDocument(String documentName) {
		documentMap.put(documentName, new StringBuffer());
		documentVersionMap.put(documentName, 1);
		editManager.createNewlog(documentName);

	}

	
	public synchronized void updateVersion(String documentName, int version) {
		documentVersionMap.put(documentName, version);
	}

	
	public synchronized int getVersion(String documentName) {
		return documentVersionMap.get(documentName);
	}

	
	public synchronized void delete(String documentName, int offset,
			int endPosition) {
		if (offset < 0 || endPosition < 1) {
			throw new RuntimeException("invalid args");
		}
		documentMap.get(documentName).delete(offset, endPosition);
	}

	
	public synchronized void insert(String documentName, int offset, String text) {
		documentMap.get(documentName).insert(offset, text);
	}

	public synchronized String getDocumentText(String documentName) {
		String document = "";
		document = documentMap.get(documentName).toString();
		return document;
	}

	
	public synchronized int getDocumentLength(String documentName) {
		return documentMap.get(documentName).length();
	}

	
	public void returnMessageToEveryOtherClient(String message,
			OurThreadClass thread) {
		for (OurThreadClass t : threadList) {
			if (!thread.equals(t) && !t.getSocket().isClosed()) {
				
				PrintWriter out;
				if (t.getSocket().isConnected()) {
					synchronized (t) {
						try {
							
							out = new PrintWriter(t.getSocket()
									.getOutputStream(), true);
							out.println(message);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

}