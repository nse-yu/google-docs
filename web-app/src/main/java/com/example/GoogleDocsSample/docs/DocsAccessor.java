package com.example.GoogleDocsSample.docs;

import com.example.GoogleDocsSample.auth.GoogleAuth;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocsAccessor {

    public interface SearchCallback{
        void onSearchCompleted(List<SearchResult> results);
    }

    private final DocsRepository    repository;
    private final DocsAccessor.SearchCallback callback;
    private final GoogleAuth auth;


    public DocsAccessor(DocsAccessBuilder builder) {

        if(builder.getDocs() == null)
            throw new RuntimeException("This builder is not valid status.");

        callback    = builder.getCallback();
        repository  = new DocsRepository(builder.getDocs());

        try {

            this.auth = new GoogleAuth(GoogleNetHttpTransport.newTrustedTransport());

        } catch (GeneralSecurityException | IOException e) {

            throw new RuntimeException(e);

        }

    }

    public boolean  isAuthorized() {

        return auth.isAuthorized();

    }

    public String   redirectURI(){

        return auth.redirectURI();

    }

    public TokenResponse requestNewToken(final String code){

        try {

            return auth.requestNewToken(code);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    public void authorize(final TokenResponse tokenResponse){

        Credential credential;

        try {

            credential = auth.authorize(tokenResponse);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        System.out.println("Obtained Access Token: " + credential.getAccessToken());

    }

    public Document create(
            final String title,
            final String bodyString
    ) throws IOException {

        return repository.create(title, bodyString, auth.getAuthorizedCredential());

    }

    public BatchUpdateDocumentResponse batchUpdate(final Document doc, final String bodyString) throws IOException {

        return repository.batchUpdate(doc, bodyString, auth.getAuthorizedCredential());

    }

    public Document get(final String documentId, boolean isLog) throws IOException {

        return repository.get(documentId, auth.getAuthorizedCredential(), isLog);

    }
}

class DocsRepository{

    private final Docs docService;

    public DocsRepository(Docs docService) {

        this.docService = docService;

    }

    private Document    document(final String title){

        return new Document().setTitle(title);
    }

    public Document     create(
            final String title,
            final String bodyString,
            final Credential credential
    ) throws IOException {

        Document doc;

        Docs.Documents.Create create =
                docService.documents().create(document(title));
        create.setAccessToken(credential.getAccessToken());

        doc = create.execute();

        // failed to create a document
        if(doc == null)
            return null;

        // completed creating documents
        System.out.printf("Document [%s] is successfully created.\n", doc.getDocumentId());


        // insert content string
        //get(doc.getDocumentId(), credential, false);

        BatchUpdateDocumentResponse response = batchUpdate(doc, bodyString, credential);

        return get(response.getDocumentId(), credential, false);

    }

    public BatchUpdateDocumentResponse batchUpdate(
            final Document doc,
            final String bodyString,
            final Credential credential
    ) throws IOException {

        // multiple request layer
        List<Request> requests = new ArrayList<>();

        int[] docRange = getBodyRange(doc);

        System.out.println(Arrays.toString(docRange));


        if(docRange[0] != 1 || docRange[1] != 1)
            requests.add(new Request().setDeleteContentRange(
                    new DeleteContentRangeRequest()
                            .setRange(new Range()
                                    .setStartIndex(docRange[0])
                                    .setEndIndex(docRange[1])
                            )
            ));

        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setText(bodyString)
                .setLocation(new Location().setIndex(1))
        ));

        BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);

        return docService.documents()
                .batchUpdate(doc.getDocumentId(), body)
                .setAccessToken(credential.getAccessToken())
                .execute();
    }


    public Document get(final String documentId, final Credential credential, boolean isLog) throws IOException {

        Document doc;

        Docs.Documents.Get get = docService.documents().get(documentId);
        get.setAccessToken(credential.getAccessToken());

        doc = get.execute();

        if(!isLog) return doc;

        doc.getBody().getContent().forEach(
                seg -> {
                    System.out.printf("[StructuralElement(size=%d) start: %d, end: %d] has ", seg.size(), seg.getStartIndex(), seg.getEndIndex());
                    System.out.print(seg.keySet().stream().reduce("", (pre, next) -> pre + " " + next).trim());
                    System.out.println(
                            seg.containsKey("paragraph") ?
                                    String.format(" -> size(%d) text(%s)" ,seg.size(), seg.getParagraph().getElements().get(0).getTextRun())
                                    :
                                    ""
                    );
                }
        );

        return doc;

    }

    private int[]   getBodyRange(final Document doc){

        List<StructuralElement> contents = doc.getBody().getContent();

        return new int[]{
                contents.get(1).getStartIndex(),
                contents.get(contents.size() - 1).getEndIndex() - 1
        };

    }

}
