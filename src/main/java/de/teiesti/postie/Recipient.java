package de.teiesti.postie;

public interface Recipient {

	public <Letter> void accept(Letter letter);

}
