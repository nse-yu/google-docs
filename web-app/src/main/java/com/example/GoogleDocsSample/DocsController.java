package com.example.GoogleDocsSample;

import com.example.GoogleDocsSample.docs.DocsAccessBuilder;
import com.example.GoogleDocsSample.docs.DocsAccessor;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.docs.v1.model.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


@Controller
public class DocsController {

    private DocsAccessor    accessor;
    private TokenResponse   response;
    private String title;
    private String body;
    private String origin;


    @CrossOrigin
    @ResponseBody
    @PostMapping(value = "/docs/generate", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> generateDocument(
            @RequestParam("title") String title,
            @RequestParam("body") String body,
            HttpServletRequest request
    ){

        // obtain form values
        this.title  = title;
        this.body   = body;
        this.origin = request.getHeader("referer");

        System.out.printf("Accepted values: [title: %s, body: %s]%n", title, body);

        // create Accessor for Google Docs API
        accessor = DocsAccessBuilder
                .init("docs-desktop-app")
                .setCallback(System.out::println)
                .build();

        // authorization check before accessing API
        if(accessor.isAuthorized()){

            // trying to redirect browser to the Google auth page
            System.out.println(accessor.redirectURI());
            return ResponseEntity.status(HttpStatus.SEE_OTHER).body(accessor.redirectURI());

        }

        // create new documents and get new instances
        Document doc = null;
        try {

            doc = accessor.create(title, body);

        } catch (IOException e) {

            if(((GoogleJsonResponseException) e).getStatusCode() == HttpStatus.UNAUTHORIZED.value())
                return ResponseEntity.status(HttpStatus.SEE_OTHER).body(accessor.redirectURI());
            else
                throw new RuntimeException(e);

        }

        return ResponseEntity.status(HttpStatus.CREATED).body(doc.getTitle());
    }

    @RequestMapping(value = "/docs/redirect", method = RequestMethod.GET)
    public String redirectedAndGenerate(
            @RequestParam("code") String code
    ){

        // there is no response for this session
        if(response == null)
            response = accessor.requestNewToken(code);

        // apply for the user authorization using Token response
        accessor.authorize(response);
        response = null;

        // create new documents and get new instances
        Document doc = null;
        try {

            doc = accessor.create(title, body);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        System.out.println("redirect to "+origin);

        return "redirect:"+origin;
    }

}
