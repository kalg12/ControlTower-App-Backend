package com.controltower.app.identity.application;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * TOTP (Time-based One-Time Password) service using Google Authenticator.
 */
@Service
public class TotpService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * Generates a new Base32-encoded TOTP secret.
     */
    public String generateSecret() {
        GoogleAuthenticatorKey credentials = gAuth.createCredentials();
        return credentials.getKey();
    }

    /**
     * Builds an otpauth:// URI for QR code generation.
     */
    public String getQrUrl(String email, String secret) {
        String issuer = "ControlTower";
        String encodedIssuer  = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /**
     * Verifies a TOTP code against the given secret.
     */
    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}
