package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

public class SequentialPostman<Letter> extends Postman<Letter> {

    @Override
    protected Postman<Letter> deliver(Letter letter) {
        for (Recipient r : recipients)
            r.accept(letter, this);

        return this;
    }

}
