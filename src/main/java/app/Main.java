package app;

import editor.DocumentEditor;
import parser.CommandParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

/** Runs the same command parser from the console or a UTF-8 command script. */
public final class Main {
    private Main() {
    }

    /** Starts the editor. With one argument, reads commands from that file. */
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage: java app.Main [script-file]");
            return;
        }
        CommandParser parser = new CommandParser(new DocumentEditor());
        if (args.length == 1) {
            try {
                runScript(parser, Path.of(args[0]));
            } catch (IOException | RuntimeException failure) {
                System.err.println("Error: " + failure.getMessage());
                System.exit(1);
            }
        } else {
            runInteractive(parser);
        }
    }

    private static void runScript(CommandParser parser, Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isExit(line)) {
                    return;
                }
                try {
                    printResponse(parser.execute(line));
                } catch (RuntimeException failure) {
                    throw new IllegalArgumentException(
                            "Line " + lineNumber + ": " + failure.getMessage(), failure);
                }
            }
        }
    }

    private static void runInteractive(CommandParser parser) {
        System.out.println("Document Editor. Type help for commands; exit to quit.");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.print("editor> ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String line = scanner.nextLine();
            if (isExit(line)) {
                return;
            }
            try {
                printResponse(parser.execute(line));
            } catch (RuntimeException failure) {
                System.out.println("Error: " + failure.getMessage());
            }
        }
    }

    private static void printResponse(String response) {
        if (!response.isEmpty()) {
            System.out.println(response);
        }
    }

    private static boolean isExit(String input) {
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        return trimmed.equals("exit") || trimmed.equals("quit");
    }
}
