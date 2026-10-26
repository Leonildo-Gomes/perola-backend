package cv.perola.modules.identity.service;

public interface GoogleTokenVerifier {
    GoogleUserData verify(String idToken);
}
