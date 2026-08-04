package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMS;

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookie;

    //Getting JWT from Header
    public String getJwtFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", bearerToken);
        if(bearerToken != null && bearerToken.startsWith("Bearer "))
            return bearerToken.substring(7); //Remove Bearer prefix including space

        return null;
    }

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if (cookie != null) {
            //System.out.println("Cookie::" + cookie.getValue());
            return cookie.getValue();
        } else {
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        ResponseCookie responseCookie = ResponseCookie.from(jwtCookie, jwt)
                .path("/api")
                .maxAge(24 * 60 * 60)
                .httpOnly(false)
                .build();
        return responseCookie;
    }

    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie responseCookie = ResponseCookie.from(jwtCookie, null)
                .path("/api")
                .build();
        return responseCookie;
    }

    //Generating Token from username
    public String generateTokenFromUsername(String username){
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(
                        (new Date().getTime() + jwtExpirationMS)
                ))
                .signWith(key())
                .compact();
    }

    //Generating Username from token
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    //Generate Sign key
    public Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    //Validate JWT Token
    public boolean validateJwtToken(String authToken) {
        try{
            System.out.println("Validate");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch(MalformedJwtException e){
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        catch(ExpiredJwtException e){
            logger.error("JWT Token Expired: {}", e.getMessage());
        }
        catch(UnsupportedJwtException e){
            logger.error("JWT Token is unsupported: {}", e.getMessage());
        }
        catch(IllegalArgumentException e){
            logger.error("JWT claims String is empty: {}", e.getMessage());
        }
        return false;
    }
}
