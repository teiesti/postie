package de.teiesti.postie.serializers;

import com.google.gson.Gson;

/**
 * A {@link GsonSerializer} is a {@link de.teiesti.postie.Serializer} that encodes into respectively decodes from a
 * JSON string. Therefore it uses the GSON library.
 *
 * @param <Letter> type of the letters
 */
public class GsonSerializer<Letter> extends AbstractSerializer<Letter> {

	private Gson gson = new Gson();
	private Class<Letter> letterClass;

	/**
	 * Creates a new {@link GsonSerializer} that works on objects that instantiate the given {@link Class}. {@code
	 * letterClass} must fit with {@link Letter}. Otherwise this {@link GsonSerializer} will not work correctly.
	 *
	 * @param letterClass the {@link Class} of the letters
	 */
	public GsonSerializer(Class<Letter> letterClass) {
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

		this.letterClass = letterClass;
	}

	/**
	 * Encodes the given {@link Letter} into a JSON string. This method uses the GSON library to do so.
	 *
	 * @param letter the {@link Letter} to encode
	 * @return the {@link Letter} as JSON string
	 */
	@Override
	public String encode(Letter letter) {
		return gson.toJson(letter);
	}

	/**
	 * Decodes a given JSON string into a {@link Letter}. This method uses the GSON library.
	 *
	 * @param letter the {@link String} to decode
	 * @return the {@link Letter}
	 */
	@Override
	public Letter decode(String letter) {
		return gson.fromJson(letter, letterClass);
	}

	/**
	 * Returns a separator. A {@link GsonSerializer} uses a single LF ("line feed") to separate different letters
	 * within a character stream.
	 *
	 * @return a separator string
	 */
	@Override
	public String separator() {
		return Character.toString('\n');
	}

}
