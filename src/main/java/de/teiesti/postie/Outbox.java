package de.teiesti.postie;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.pmw.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Outbox implements Runnable, AutoCloseable {

	private JsonWriter out;
	private BlockingQueue<Object> outbox = new LinkedBlockingDeque<>();

	private boolean close = false;

	private Type letterType;

	private Gson gson = new Gson();

	public Outbox(BufferedWriter out, Type letterType) {
		if (out == null)
			throw new IllegalArgumentException("out == null");
		if (letterType == null)
			throw new IllegalArgumentException("letterClass == null");

		this.out = new JsonWriter(out);
		this.letterType = letterType;

		// TODO ensure that only one thread is started for one instance
		new Thread(this).start();
	}

	public void send(Object letter) {
		if (close)
			throw new IllegalStateException("cannot send a letter because this is already closed");

		try {
			outbox.put(letter);
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	@Override
	public void run() {
		Object letter;
		try {
			while (!close) {
				letter = outbox.take();
				gson.toJson(letter, letterType, out);
				if (outbox.isEmpty()) out.flush();
			}
		} catch (InterruptedException | IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	@Override
	public void close() {
		close = true;

		Object letter;
		while (!outbox.isEmpty()) {
			letter = outbox.poll();
			gson.toJson(letter, letterType, out);
		}

		try {
			// don't close out here, because it belongs to a socket which is handled from elsewhere
			out.flush();
		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

}
