package de.teiesti.postie.old;

public interface Recipient<Letter> {

	public void accept(Letter letter);

}
