package de.teiesti.postie;

import com.google.gson.Gson;
import org.pmw.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Outbox implements Runnable, AutoCloseable {

	private BufferedWriter out;
	private BlockingQueue<Object> outbox = new LinkedBlockingDeque<>();

	private boolean close = false;

	private Class<?> letterClass;

	private Gson gson = new Gson();

	public Outbox(BufferedWriter out, Class<?> letterClass) {
		if (out == null)
			throw new IllegalArgumentException("out == null");
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

		this.out = out;
		this.letterClass = letterClass;

		// TODO ensure that only one thread is started for one instance
		new Thread(this).start();
	}

	public void send(Object letter) {
		if (letter == null)
			throw new IllegalArgumentException("letter == null");
		if (!letter.getClass().isAssignableFrom(letterClass))
			throw new ClassCastException("letter is not assignable to given letter class " + letterClass);
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
				gson.toJson(letter, out);
				out.newLine();
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
			gson.toJson(letter, letterClass, out);
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
