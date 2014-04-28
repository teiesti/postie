package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;

public class Postman implements AutoCloseable {

	private Inbox inbox;
	private Outbox outbox;

	private Socket socket;

	public Postman(Socket socket, Type letterType) {
		if (socket == null)
            throw new IllegalArgumentException("socket == null");
        // TODO check more about the socket here
        if (letterType == null)
			throw new IllegalArgumentException("letterClass == null");

        this.socket = socket;

		try {

			int inBuffer = socket.getReceiveBufferSize();
			InputStream inStream = socket.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream), inBuffer);
			inbox = new Inbox(in, letterType);

			int outBuffer = socket.getSendBufferSize();
			OutputStream outStream = socket.getOutputStream();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream), outBuffer);
			outbox = new Outbox(out, letterType);

		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	public void send(Object letter) {
		outbox.send(letter);
	}

	public Object receive() {
		return inbox.receive();
	}

	@Override
	public void close() throws Exception {
		// inbox.close(); 	// TODO does this need to be closed???
		outbox.close();
		socket.close();
	}
}