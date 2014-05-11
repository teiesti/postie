package de.teiesti.postie.recipients;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

public class Mailbox<Letter> implements Recipient<Letter> {

	@Override
	public void accept(Letter letter, Postman postman) {
		// TODO
	}

}
