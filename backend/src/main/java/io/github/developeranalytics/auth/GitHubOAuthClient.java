package io.github.developeranalytics.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class GitHubOAuthClient {
    @ConfigProperty(name="developer-analytics.github.client-id") String clientId;
    @ConfigProperty(name="developer-analytics.github.client-secret") String clientSecret;
    @ConfigProperty(name="developer-analytics.github.callback-url") String callbackUrl;
    @Inject ObjectMapper mapper;

    private final HttpClient http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public URI authorizationUri(String state,String challenge){
        return authorizationUri(state, challenge, null);
    }

    public URI authorizationUri(
            String state,
            String challenge,
            String scope
    ){
        String q="client_id="+enc(clientId)+"&redirect_uri="+enc(callbackUrl)+"&state="+enc(state)+
            "&code_challenge="+enc(challenge)+"&code_challenge_method=S256";
        if(scope != null && !scope.isBlank()) {
            q += "&scope=" + enc(scope);
        }
        return URI.create("https://github.com/login/oauth/authorize?"+q);
    }

    public String exchangeCode(String code,String verifier) throws Exception {
        String body="client_id="+enc(clientId)+"&client_secret="+enc(clientSecret)+"&code="+enc(code)+
            "&redirect_uri="+enc(callbackUrl)+"&code_verifier="+enc(verifier);
        HttpRequest req=HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
            .header("Accept","application/json").header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> res=http.send(req,HttpResponse.BodyHandlers.ofString());
        if(res.statusCode()/100!=2) throw new IllegalStateException("GitHub token exchange failed");
        JsonNode json=mapper.readTree(res.body());
        if(!json.hasNonNull("access_token")) throw new IllegalStateException("GitHub token exchange returned no access token");
        return json.get("access_token").asText();
    }

    public GitHubUserProfile currentUser(String accessToken) throws Exception {
        HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
            .header("Accept","application/vnd.github+json")
            .header("Authorization","Bearer "+accessToken)
            .header("X-GitHub-Api-Version","2022-11-28").GET().build();
        HttpResponse<String> res=http.send(req,HttpResponse.BodyHandlers.ofString());
        if(res.statusCode()/100!=2) throw new IllegalStateException("GitHub user lookup failed");
        JsonNode json=mapper.readTree(res.body());
        return new GitHubUserProfile(json.get("id").asLong(), json.get("login").asText(),
            json.hasNonNull("name") ? json.get("name").asText() : null);
    }

    private static String enc(String v){return URLEncoder.encode(v, StandardCharsets.UTF_8);}
}
