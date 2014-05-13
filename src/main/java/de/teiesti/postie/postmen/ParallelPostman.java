package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelPostman<Letter> extends Postman<Letter> {

    private ExecutorService es = Executors.newCachedThreadPool();

    @Override
    protected Postman<Letter> deliver(Letter letter) {
        for (Recipient<Letter> r : recipients)
            es.submit(new Worker(r, letter, this));

        return this;
    }

    private class Worker implements Runnable {

        private Recipient<Letter> recipient;
        private Letter letter;
        private Postman<Letter> postman;

        public Worker(Recipient<Letter> recipient, Letter letter, Postman<Letter> postman) {
            this.recipient = recipient;
            this.letter = letter;
            this.postman = postman;
        }

        @Override
        public void run() {
            recipient.accept(letter, postman);
        }
    }

}
