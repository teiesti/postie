package de.teiesti.postie.recipients;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Mailbox<Letter> implements Recipient<Letter> {

    private final BlockingQueue<Letter> inbox = new LinkedBlockingQueue<>();

	@Override
	public void accept(Letter letter, Postman postman) {
       inbox.add(letter);
    }

    public Letter receive() throws InterruptedException {
        return inbox.take();
    }

}
