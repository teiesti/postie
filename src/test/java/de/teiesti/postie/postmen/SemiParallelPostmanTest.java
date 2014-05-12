package de.teiesti.postie.postmen;

import de.teiesti.postie.PostmanTest;

public class SemiParallelPostmanTest extends PostmanTest {

    @Override
    public <Letter> de.teiesti.postie.Postman<Letter> createPostman() {
        return new SemiParallelPostman<>();
    }


}
