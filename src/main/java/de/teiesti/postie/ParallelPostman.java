package de.teiesti.postie;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelPostman<Letter> extends Postman<Letter> {

	private ExecutorService es = Executors.newCachedThreadPool();

	public ParallelPostman(Socket socket, Class<? extends Letter> letterClass) {
		super(socket, letterClass);
	}

	@Override
	protected void deliver(Letter letter) {
		for (Recipient r : getRecipients())
			es.submit(new Worker(r, letter));
	}

	private class Worker implements Runnable {

		private Recipient<Letter> recipient;
		private Letter letter;

		public Worker(Recipient<Letter> recipient, Letter letter) {
			this.recipient = recipient;
			this.letter = letter;
		}

		@Override
		public void run() {
			recipient.accept(letter);
		}
	}

}
