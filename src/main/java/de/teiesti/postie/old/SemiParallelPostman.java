package de.teiesti.postie.old;

import org.pmw.tinylog.Logger;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO doc: difference between ParallelPostman and SemiParallelPostman: the last sequentializes the letters!
public class SemiParallelPostman<Letter> extends Postman<Letter> {

	private ExecutorService es = Executors.newCachedThreadPool();
	private CountDownLatch cdl;

	public SemiParallelPostman(Socket socket, Class<? extends Letter> letterClass) {
		super(socket, letterClass);
		cdl = new CountDownLatch(0);
	}

	@Override
	protected void deliver(Letter letter) {
		try {
			cdl.await();
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}

		cdl = new CountDownLatch(getRecipients().size());

		for (Recipient r : getRecipients())
			es.submit(new Worker(r, letter, cdl));
	}

	private class Worker implements Runnable {

		private Recipient<Letter> recipient;
		private Letter letter;

		private CountDownLatch cdl;

		public Worker(Recipient<Letter> recipient, Letter letter, CountDownLatch cdl) {
			this.recipient = recipient;
			this.letter = letter;
		}

		@Override
		public void run() {
			recipient.accept(letter);
			cdl.countDown();
		}
	}

}
