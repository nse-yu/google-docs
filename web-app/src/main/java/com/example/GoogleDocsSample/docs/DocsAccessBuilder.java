package com.example.GoogleDocsSample.docs;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;

public class DocsAccessBuilder {

    private final Docs docs;
    private DocsAccessor.SearchCallback callback;
    private static final HttpTransport  HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory    JSON_FACTORY   = new GsonFactory();


    private DocsAccessBuilder(Docs docs) {
        this.docs = docs;
    }


    /*==================Builder Patterns===================*/
    public static DocsAccessBuilder init(final String applicationName){

        Docs docs = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, request -> {})
                .setApplicationName(applicationName != null ? applicationName : "docs-search-app")
                .build();

        return new DocsAccessBuilder(docs);
    }

    public DocsAccessBuilder setCallback(DocsAccessor.SearchCallback callback){

        if(docs == null)
            throw new RuntimeException("You should call DocsBuilder.init() first.");

        this.callback = callback;
        return this;

    }

    public DocsAccessor build(){

        if(this.callback == null)
            throw new RuntimeException("You should call DocsBuilder.setCallback() after called init() or setProperties().");

        return new DocsAccessor(this);
    }


    /*=================Getter and Others===================*/
    public Docs         getDocs(){ return docs; }

    public DocsAccessor.SearchCallback getCallback(){ return callback; }
}
