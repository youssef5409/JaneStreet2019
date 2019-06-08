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


    private String readData() {
        int bookIndex = data.indexOf("BOOK");

        if (bookIndex != -1) {
            int buyIndex = data.indexOf("BUY");
            int sellIndex = data.indexOf("SELL");

            String symbol = data.substring(buyIndex + 5, sellIndex - 1);

            if (!(sellIndex - bookIndex < 4)) {
                parseInfo(data.substring(buyIndex + 4, sellIndex - 1), buyPrices, buySize);
            }

            if (sellIndex + 4 != data.length()) {
                String sellInfo = data.substring(sellIndex + 5);

                if (sellInfo.length() > 0) {
                    parseInfo(sellInfo, sellPrices, sellSize);
                }
            }
            return symbol;
        }

        return "Error";
    }

    private void parseInfo(String info, List<Integer> prices, List<Integer> size) {

        String temp = "";

        List<Integer> tempData = new ArrayList<>();

        for (int i = 0; i < info.length(); i++) {
            if (info.charAt(i) == ':' || info.charAt(i) == ' ') {
                tempData.add(Integer.parseInt(temp));
                temp = "";
            } else {
                temp += info.charAt(i);
            }

        }
        tempData.add(Integer.parseInt(temp));

        for (int i = 0; i < tempData.size(); i++) {
            if (i % 2 == 0) {
                prices.add(tempData.get(i));
            } else {
                size.add(tempData.get(i));
            }
        }

    }


    public void trade() {
        try {
            String symbol;
            while (true) {

                clearData();

                data = from_exchange.readLine().trim();

                symbol = readData();

                fairV = calcFairValue(symbol);

                


            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            initConnection(isTesting);
        }

    }

    private float calcFairValue(String type) {
        if (!buyPrices.isEmpty() && !sellPrices.isEmpty())
            switch (type) {
                case "BOND":
                    return 1000;
                case "VALBZ":
                    return simpleFairValue();
                case "VALE":
                    return simpleFairValue();
                case "GS": //GS
                    return simpleFairValue();
                case "MS": // MS
                    return simpleFairValue();
                case "WFC":
                    return simpleFairValue();
                case "XLF":
                    return simpleFairValue();
                case "Error":
                    return -1;
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


