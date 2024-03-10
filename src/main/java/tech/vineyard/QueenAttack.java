package tech.vineyard;

import tech.vineyard.model.Input;
import tech.vineyard.model.Output;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * https://www.topcoder.com/challenges/fd5f10e0-34de-4fdc-9df1-6bd8681debf3
 */
public class QueenAttack {
    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream(args[0]); // System.in;
        Input input = parseInput2(inputStream);

        Output output = new Service(new QueenAttackModel(input))
                .output();

        OutputStream outputStream = new FileOutputStream("1.out"); // System.out; //
        writeOutput(output, outputStream);
    }

    public static Input parseInput(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        int N = Integer.parseInt(bufferedReader.readLine());
        int C = Integer.parseInt(bufferedReader.readLine());

        int[][] grid = new int[N][N];
        IntStream.range(0, N)
                .forEach(r -> IntStream.range(0, N)
                        .forEach(c ->
                                grid[r][c] = readInt(bufferedReader)
                        ));

        bufferedReader.close();

        return new Input(N, C, grid);
    }

    public static Input parseInput2(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        int N = Integer.parseInt(bufferedReader.readLine());
        int C = Integer.parseInt(bufferedReader.readLine());

        int[][] grid = new int[N][N];
        IntStream.range(0, N)
                .forEach(r -> {
                    String line = readLine(bufferedReader);
                    IntStream.range(0, N)
                            .forEach(c ->
                                    grid[r][c] = readInt(line.charAt(c))
                            );
                });

        bufferedReader.close();

        return new Input(N, C, grid);
    }

    private static int readInt(char c) {
        switch (c) {
            case '.':
                return 0;
            case '#':
                return -1;
            default:
                return c - '0';
        }
    }

    public static void writeOutput(Output output, OutputStream outputStream) throws IOException {
        PrintWriter printWriter = new PrintWriter(outputStream);

        printWriter.println(output.moves().length);
        Arrays.stream(output.moves())
                .forEach(printWriter::println);

        outputStream.close();
    }

    private static String readLine(BufferedReader bufferedReader) {
        try {
            return bufferedReader.readLine();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static int readInt(BufferedReader bufferedReader) {
        return Integer.parseInt(readLine(bufferedReader));
    }
}