package com.osc.backend.admin.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Local command-line utility to securely generate a BCrypt hash for ADMIN_PASSWORD_HASH.
 *
 * <p>Security Properties:
 * <ul>
 *   <li>Prompts for password interactively via {@link Console#readPassword()} when available,
 *       preventing characters from echoing to the terminal screen or recording in shell history.</li>
 *   <li>Clears sensitive memory by zeroing the character arrays immediately after use.</li>
 *   <li>Never logs or stores the plaintext password anywhere.</li>
 * </ul>
 *
 * <p>Execution Command:
 * <pre>
 *   java -cp "backend/target/classes;backend/target/dependency/*" com.osc.backend.admin.security.AdminPasswordHashGenerator
 * </pre>
 */
public class AdminPasswordHashGenerator {

    public static void main(String[] args) {
        Console console = System.console();
        char[] passwordChars;

        if (console != null) {
            passwordChars = console.readPassword("Enter desired administrator password: ");
            char[] confirmChars = console.readPassword("Confirm desired administrator password: ");
            if (!Arrays.equals(passwordChars, confirmChars)) {
                System.err.println("Error: Passwords do not match. Aborting.");
                Arrays.fill(passwordChars, '\0');
                Arrays.fill(confirmChars, '\0');
                System.exit(1);
            }
            Arrays.fill(confirmChars, '\0');
        } else {
            System.out.println("Notice: Interactive console unavailable. Reading from standard input.");
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter desired administrator password: ");
            String input = scanner.hasNextLine() ? scanner.nextLine() : "";
            passwordChars = input.toCharArray();
        }

        if (passwordChars == null || passwordChars.length == 0) {
            System.err.println("Error: Password cannot be empty.");
            System.exit(1);
        }

        try {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
            String hash = encoder.encode(new String(passwordChars));
            System.out.println("\nGenerated BCrypt Hash (strength 12):");
            System.out.println("ADMIN_PASSWORD_HASH=" + hash);
            System.out.println("\nConfigure this value in your Render environment variables.");
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }
}
