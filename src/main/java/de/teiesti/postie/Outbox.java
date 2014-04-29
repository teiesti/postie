package de.teiesti.postie;

import com.google.gson.Gson;
import org.pmw.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Outbox implements Runnable, AutoCloseable {

	private BufferedWriter out;
	private BlockingQueue<String> outbox = new LinkedBlockingDeque<>();

	private boolean close = false;

	public Outbox(BufferedWriter out) {
		if (out == null)
			throw new IllegalArgumentException("out == null");

		this.out = out;

		// TODO ensure that only one thread is started for one instance
		new Thread(this).start();
	}

	public void send(String letter) {
		if (letter == null)
			throw new IllegalArgumentException("letter == null");
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
		String letter;
		try {
			while (!close) {
				letter = outbox.take();
				out.write(letter);
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
		try {
			close = true;

			String letter = outbox.poll();
			while (letter != null) {
				out.write(letter);
				out.newLine();
			}

			// don't close out here, because it belongs to a socket which is handled from elsewhere
			out.flush();
		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

}
