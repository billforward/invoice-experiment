package net.billforward;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by birch on 23/08/2015.
 */
public class Uncss {
    protected File inputDir;
    public Uncss(File inputDir) {
        this.inputDir = inputDir;
    }

    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    public String uncss(String string) throws UncssFailedException {
        File file = writeToFile(string);
        return uncss(file);
    }

    protected File writeToFile(String string) throws UncssFailedException {
        UUID unique = UUID.randomUUID();
        String fileName = String.format("%s.html", unique);
        File file = new File(getInputDir(), fileName);
        Path filePath = file.toPath();
        try {
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
        } catch (IOException e) {
            UncssFailedException uncssFailedException = new UncssFailedException("Couldn't reserve temporary filepath for input");
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        }

        PrintWriter writer;
        try {
            writer = new PrintWriter(file, "UTF-8");
        } catch (FileNotFoundException e) {
            UncssFailedException uncssFailedException = new UncssFailedException("Couldn't write generated text to file");
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        } catch (UnsupportedEncodingException e) {
            UncssFailedException uncssFailedException = new UncssFailedException("Couldn't write generated text to file using specified encoding");
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        }
        writer.print(string);
        writer.close();

        return file;
    }

    public String uncss(File inputFile) throws UncssFailedException {
        System.out.println("Hello World!");

        // Get current classloader
        ClassLoader classLoader = this.getClass().getClassLoader();
        String node;
        String index;
        try {
            node = classLoader.getResource("generated-resources/node").getFile();
            index = classLoader.getResource("js/index.js").getFile();
        } catch(NullPointerException e) {
            UncssFailedException uncssFailedException = new UncssFailedException("Couldn't find Uncsser");
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        }

        String[] command = {node, index, inputFile.getAbsolutePath()};
        System.out.printf("Running '%s'...\n",
                Arrays.toString(command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            UncssFailedException uncssFailedException = new UncssFailedException("Couldn't start Uncss process");
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        }

        //Read out dir output
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        System.out.printf("Output of running '%s' is:\n",
                Arrays.toString(command));

        StringBuilder outputBuilder = new StringBuilder();

        try {
            while ((line = br.readLine()) != null) {
                outputBuilder.append(line);
                outputBuilder.append('\n');
            }
        } catch (IOException e) {
            UncssFailedException uncssFailedException = new UncssFailedException("Error whilst running Uncss");
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        } finally {
            //Wait to get exit value
            try {
                int exitValue = process.waitFor();
                System.out.println("\n\nExit Value is " + exitValue);
            } catch (InterruptedException e) {
                UncssFailedException uncssFailedException = new UncssFailedException("Interrupted whilst waiting for Uncss to close");
                uncssFailedException.setStackTrace(e.getStackTrace());
                throw uncssFailedException;
            }
        }

        String output = outputBuilder.toString();

        cleanup(inputFile);

        File outputFile = writeToFile(output);

        return output;
    }

    protected void cleanup(File file) throws UncssFailedException {
        Path inputFilePath = file.toPath();

        try {
            Files.delete(inputFilePath);
        } catch (NoSuchFileException e) {
//            System.err.format("%s: no such" + " file or directory%n", path);
            UncssFailedException uncssFailedException = new UncssFailedException(String.format("Failed to clean up input; '%s': no such file or directory", inputFilePath));
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        } catch (DirectoryNotEmptyException e) {
//            System.err.format("%s not empty%n", path);
            UncssFailedException uncssFailedException = new UncssFailedException(String.format("Failed to clean up input; '%s': directory not empty", inputFilePath));
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        } catch (IOException e) {
            // File permission problems are caught here.
//            System.err.println(x);
            UncssFailedException uncssFailedException = new UncssFailedException(String.format("Failed to clean up input; '%s': IO exception.", inputFilePath));
            uncssFailedException.setStackTrace(e.getStackTrace());
            throw uncssFailedException;
        }
    }
}
