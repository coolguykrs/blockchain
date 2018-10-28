public class P2P {

    public static void main(String[] args){
        try {
            Server server = new Server();
            server.startServer(Integer.valueOf(args[0]));
        } catch (Exception e) {
            System.out.println("Must give port as the first argument and some string as the second argument (eg. 8089)");
            e.printStackTrace();
        }
    }


}

