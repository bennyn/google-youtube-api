package com.welovecoding.web.login.google;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.PlusScopes;
import com.google.api.services.plus.model.Person;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * @see https://github.com/google/google-http-java-client
 * @see
 * @see https://developers.google.com/api-client-library/java/
 * @see https://developers.google.com/api-client-library/java/google-api-java-client/oauth2#web_server_applications
 */
@ApplicationScoped
public class UtilBean implements Serializable {

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final Logger LOG = Logger.getLogger(UtilBean.class.getName());
  private static final List<String> SCOPES = Arrays.asList(
    PlusScopes.PLUS_ME,
    PlusScopes.USERINFO_EMAIL,
    PlusScopes.USERINFO_PROFILE,
    YouTubeScopes.YOUTUBE_READONLY
  );
  private static String applicationName;
  private static GoogleClientSecrets clientSecrets;
  private static final long serialVersionUID = 1L;

  static {
    LOG.setLevel(Level.INFO);
  }

  public GoogleTokenResponse convertCodeToToken(String code, String redirectUri) throws IOException {
    return new GoogleAuthorizationCodeTokenRequest(
      HTTP_TRANSPORT,
      JSON_FACTORY,
      clientSecrets.getDetails().getClientId(),
      clientSecrets.getDetails().getClientSecret(),
      code,
      redirectUri
    ).execute();
  }

  public AuthorizationCodeFlow getFlow() {
    return new GoogleAuthorizationCodeFlow.Builder(
      HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
    ).setAccessType("offline").build();
  }

  public String getUserInfo(GoogleTokenResponse googleTokenResponse) throws IOException {
    GoogleAuthorizationCodeFlow flow
      = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        clientSecrets.getDetails().getClientId(),
        clientSecrets.getDetails().getClientSecret(),
        SCOPES).build();

    String userId = googleTokenResponse.parseIdToken().getPayload().getSubject();
    Credential credential = flow.createAndStoreCredential(googleTokenResponse, userId);
    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);

    GenericUrl url = new GenericUrl("https://www.googleapis.com/oauth2/v1/userinfo");
    HttpRequest request = requestFactory.buildGetRequest(url);
    String userInfo = request.execute().parseAsString();
    return userInfo;
  }

  /**
   * @todo Outsource functionality to "PlusService" EJB
   * @param client
   * @return
   * @throws IOException
   */
  public Person getSelfUser(Plus client) throws IOException {
    return client.people().get("me").execute();
  }

  /**
   * @see http://www.programcreek.com/java-api-examples/index.php?api=com.google.api.services.plus.Plus
   * @param person
   * @return
   * @throws Exception
   */
  private String getEmailAddress(Person person) throws Exception {
    String emailAddress = null;

    List<Person.Emails> emails = person.getEmails();
    for (Person.Emails email : emails) {
      if (email.getType().equals("account")) {
        emailAddress = email.getValue();
      }
    }

    if (emailAddress == null) {
      throw new Exception("Account email not in email list");
    }

    return emailAddress;
  }

  public Plus getPlusClient(String accessToken) {
    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
    return new Plus.Builder(
      HTTP_TRANSPORT, JSON_FACTORY, credential
    ).setApplicationName(applicationName).build();
  }

  public YouTube getYouTubeClient(String accessToken) {
    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
    return new YouTube.Builder(
      HTTP_TRANSPORT, JSON_FACTORY, credential
    ).setApplicationName(applicationName).build();
  }

  private GoogleCredential getCredential(final String accessToken) {
    return new GoogleCredential.Builder()
      .setClientSecrets(clientSecrets)
      .setJsonFactory(JSON_FACTORY)
      .setTransport(HTTP_TRANSPORT)
      .setRequestInitializer((new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest request)
          throws IOException {
          request.getHeaders().put("Authorization", "Bearer " + accessToken);
        }
      })).build();
  }

  @PostConstruct
  void init() {
    String file = "/production/google/client_secrets.json";
    try {
      clientSecrets = readClientSecrets(file);
      applicationName = readProjectId(file);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, ex.getMessage());
    }
  }

  GoogleClientSecrets readClientSecrets(String file) throws IOException {
    GoogleClientSecrets secrets;

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream(file);
      InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      secrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
    }

    return secrets;
  }

  String readProjectId(String file) throws IOException {
    String projectId;

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream(file)) {
      JsonReader reader = Json.createReader(stream);
      JsonObject payload = reader.readObject();
      JsonObject web = payload.getJsonObject("web");
      projectId = web.getString("project_id");
    }

    return projectId;
  }
}
