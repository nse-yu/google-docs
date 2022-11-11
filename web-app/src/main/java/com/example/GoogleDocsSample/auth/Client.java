package com.example.GoogleDocsSample.auth;

import lombok.Data;

import java.util.List;

@Data
public class Client {
    private String client_id;
    private String project_id;
    private String auth_uri;
    private String token_uri;
    private String auth_provider_x509_cert_url;
    private String client_secret;
    private List<String> redirect_uris;
}
