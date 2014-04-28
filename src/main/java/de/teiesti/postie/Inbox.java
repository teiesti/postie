package de.teiesti.postie;

import com.google.gson.Gson;
import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

// TODO Inbox does not need to be closed?!
public class Inbox implements Runnable {

	private BufferedReader in;
	private BlockingQueue<Object> inbox = new LinkedBlockingDeque<>();

	private Class<?> letterClass;

	private Gson gson = new Gson();

	public Inbox(BufferedReader in, Class<?> letterClass) {
		if (in == null)
			throw new IllegalArgumentException("in == null");
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

		this.in = in;
		this.letterClass = letterClass;

		// TODO ensure that only one thread is started for one instance
		new Thread(this).start();
	}

	public Object receive() {
		try {
			return inbox.take();
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
		return null;	// will never be executed but is required by Java
	}

	@Override
	public void run() {
		try {
			String rawLetter = in.readLine();
			Object letter;
			while(rawLetter != null) {
				letter = gson.fromJson(rawLetter, letterClass);
				inbox.put(letter);
				rawLetter = in.readLine();
			}
		} catch (InterruptedException | IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

}
