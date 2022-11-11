package com.example.GoogleDocsSample.auth;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

public class GoogleAuth {

    private static final String             CLIENT_INFO = "/credentials.json";
    private static final String             CREDENTIAL_DIR = "src/main/resources/static/cred/";
    private static final Collection<String> SCOPE = Collections.singleton("https://www.googleapis.com/auth/documents");
    private static final JsonFactory        JSON_FACTORY = new GsonFactory();
    private static final Client             CLIENT = readClientFromJson();
    private static final Credential.AccessMethod METHOD = BearerToken.authorizationHeaderAccessMethod();
    private final AuthorizationCodeFlow   FLOW;


    public GoogleAuth(final HttpTransport httpTransport){

        try {

            FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(
                    new File(CREDENTIAL_DIR)
            );

            FLOW = new AuthorizationCodeFlow.Builder(
                    METHOD,
                    httpTransport,
                    JSON_FACTORY,
                    new GenericUrl(CLIENT.getToken_uri()),
                    new ClientParametersAuthentication(CLIENT.getClient_id(), CLIENT.getClient_secret()),
                    CLIENT.getClient_id(),
                    CLIENT.getAuth_uri()
            )
                    .setCredentialDataStore(StoredCredential.getDefaultDataStore(fileDataStoreFactory))
                    .setScopes(SCOPE)
                    .enablePKCE()
                    .build();

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }


    public boolean  isAuthorized(){

        try {

            if(FLOW.loadCredential(CLIENT.getClient_id()) != null)
                return true;

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        return false;
    }

    public Credential getAuthorizedCredential(){

        if(isAuthorized()) {
            try {

                return FLOW.loadCredential(CLIENT.getClient_id());

            } catch (IOException e) {

                throw new RuntimeException(e);

            }
        }

        return null;

    }

    public String   redirectURI(){

        return FLOW.newAuthorizationUrl()
                .setRedirectUri(CLIENT.getRedirect_uris().get(0))
                .set("access_type", "offline")
                .build();

    }

    public TokenResponse requestNewToken(final String code) throws IOException {

        return FLOW.newTokenRequest(code).setRedirectUri(CLIENT.getRedirect_uris().get(0)).execute();

    }

    public Credential authorize(final TokenResponse response) throws IOException {

        return FLOW.createAndStoreCredential(response, CLIENT.getClient_id());

    }

    private static Client readClientFromJson(){

        // credential json
        Client client;

        try (InputStream in = GoogleAuth.class.getResourceAsStream(CLIENT_INFO)) {

            if (in == null)
                throw new RuntimeException(String.format("The credential file in %s couldn't read.", CLIENT_INFO));

            Gson gson = new Gson();
            client = gson.fromJson(new InputStreamReader(in), Client.class);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        return client;

    }
}
