package com.homehub.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.homehub.exceptions.HomeHubException;

// Asks questions and turns the typed answers into values; a bad answer becomes a HomeHubException that says what was expected.
class Prompter {

    private static final int MAX_AMOUNT = 100;

    // Thrown when the input runs out, so the menu loop can say goodbye even in the middle of a question.
    static class InputEnded extends RuntimeException {
    }

    private final BufferedReader in;
    private final PrintStream out;

    Prompter(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    String ask(String prompt) {
        out.print(prompt);
        try {
            String line = in.readLine();
            if (line == null) {
                throw new InputEnded();
            }
            return line.trim();
        } catch (IOException e) {
            throw new HomeHubException("Could not read your input.");
        }
    }

    // Asks for one of an enum's values and explains the options if the answer is not one of them.
    <E extends Enum<E>> E askChoice(String label, Class<E> type) {
        List<E> options = List.of(type.getEnumConstants());
        String text = ask(label + " " + options + ": ");
        try {
            return Enum.valueOf(type, text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new HomeHubException("'" + text + "' is not a valid " + label.toLowerCase() + ". Choose from " + options + ".");
        }
    }

    int askNumber(String prompt) {
        String text = ask(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new HomeHubException("'" + text + "' is not a whole number.");
        }
    }

    int askAmount() {
        int amount = askNumber("Amount (0-" + MAX_AMOUNT + "): ");
        if (amount < 0 || amount > MAX_AMOUNT) {
            throw new HomeHubException("The amount must be between 0 and " + MAX_AMOUNT + ".");
        }
        return amount;
    }
}
