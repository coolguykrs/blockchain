import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileParser {

    private static final String NODES_FILE_PATH = "resources/network.txt";
    private static final String TRANSACTIONS_FILE_PATH = "resources/transactionBlocks.json";


    public static String readAddressFile() {
        try {
            return new String(Files.readAllBytes(Paths.get(NODES_FILE_PATH)));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String readTransactionsFile() {
        try {
            return new String(Files.readAllBytes(Paths.get(TRANSACTIONS_FILE_PATH)));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }


    //Parse connected nodes from file
    public static List<String> getConNodes() {
        List<String> nodes = new ArrayList<>();
        String contents = readAddressFile();
        BufferedReader bufReader = new BufferedReader(new StringReader(contents));
        String line;
        try {
            while ((line = bufReader.readLine()) != null) {
                nodes.add(line);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodes;
    }

    public static boolean isListEmpty() {
        return getConNodes().isEmpty();
    }

    public static void addAddress(String address) {
        System.out.println("writing address to file: " + address);
        try {
            Files.write(Paths.get(FileParser.NODES_FILE_PATH), (FileParser.readAddressFile()  + address + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addTransaction(String block) {
        System.out.println("Adding new transaction block to file: " + block);
        try {
            JSONObject file = new JSONObject(new String(Files.readAllBytes(Paths.get(TRANSACTIONS_FILE_PATH))));
            JSONObject blocks = new JSONObject(block);
            file.append("blocks", blocks);
            String content = file.toString();
            Files.write(Paths.get(FileParser.TRANSACTIONS_FILE_PATH), content.getBytes());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void addAllTransactions(String[] response) {
        try {
            String content = "";
            for (String line : response) {
                content = content + line;
            }
            Files.write(Paths.get(FileParser.TRANSACTIONS_FILE_PATH), content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
