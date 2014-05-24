package de.teiesti.postie.serializers;

import de.teiesti.postie.Serializer;
import de.teiesti.postie.serializers.matcher.KnuthMorrisPrattMatcher;
import de.teiesti.postie.serializers.matcher.Matcher;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * An {@link AbstractSerializer} is a {@link Serializer} that implements {@link #encodeNext(java.io.Writer, Object)} and
 * {@link #decodeNext(java.io.Reader)} by using {@link #encode(Object)}, {@link #decode(String)} and
 * {@link #separator()} in a generic way.
 *
 * @param <Letter> type of the letters
 */
public abstract class AbstractSerializer<Letter> implements Serializer<Letter> {

	private Matcher blueprint;

	/**
	 * Creates a new {@link AbstractSerializer}.
	 */
	public AbstractSerializer() {
		blueprint = new KnuthMorrisPrattMatcher();
		blueprint.initialize(separator());
	}

	@Override
	public void encodeNext(Writer writer, Letter letter) throws IOException {
		String rawLetter = encode(letter);
		writer.write(rawLetter);
		writer.write(separator());
	}

	@Override
	public Letter decodeNext(Reader reader) throws IOException {
		StringBuilder rawLetter = new StringBuilder();

		Matcher matcher = null;
		try {
			matcher = blueprint.clone();
		} catch (CloneNotSupportedException e) {
			Logger.error(e);
			System.exit(1);
		}

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
