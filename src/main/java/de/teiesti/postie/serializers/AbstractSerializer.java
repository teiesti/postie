package de.teiesti.postie.serializers;

import de.teiesti.postie.Serializer;
import de.teiesti.postie.serializers.matcher.KnuthMorrisPrattMatcher;
import de.teiesti.postie.serializers.matcher.Matcher;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public abstract class AbstractSerializer<Letter> implements Serializer<Letter> {

	private Matcher matcher;

	public AbstractSerializer() {
		matcher = new KnuthMorrisPrattMatcher();
		matcher.initialize(separator());
	}

	public void encodeNext(Writer writer, Letter letter) throws IOException {
		String rawLetter = encode(letter);
		writer.write(rawLetter);
		writer.write(separator());
	}

	public Letter decodeNext(Reader reader) throws IOException {
		StringBuilder rawLetter = new StringBuilder();

		for (int c = reader.read(); c != -1; c = reader.read()) {
			rawLetter.append((char) c);
			if (matcher.feed((char) c)) {
				rawLetter.delete(rawLetter.length() - separator().length(), rawLetter.length());
				matcher.reset();
				break;
			}
		}

		return decode(rawLetter.toString());
	}

}
