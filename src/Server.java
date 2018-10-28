import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    private PeerNode currentNode;
    private List<String> currentTransactions;

    public void startServer(Integer port) throws Exception {
        this.currentNode = new PeerNode(InetAddress.getLocalHost().getHostAddress(), port);
        System.out.println("Server started on address: " + currentNode.getFullAddress());
        startListening();
        if (!FileParser.isListEmpty()) {
            //1. hakka küsima kõigilt kes ühendunud teisi node ja lisa aind unikaalsed
            getConNodes(); //get other connected nodes and write new ones to file
            sendAddressToEveryone(); //notify everyone else about yourself
            getTransactions();//get transactions file from random node
        }
    }

    //notify everyone else of the new transaction
    private static void syncTransactionsWithEveryone(String newTransaction) {
        System.out.println("Notifying everyone of the new transactions block");
        List<String> everyone = FileParser.getConNodes();
        for (String node : everyone) {
            if (!node.trim().isEmpty()) sendSimplePost("http://" + node + "/transactions/sync", newTransaction);
        }
    }

    private void sendAddressToEveryone() {
        System.out.println("Notifying everyone of arrival");
        List<String> everyone = FileParser.getConNodes();
        for (String node : everyone) {
            if (!node.trim().isEmpty())
                sendSimpleGet("http://" + node + "/nodes/newNode?ip=" + currentNode.getIp() + "&port=" + currentNode.getPort());
        }
    }

    private void startListening() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(currentNode.getPort()), 0);
            server.createContext("/nodes", new GetNodesHandler()); //siit saavad kätte teised
            server.createContext("/nodes/newNode", new NewNodeHandler());
            server.createContext("/transactions/sync", new SyncTransactionsHandler()); //siin from ka, kuna siia saadavfad teised enda omi
            server.createContext("/transactions/get", new GetTransactionsHandler());
            server.createContext("/transactions/add", new AddTransactionHandler()); //siin from pole, sest seda kasutad ainult ise

            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //send simple get and return response
    private static String sendSimpleGet(String url) {
        StringBuffer response = new StringBuffer();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private static String sendSimplePost(String address, String data) {
        String response = "";
        try {
            URL url = new URL(address);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection)con;
            http.setRequestMethod("POST"); // PUT is another valid option
            http.setDoOutput(true);
            http.setFixedLengthStreamingMode(data.getBytes().length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();
            OutputStream os = http.getOutputStream();
            os.write(data.getBytes());
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response += inputLine + "\n";
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    //get connected nodes from central node and write them to file
    private void getConNodes() {
        List<String> knownNodes = FileParser.getConNodes();
        for (String address : knownNodes) {
            System.out.println("Requesting connected nodes from: " + address);
            String[] response = sendSimpleGet("http://" + address + "/nodes").split("\n");
            for (String line : response) {
                if (!knownNodes.contains(line.trim()) && !line.equals(currentNode.getFullAddress().trim()))
                    FileParser.addAddress(line);
            }
        }
    }

    private void getTransactions() {
        List<String> knownNodes = FileParser.getConNodes();
        String node = knownNodes.get(0).trim();
        System.out.println("Requesting initial transactions from: " + node);
        String[] response = sendSimpleGet("http://" + node + "/transactions/get").split("\n");
        FileParser.addAllTransactions(response);
    }


    public static Map<String, String> queryStringToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }


    //HANDLERS

    static class NewNodeHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            Map<String, String> ipNode = Server.queryStringToMap(httpExchange.getRequestURI().getQuery());
            String ip = ipNode.get("ip");
            String port = ipNode.get("port");
            System.out.println("Node " + ip + ":" + port + " joined the network!");
            String response = ip + ":" + port + " succesfully added!";
            FileParser.addAddress(ip + ":" + port);
            Server.writeResponse(httpExchange, response);
        }
    }


    static class GetNodesHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println("Node request occurred, returning nodes");
            Server.writeResponse(httpExchange, FileParser.readAddressFile());
        }
    }

    static class GetTransactionsHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println("Transactions request occurred, returning transactions");
            String response = FileParser.readTransactionsFile();
            Server.writeResponse(httpExchange, response);
        }
    }


    static class SyncTransactionsHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            String data = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                data += inputLine;
            }
            in.close();
            FileParser.addTransaction(data);
            String response = "Synchronizing transactions block successfully added!";
            Server.writeResponse(httpExchange, response);
        }
    }

    static class AddTransactionHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            Map<String, String> params = Server.queryStringToMap(httpExchange.getRequestURI().getQuery());
            String to = params.get("to");
            String from = DigitalSignature.keyToString(DigitalSignature.getPublicKey().getEncoded());
            BigDecimal balance = TransactionUtil.findAccountBalance(from);
            String sum = params.get("sum");
            String response;
            if (balance.subtract(new BigDecimal(sum)).compareTo(BigDecimal.ZERO) > -1) {
                String timestamp = new Timestamp(System.currentTimeMillis()).toString();
                String signature = DigitalSignature.signTransaction(from + to + sum + timestamp);
                JSONObject transaction = TransactionUtil.createTransactionJson(from, to, sum, timestamp, signature);
                String transactionBlock = TransactionUtil.addTransaction(transaction);
                if (transactionBlock != null) { //saadame edasi kui blokk sai täis
                    //String transferJson = "{" + transactionBlock + "}";
                    syncTransactionsWithEveryone(transactionBlock);//saadame edasi.
                }
                System.out.println("Account balance was: " + balance + ", now is: " + balance.subtract(new BigDecimal(sum)));
                response = "New transaction: " + transaction.toString() + " succesfully added";
            } else {
                response = "Account does not have enough balance for the transaction";
            }
            Server.writeResponse(httpExchange, response);
        }


    }

    public static void writeResponse(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}