import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.*;

public class TransactionUtil {

    private static List<JSONObject> transactions = new ArrayList<>();
    public static String lastBlock = "";

    private static final int TRANSACTIONS_IN_BLOCK = 4; // nii palju hoiame transaktsioone ühes blokis

    public static JSONObject createTransactionJson(String from, String to, String sum, String timestamp, String signature) {
        JSONObject transactionJson = new JSONObject();
        transactionJson.put("from", from);
        transactionJson.put("to", to);
        transactionJson.put("sum", sum);
        transactionJson.put("timestamp", timestamp);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("signature", signature);
        jsonObject.put("transaction", transactionJson);
        return jsonObject;
    }

    private static String createBlocksJson(List<JSONObject> transactions) {
        JSONObject blocksJson = new JSONObject();
        String lastHash = getLastBlockValue("hash");
        String merkleRoot = calculateMerkleRoot(transactions);
        Timestamp blockTimestamp = new Timestamp(System.currentTimeMillis());
        Integer nr = Integer.valueOf(getLastBlockValue("nr")) + 1;
        String creator = DigitalSignature.keyToString(DigitalSignature.getPublicKey().getEncoded());
        String nonce = blockMining(String.valueOf(nr) + lastHash + blockTimestamp + creator + merkleRoot + transactions.size() + transactions.toString());
        String hash = blockMining(String.valueOf(nr) + lastHash + blockTimestamp + nonce + creator + merkleRoot + transactions.size() + transactions.toString());
        blocksJson.put("nr", nr);
        blocksJson.put("previous_hash", lastHash);
        blocksJson.put("timestamp",blockTimestamp);
        blocksJson.put("nonce", nonce);
        blocksJson.put("hash", hash);
        blocksJson.put("creator",creator);
        blocksJson.put("merkle_root", merkleRoot);
        blocksJson.put("count", transactions.size());
        blocksJson.put("transactions", transactions);
        return blocksJson.toString();
    }

    public static String addTransaction(JSONObject transaction) {
        Boolean alreadyExists = checkBlocksForTransaction(transaction);
        if (!alreadyExists) transactions.add(transaction);
        if (transactions.size() >= TRANSACTIONS_IN_BLOCK){
            lastBlock = writeTransactionsBlockToFile(transactions);
            transactions.clear();
            return lastBlock;
        }
        return null;
    }

    private static String getLastBlockValue(String value) {
        try {
            JSONObject transactionBlocks = new JSONObject(FileParser.readTransactionsFile());
            JSONArray blocks = transactionBlocks.getJSONArray("blocks");
            JSONObject lastObj = blocks.getJSONObject(blocks.length() - 1);
            return String.valueOf(lastObj.get(value));
        } catch (Exception e) {
            System.out.println("First block, no previous " + value);
            if ("nr".equals(value)) return String.valueOf(0);
            else return "";
        }
    }


    private static Boolean checkBlocksForTransaction(JSONObject transaction) {
        return (FileParser.readTransactionsFile().contains(transaction.toString()));
    }

    private static String writeTransactionsBlockToFile(List<JSONObject> transactions) {
        String blocksJson = createBlocksJson(transactions);
        FileParser.addTransaction(blocksJson);
        return blocksJson;
    }

    private static String calculateMerkleRoot(List<JSONObject> transactions) {
        List<String> hashes = new ArrayList<>();
        for (JSONObject transaction: transactions) {
            String hash = calculateShaHash(transaction.toString());
            hashes.add(hash);
        }
        int counter = TRANSACTIONS_IN_BLOCK;
        while (counter > 1) {
            counter = counter / 2;
            for (int i = 0; i + 1 < hashes.size(); i += 2) {
                String hashPair = hashes.get(i) + hashes.get(i + 1);
                String newHash = calculateShaHash(hashPair);
                hashes.set(i, newHash);
            }
        }
        return hashes.get(0);
    }

    //sha on one way hash
    private static String calculateShaHash(String transaction) {
        try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(transaction.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.out.println("SHA hash calculation failed: " + e.getMessage());
            return "";
        }
    }

    private static String blockMining(String block) {
        boolean nonceFound = false;
        String randomString = "";
        while (!nonceFound) {
            randomString = randomString(8);
            String blockWithNonce = block + randomString;
            String hash = calculateShaHash(blockWithNonce);
            if (hash.substring(0, 2).equals("00")) {
                nonceFound = true;
            }
        }
        return randomString;
    }

    private static String randomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return Base64.getEncoder().encodeToString(array);
    }

    public static BigDecimal findAccountBalance(String from) {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            JSONObject transactionBlocks = new JSONObject(FileParser.readTransactionsFile());
            JSONArray blocks = transactionBlocks.getJSONArray("blocks");
            for (Object block : blocks) {
                JSONArray blockTransactions = (JSONArray) ((JSONObject) block).get("transactions");
                for (Object transaction : blockTransactions) {
                    JSONObject transcationDetails = (JSONObject) ((JSONObject) transaction).get("transaction");
                    if ((((String) ((JSONObject) transcationDetails).get("from")).equals(from))) {
                        BigDecimal sum = new BigDecimal((String) transcationDetails.get("sum"));
                        balance = balance.subtract(sum);
                    }
                    if ((((String) ((JSONObject) transcationDetails).get("to")).equals(from))) {
                        BigDecimal sum = new BigDecimal((String) transcationDetails.get("sum"));
                        balance = balance.add(sum);
                    }
                }
            }
            for (JSONObject trans : transactions) {
                JSONObject transcationDetails = (JSONObject) ((JSONObject) trans).get("transaction");
                if ((((String) ((JSONObject) transcationDetails).get("from")).equals(from))) {
                    BigDecimal sum = new BigDecimal((String) transcationDetails.get("sum"));
                    balance = balance.subtract(sum);
                }
                if ((((String) ((JSONObject) transcationDetails).get("to")).equals(from))) {
                    BigDecimal sum = new BigDecimal((String) transcationDetails.get("sum"));
                    balance = balance.add(sum);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return balance;
        }
        return balance;
    }
}
