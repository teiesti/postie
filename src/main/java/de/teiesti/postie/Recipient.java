package de.teiesti.postie;

public interface Recipient<Letter> {

	public void accept(Letter letter);

}
