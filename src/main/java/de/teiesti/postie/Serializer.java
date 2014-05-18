package de.teiesti.postie;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * A {@link Serializer} encodes {@link Letter} into a {@link String} or vice versa.
 *
 * @param <Letter> type of the letters
 */
public interface Serializer<Letter> {

	/**
	 * Encodes a given {@link Letter} into a {@link String}.
	 *
	 * @param letter the {@link Letter} to encode
	 * @return the encodes {@link Letter} as {@link String}
	 */
	public String encode(Letter letter);

	/**
	 * Encodes given {@link Letter} into a {@link String} and writes it to {@link Writer}. Afterwards a separator is
	 * written: A separator is what {@link #separator()} returns.
	 *
	 * @param writer the {@link Writer}
	 * @param letter the {@link Letter}
	 *
	 * @throws IOException if there is a problem with the {@link Writer}
	 */
	public void encodeNext(Writer writer, Letter letter) throws IOException;

	/**
	 * Decodes a given {@link String} into a {@link Letter}.
	 *
	 * @param letter the {@link String} to decode
	 * @return the decoded {@link Letter}
	 */
	public Letter decode(String letter);

	/**
	 * Reads a {@link String} from a {@link Reader} and decodes it into a {@link Letter}. Two raw {@link Letter}s
	 * must be separated with a separator: A separator is what {@link #separator()} returns.
	 *
	 * @param reader the {@link Reader}
	 * @return the {@link Letter} that was read and decoded
	 *
	 * @throws IOException if there is a problem with {@link Reader}
	 */
	public Letter decodeNext(Reader reader) throws IOException;

	/**
	 * Returns a {@link String} that separates two string-encodes {@link Letter}s.
	 * @return the separator string
	 */
	public String separator();

}
