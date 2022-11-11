package org.example.docs;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import org.example.auth.GoogleAuth;

import javax.naming.directory.SearchResult;
import java.io.*;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;

public class DocsAccessor {

    public interface SearchCallback{
        void onSearchCompleted(List<SearchResult> results);
    }

    private final DocsRepository    repository;

    private final DocsAccessor.SearchCallback callback;

    public DocsAccessor(DocsAccessBuilder builder) {

        if(builder.getDocs() == null)
            throw new RuntimeException("This builder is not valid status.");

        callback    = builder.getCallback();

        try {

            repository  = new DocsRepository(builder.getDocs());

        } catch (GeneralSecurityException | IOException | URISyntaxException e) {

            throw new RuntimeException(e);

        }

    }

    public Document create(
            final String title,
            final String bodyString
    ){

        return repository.create(title, bodyString);

    }

    public BatchUpdateDocumentResponse batchUpdate(final Document doc, final String bodyString){

        try {

            return repository.batchUpdate(doc, bodyString);

        } catch (IOException e) {

            throw new RuntimeException("Failed to update specified document.", e);

        }

    }

    public Document get(final String documentId, boolean isLog){

        return repository.get(documentId, isLog);

    }
}

class DocsRepository{

    private final Docs      docService;
    private final String    accessToken;

    private static final String TOKEN_PATH = "/token.properties";


    public DocsRepository(Docs docService) throws GeneralSecurityException, IOException, URISyntaxException {

        this.docService = docService;

        String token;
        this.accessToken    = (token = readToken()) != null ?
                token
                :
                GoogleAuth.authorize(GoogleNetHttpTransport.newTrustedTransport()).getAccessToken();

        System.out.printf("An access token was obtained from the authorization server: %s%n", accessToken);

        saveToken();

    }

    private Document    document(final String title){

        return new Document().setTitle(title);
    }

    public Document     create(
            final String title,
            final String bodyString
    ){

        Document doc;

        try {

            Docs.Documents.Create create =
                    docService.documents().create(document(title));
            create.setAccessToken(accessToken);

            doc = create.execute();

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        // failed to create a document
        if(doc == null)
            return null;

        // completed creating documents
        System.out.printf("Document [%s] is successfully created.\n", doc.getDocumentId());


        // insert content string
        get(doc.getDocumentId(), false);

        BatchUpdateDocumentResponse response;

        try {

            response = batchUpdate(doc, bodyString);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        return get(response.getDocumentId(), false);

    }

    public BatchUpdateDocumentResponse batchUpdate(
            final Document doc,
            final String bodyString
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

        return docService.documents().batchUpdate(doc.getDocumentId(), body).setAccessToken(accessToken).execute();
    }


    public Document get(final String documentId, boolean isLog){

        Document doc;

        try {

            Docs.Documents.Get get = docService.documents().get(documentId);
            get.setAccessToken(accessToken);

            doc = get.execute();

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

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

    private void    saveToken() throws IOException, URISyntaxException {

        File token = new File(
                Objects
                        .requireNonNull(getClass().getResource(TOKEN_PATH))
                        .toURI()
        );

        try (OutputStream writer = new FileOutputStream(token)) {

            Properties properties = new Properties();
            properties.setProperty("access_token", accessToken);
            properties.store(writer, "google oauth2.0 access token");

        }

    }

    private String  readToken() throws IOException {

        Properties properties;

        try(InputStream is = getClass().getResourceAsStream(TOKEN_PATH)){

            if(is == null)
                throw new RuntimeException();

            properties = new Properties();
            properties.load(new InputStreamReader(is));

        }

        return properties.getProperty("access_token");
    }
}
