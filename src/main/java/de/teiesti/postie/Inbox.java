package de.teiesti.postie;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

// TODO Inbox does not need to be closed?!
public class Inbox implements Runnable {

	private JsonReader in;
	private BlockingQueue<Object> inbox = new LinkedBlockingDeque<>();

	private Type letterType;

	private Gson gson = new Gson();

	public Inbox(BufferedReader in, Type letterType) {
		if (in == null)
			throw new IllegalArgumentException("in == null");
		if (letterType == null)
			throw new IllegalArgumentException("letterClass == null");

		this.in = new JsonReader(in);
		this.letterType = letterType;

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
		Object letter;
		while(true) {
			letter = gson.fromJson(in, letterType);
			try {
				inbox.put(letter);
			} catch (InterruptedException e) {
				Logger.error(e);
				System.exit(1);
			}
		}
	}
}
