package com.example.projetandroidgotoesig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SimpleHasher {
    /**
     * Méthode de hashage du mot de passe
     * @param password
     * @return
     */
    public static String hashPassword(String password) {
        try {
            // Création de l'objet MessageDigest pour SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hachage du mot de passe
            byte[] hash = digest.digest(password.getBytes());

            // Conversion du résultat en hexadécimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors du hachage : " + e.getMessage());
        }
    }
}

