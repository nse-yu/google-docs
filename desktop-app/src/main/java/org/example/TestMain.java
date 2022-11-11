package org.example;

import org.example.docs.DocsAccessBuilder;
import org.example.docs.DocsAccessor;

import java.io.IOException;

public class TestMain {

    public static void main(String[] args) throws IOException {

        DocsAccessor accessor = DocsAccessBuilder
                .init("docs-app")
                .setCallback(System.out::println)
                .build();


        /*
        Document doc = accessor.create(
                "myDoc",
                "enemy"
        );

         */


        accessor.batchUpdate(
                accessor.get("1ENFP8wOUpqnozHstRYRjm0GnjjZRgGeS_90RixOJOhk", true),
                "googledocs and you are sit"
        );

    }
}