package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

// TODO doc: Inbox does not need to be closed!
// TODO doc: dont start a second thread for it
class Inbox implements Runnable {

	private BufferedReader in;
	private BlockingQueue<String> inbox = new LinkedBlockingDeque<>();

	public Inbox(BufferedReader in) {
		if (in == null)
			throw new IllegalArgumentException("in == null");

		this.in = in;

		new Thread(this).start();
	}

	public String receive() {
		try {
			return inbox.take();
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
		return null;	// will never be executed but is required by Java
	}

	public boolean hasLetter() {
		return !inbox.isEmpty();
	}

	@Override
	public void run() {
		try {
			String letter = in.readLine();
			while(letter != null) {
				inbox.put(letter);
				letter = in.readLine();
			}
		} catch (InterruptedException | IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

}
