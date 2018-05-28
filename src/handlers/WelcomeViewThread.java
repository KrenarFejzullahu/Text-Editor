package handlers;

import client.Client;
import debug.Debug;


public class WelcomeViewThread extends Thread {
	private static final boolean DEBUG = Debug.DEBUG;

	private final String message;
	private final Client client;
	
    
	public WelcomeViewThread(Client client, String message) {

		this.message = message;
		this.client = client;
	}
    
	public void run() {
		if (DEBUG) {System.out.println("sending message");}
		client.sendMessageToServer(message);
	}

}