package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.PostmanTest;

public class SemiParallelPostmanTest extends PostmanTest {

    @Override
    public <Letter> Postman<Letter> createPostman() {
        return new ParallelPostman<>();
    }


}
