package com.dreamgames.backendengineeringcasestudy.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.dreamgames.backendengineeringcasestudy.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Service
public class SecurityService {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthJSON {

        private UUID userID;
        private String username;
        private String country;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${security.secretkey}")
    private String secretKey;
    
    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        algorithm = Algorithm.HMAC256(secretKey);
        verifier = JWT.require(algorithm).build(); 
    }

    public String encode(UUID userID, String username, String country) {
        try {
            AuthJSON authObject = new AuthJSON(
                userID,
                username,
                country
            );
            String authString = objectMapper.writeValueAsString(authObject);

            return JWT.create()
                    .withSubject(authString)
                    .sign(algorithm);   
        } catch (JWTCreationException | IllegalArgumentException | JsonProcessingException e) {
            throw new RuntimeException("Failed to create JWT token: " + e.getMessage(), e);
        }
    }

    public AuthJSON decode(String token) {
        if (token.startsWith("Bearer")) {
            try {
                token = token.substring(7, token.length());
            } catch (StringIndexOutOfBoundsException ex) {
                throw new UnauthorizedException();
            }
        }

        try {
            String authJsonString = verifier.verify(token).getSubject();
            return objectMapper.readValue(authJsonString, AuthJSON.class);
        } catch (JWTVerificationException | JsonProcessingException e) {
            throw new UnauthorizedException();
        }
    }

    /**
     * Check if the the given userID matches the encoded user ID in the token
     * @param userID ID of the user
     * @param token Auth token of the user 
     */
    public boolean isAuthorized(String userID, String token) {
        AuthJSON authJson;
        try {
            authJson = decode(token);
        } catch (UnauthorizedException ex) {
            return false;    
        }

        return userID.equals(authJson.getUserID().toString());
    }
}
