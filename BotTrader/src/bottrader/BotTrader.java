/* HOW TO RUN
   1) Configure things in the Configuration class
   2) Compile: javac Bot.java
   3) Run in loop: while true; do java Bot; sleep 1; done
 */
package bottrader;

import java.lang.*;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Configuration {

    String exchange_name;
    int exchange_port;
    /* 0 = prod-like
       1 = slow
       2 = empty
     */
    final Integer test_exchange_kind = 2;
    /* replace REPLACEME with your team name! */
    final String team_name = "TEAMBOUCHER";

    Configuration(Boolean test_mode) {
        if (!test_mode) {
            exchange_port = 20000;
            exchange_name = "production";
        } else {
            exchange_port = 20000 + test_exchange_kind;
            exchange_name = "test-exch-" + this.team_name;
        }
    }

    String exchange_name() {
        return exchange_name;
    }

    Integer port() {
        return exchange_port;
    }
}

public class BotTrader {

    BufferedReader from_exchange;
    PrintWriter to_exchange;

    boolean isTesting = true;

    List<Integer> buyPrices, buySize, sellPrices, sellSize;

    float[] fairValues = new float[7];

    float fairV;

    String symbol;

    private String data;


    public BotTrader() {
        initConnection(isTesting);
    }

    public static void main(String[] args) {
        /* The boolean passed to the Configuration constructor dictates whether or not the
           bot is connecting to the prod or test exchange. Be careful with this switch! */
        BotTrader bot = new BotTrader();

        while (true) {
            bot.trade();
        }
    }

    private void clearData() {
        buyPrices.clear();
        buySize.clear();
        sellPrices.clear();
        sellSize.clear();
    }

    private void readData() {
        int bookIndex = data.indexOf("BOOK");

        if (bookIndex != -1) {
            int buyIndex = data.indexOf("BUY");
            int sellIndex = data.indexOf("SELL");

            symbol = data.substring(buyIndex + 5, sellIndex - 1);

            String buyInfo = ;

            if (!(sellIndex - bookIndex < 4)) {

            }

            if (sellIndex + 4 != data.length()) {
                parseInfo();
            }

            String sellTemp = "";
            List<Integer> sellData = new ArrayList<Integer>();
            if (sellIndex + 4 != data.length()) {
                String sellInfo = data.substring(sellIndex + 5);

                if (sellInfo.length() > 0) {
                    for (int i = 0; i < sellInfo.length(); i++) {
                        if (sellInfo.charAt(i) == ':' || sellInfo.charAt(i) == ' ') {
                            sellData.add(Integer.parseInt(sellTemp));
                            sellTemp = "";
                        } else {
                            sellTemp += sellInfo.charAt(i);
                        }
                    }
                    sellData.add(Integer.parseInt(sellTemp));
                    //System.out.println("sellData: " + sellData);
                    for (int i = 0; i < sellData.size(); i++) {
                        if (i % 2 == 0) {
                            sellPrice.add(sellData.get(i));
                        } else {
                            sellAmount.add(sellData.get(i));
                        }
                    }
                }
            }
        }



    }

    private void parseInfo(String buyInfo) {


        if (!(sellIndex - bookIndex < 4)) {
            String buyTemp = "";

            List<Integer> buyData = new ArrayList<>();


            for (int i = 0; i < buyInfo.length(); i++) {
                if (buyInfo.charAt(i) == ':' || buyInfo.charAt(i) == ' ') {
                    buyData.add(Integer.parseInt(buyTemp));
                    buyTemp = "";
                } else {
                    buyTemp += buyInfo.charAt(i);
                }

            }
            buyData.add(Integer.parseInt(buyTemp));

            for (int i = 0; i < buyData.size(); i++) {
                if (i % 2 == 0) {
                    buyPrices.add(buyData.get(i));
                } else {
                    buySize.add(buyData.get(i));
                }
            }

        }
    }

    public void trade() {
        try {
            while (true) {

                clearData();

                data = from_exchange.readLine().trim();

                readData();

            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            initConnection(isTesting);
        }

    }

    private float calcFairValue(int type) {
        if (!buyPrices.isEmpty() && !sellPrices.isEmpty())
            switch (type) {
                case 0:
                    return 1000;
                case 1: //VALBZ
                    return simpleFairValue();
                case 2: // VALE
                    return simpleFairValue();
                case 3: //GS
                    return simpleFairValue();
                case 4: // MS
                    return simpleFairValue();
                case 5:
                    return simpleFairValue();
                case 6:
                    return simpleFairValue();
                default:
                    return -1;
            }

        return -1;
    }

    private float simpleFairValue() {
        int maxBid = Collections.max(buyPrices);
        int minAsk = Collections.min(sellPrices);

        return (float) (maxBid + minAsk) / 2;
    }

    private void initConnection(boolean testMode) {

        Configuration config = new Configuration(testMode);

        try {
            Socket skt = new Socket(config.exchange_name(), config.port());
            from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            to_exchange = new PrintWriter(skt.getOutputStream(), true);

            /*
              A common mistake people make is to to_exchange.println() > 1
              time for every from_exchange.readLine() response.
              Since many write messages generate marketdata, this will cause an
              exponential explosion in pending messages. Please, don't do that!
             */
            to_exchange.println(("HELLO " + config.team_name));
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}


