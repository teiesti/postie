package de.teiesti.postie;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public interface Serializer<Letter> {

	public String encode(Letter letter);

	public void encodeNext(Writer writer, Letter letter) throws IOException;

	public Letter decode(String letter);

	public Letter decodeNext(Reader reader) throws IOException;

	public String separator();

}
