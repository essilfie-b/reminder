package com.example.reminder.security;

public class SecurityConstants {
    final static int JWT_EXPIRATION_TIME = 86400000;
    final static String SECRET_KEY = System.getenv("JWT_SECRET_KEY");
}
