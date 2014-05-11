package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;

public class Office {

	private ServerSocket serverSocket;

	private Postman blueprint;

	private Set<Postman> postmen;

	private Thread acceptor;

	public Office bind(ServerSocket socket) {

	}

	public Office spawn(Postman blueprint) {

	}

	public Office start() {
		acceptor = new Acceptor();
		acceptor.start();

		return this;
	}

	public Office stop() {
		// TODO how to do this?
	}

	private class Acceptor extends Thread {

	 	@Override
		public void run() {
			Socket socket;
			Postman postman;
			while (!isInterrupted()) {
				try {
					socket = serverSocket.accept();
					postman = blueprint.clone().bind(socket).start();
					postmen.add(postman);
				} catch (IOException e) {
					Logger.error(e);
					System.exit(1);
				}
			}

		}

	}

}
