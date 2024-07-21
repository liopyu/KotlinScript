package net.liopyu.kotlinscript;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class EnhancedScanner implements Closeable {
    private Scanner scanner;
    private Queue<String> buffer;

    public EnhancedScanner(InputStream source) {
        this.scanner = new Scanner(source);
        this.buffer = new LinkedList<>();
    }
    public EnhancedScanner(InputStream source, Charset charset){
        this.scanner = new Scanner(source,charset);
        this.buffer = new LinkedList<>();
    }
    public EnhancedScanner(Scanner scanner) {
        this.scanner = scanner;
        this.buffer = new LinkedList<>();
    }
    public EnhancedScanner(String source) {
        this.scanner = new Scanner(source);
        this.buffer = new LinkedList<>();
    }
    public EnhancedScanner(File source) throws FileNotFoundException {
        this.scanner = new Scanner(source);
        this.buffer = new LinkedList<>();
    }

    public String nextLine() {
        if (!buffer.isEmpty()) {
            return buffer.poll();
        }
        return scanner.nextLine();
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void unreadLine(String line) {
        buffer.add(line);
    }
    public boolean hasNext() {
        return !buffer.isEmpty() || scanner.hasNext();
    }
    public boolean hasNextLine() {
        return !buffer.isEmpty() || scanner.hasNextLine();
    }

    public void close() {
        scanner.close();
    }
}
