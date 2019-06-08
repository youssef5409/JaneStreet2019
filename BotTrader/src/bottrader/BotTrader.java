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
import java.util.*;

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

    boolean isTesting = false;

    int lastXLF = -1;

    /*
    List<Integer> buyPrices = new ArrayList<>();
    List<Integer> buySize = new ArrayList<>();
    List<Integer> sellPrices = new ArrayList<>();
    List<Integer> sellSize = new ArrayList<>();
*/

    Map<String, Float> fairValues = new HashMap<>();

    float[] etfValues = {-1, -1, -1, -1};

    int maxBid;
    int minAsk;

    int fairV;

    String symbol;

    private int orderID = 0;

    private String data;

    private String[] split = new String[4];

    public BotTrader() {

        initConnection(isTesting);
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

    public static void main(String[] args) {
        /* The boolean passed to the Configuration constructor dictates whether or not the
           bot is connecting to the prod or test exchange. Be careful with this switch! */
        BotTrader bot = new BotTrader();

        while (true) {
            bot.trade();
        }
    }

    private void clearData() {
        symbol = "";
        /*
        buyPrices.clear();
        buySize.clear();
        sellPrices.clear();
        sellSize.clear();

         */
    }

    private int parser() {
        int tradeIndex = data.indexOf("TRADE");

        if (tradeIndex != -1) {
            split = data.split(" ");
            symbol = split[1];
            fairV = Integer.parseInt(split[2]);

            if (symbol == "BOND") {
                etfValues[0] = fairV;
            } else if (symbol == "GS") {
                etfValues[1] = fairV;
            } else if (symbol == "MS") {
                etfValues[2] = fairV;
            } else if (symbol == "WFC") {
                etfValues[3] = fairV;
            } else if (symbol == "XLF") {
                lastXLF = fairV;


            } else {
                return -1;
            }

            return 1;
        }

        return -1;
    }

    /*
    private int readData() {

        System.out.println(data);
        int bookIndex = data.indexOf("BOOK");

        if (bookIndex != -1) {
            int buyIndex = data.indexOf("BUY");
            int sellIndex = data.indexOf("SELL");

            symbol = data.substring(bookIndex + 5, buyIndex - 1);

            if (symbol == "BOND" || symbol == "GS" || symbol == "MS" || symbol == "WFC") {
                if (!(sellIndex - bookIndex < 4)) {
                    parseInfo(data.substring(buyIndex + 4, sellIndex), buyPrices, buySize);
                }

                if (sellIndex + 4 != data.length()) {
                    String sellInfo = data.substring(sellIndex + 5);

                    if (sellInfo.length() > 0) {
                        parseInfo(sellInfo, sellPrices, sellSize);
                    }
                }
            } else {
                return -1;
            }

        }

        return 1;

    }

     */

    /*
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
        if (!"".equals(temp)) {
            tempData.add(Integer.parseInt(temp));
        }

        for (int i = 0; i < tempData.size(); i++) {
            if (i % 2 == 0) {
                prices.add(tempData.get(i));
            } else {
                size.add(tempData.get(i));
            }
        }

    }

     */

    public void trade() {
        try {
            while (true) {
                //clearData();

                data = from_exchange.readLine().trim();

                if (parser() == -1) {
                    return;
                }

                float calcXLF = etfFairV();
                if (calcXLF < 0 || lastXLF < 0) {
                    return;
                } else if (calcXLF < lastXLF) {
                    sell((int) Math.floor(Math.abs((calcXLF - lastXLF) * 0.65)));
                } else if (calcXLF > lastXLF) {
                    buy((int) Math.floor(Math.abs((calcXLF - lastXLF) * 0.65)));
                }

                /*
                fairV = calcFairValue(symbol);
                switch (symbol) {
                    case "BOND":
                        etfValues[0] = fairV;
                    case "GS":
                        etfValues[1] = fairV;
                    case "MS":
                        etfValues[2] = fairV;
                    case "WFC":
                        etfValues[3] = fairV;
                    case "XLF":
                        if (fairV < etfFairV()) {
                            buy(10);
                        } else if (fairV > etfFairV()) {
                            sell(10);
                        }
                    default:
                        break;
                }


               */


            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            initConnection(isTesting);
        }

    }

    private void buy(int margin) {

        int price = lastXLF - margin;

        String send = "ADD " + orderID + " " + "XLF" + " BUY " + price + " " + 10;
        System.out.println("Sending: " + send);
        to_exchange.println(send);
        orderID++;
    }

    private void sell(int margin) {
        int price = lastXLF + margin;

        String send = "ADD " + orderID + " " + "XLF" + " SELL " + price + " " + 10;
        System.out.println("Sending: " + send);
        to_exchange.println(send);
        orderID++;
    }

    /*
    private void bondStrat() {
        if (!sellPrices.isEmpty() || !sellSize.isEmpty()) {
            for (int ask : sellPrices) {
                if (ask >= 1000) {
                    break;
                }

                String send = "ADD " + orderID + " " + "BOND" + " BUY " + ask + " " + sellSize.get(sellPrices.indexOf(ask));
                System.out.println("Sending: " + send);
                to_exchange.println(send);
                orderID++;


                if (!buyPrices.isEmpty() || !buySize.isEmpty()) {
                    for (int bid : buyPrices) {
                        if (bid <= 1000) {
                            break;
                        }

                        send = "ADD " + orderID + " " + "BOND" + " SELL " + bid + " " + buySize.get(buyPrices.indexOf(bid));
                        System.out.println("Sending: " + send);
                        to_exchange.println(send);
                        orderID++;
                    }
                }
            }
        }
    }


    /*
    private float calcFairValue(String type) {
        if (!buyPrices.isEmpty() && !sellPrices.isEmpty()) {
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
        }

        return -1;
    }

     */

    private float etfFairV() {
        float sum = 0;
        for (int i = 0; i < 4; i++) {
            if (etfValues[i] < 0) {
                return -1;
            }
            if (i % 2 == 0) {
                sum += 3 * etfValues[i];
            } else {
                sum += 2 * etfValues[i];
            }
        }
        return sum / 10;
    }
    /*

    private float simpleFairValue() {
        maxBid = Collections.max(buyPrices);
        minAsk = Collections.min(sellPrices);

        return (float) (maxBid + minAsk) / 2;
    }

     */


}

