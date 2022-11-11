package org.example.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

public class GoogleAuth {

    private static final String CREDENTIALS = "/credentials.json";
    private static final Collection<String> SCOPE = Collections.singleton("https://www.googleapis.com/auth/documents");
    private static final JsonFactory JSON_FACTORY = new GsonFactory();


    public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {

        // credential json
        InputStream in = GoogleAuth.class.getResourceAsStream(CREDENTIALS);

        if(in == null)
            throw new RuntimeException(String.format("The credential file in %s couldn't read.", CREDENTIALS));

        // load the client secrets from credential json
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPE)
                        .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

    }

    /*
    public static GoogleCredentials authorize() {

        // Load client credentials.
        GoogleCredentials credentials;

        try (InputStream in = GoogleAuth.class.getResourceAsStream(CREDENTIALS)) {

            if(in == null)
                throw new RuntimeException("");

            credentials = GoogleCredentials.fromStream(in);
            credentials.createScoped(List.of(SCOPE));
            //credentials.refreshIfExpired();

            System.out.println(credentials.getAccessToken());

        } catch (NullPointerException e){

            throw new RuntimeException("Resource path is not valid.", e);

        } catch (IOException e){

            throw new RuntimeException("Can't generate credentials from provided client.json.", e);

        }

        return credentials;

    }

     */
}
