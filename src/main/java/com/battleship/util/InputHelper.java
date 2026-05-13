package com.battleship.util;

import java.util.Scanner;

/**
 * Utility class untuk sanitasi semua input dari Scanner.
 *
 * Semua interaksi keyboard pemain melewati class ini.
 * Mencegah crash dari: input kosong, non-numerik, angka di luar range,
 * atau Scanner yang bermasalah karena EOF.
 */
public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Baca satu baris teks dari input. Return string kosong jika error. */
    public String readLine() {
        try {
            String line = scanner.nextLine();
            return (line == null) ? "" : line.trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Baca integer dalam rentang [min, max].
     * Jika input tidak valid atau di luar rentang, kembalikan defaultValue.
     * Tidak pernah throw exception.
     */
    public int readInt(int min, int max, int defaultValue) {
        try {
            int value = Integer.parseInt(readLine());
            return (value >= min && value <= max) ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Tampilkan prompt lalu tunggu pemain menekan Enter. */
    public void pressEnter() {
        System.out.print("  [Enter untuk lanjut...] ");
        readLine();
    }
}
