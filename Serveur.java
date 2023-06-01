
//il faut lancer le serveur avec java -cp .:json.jar Serveur
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Serveur {
    // logger pour trace
    private static final Logger LOGGER = Logger.getLogger(Serveur.class.getName());
    private static final int PORT = 8080; // port serveu    client.setServiceInterface(ITestBean.class);r
    private static final String URL = "/raw"; // url de base du service
    // boucle principale qui lance le serveur sur le port 8080, à l'url test

    public static void main(String[] args) {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

            server.createContext(URL, new MyHttpHandler());
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server.setExecutor(threadPoolExecutor);
            server.start();
            LOGGER.info(" Server started on port " + PORT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MyHttpHandler implements HttpHandler {
        /**
         * Manage GET request param
         * 
         * @param httpExchange
         * @return first value
         */
        private String handleGetRequest(HttpExchange httpExchange) {
            return httpExchange.getRequestURI()
                    .toString()
                    .split("\\?")[1]
                    .split("=")[1];
        }

        private String occurencesNumbers(List<String> words, String code) {
            String response = "{";
            for (String word : words) {
                response += "\"" + word + "\":";
                response += Integer.toString(code.split(word, -1).length - 1) + ",";
            }
            return response.substring(0, response.length() - 1) + "}";
        }

        private String countFunctionsLines(String code) {
            int countLines = 0;
            int countFunctions = 0;
            int currentFunctionLines = 0;
            int functionLines = 0;
            int indentationCount = 0;
            int indentationFunction = 0;
            code = code.replace("\"", "");
            List<Integer> functionLineCounts = new ArrayList<>();
            try (BufferedReader strReader = new BufferedReader(new StringReader(code))) {
                String line;
                String lineNotStrip;
                Boolean function = false;
                while ((lineNotStrip = strReader.readLine()) != null) {

                    line = lineNotStrip.strip();

                    if (!line.isEmpty() && !line.startsWith("#")) {
                        countLines++;
                        indentationCount = 0;
                        while (indentationCount < lineNotStrip.length() && (lineNotStrip.charAt(indentationCount) == ' '
                                || lineNotStrip.charAt(indentationCount) == '\t')) {
                            indentationCount++;
                        }
                        if (function && indentationCount == indentationFunction && currentFunctionLines > 1) {
                            functionLineCounts.add(currentFunctionLines - 1);
                            functionLines += currentFunctionLines - 1;
                            currentFunctionLines = 0;
                            function = false;
                        } else {
                            currentFunctionLines++;
                        }

                        if (line.startsWith("def ")) {
                            countFunctions++;
                            indentationFunction = 0;
                            function = true;
                            while (indentationFunction < lineNotStrip.length()
                                    && (lineNotStrip.charAt(indentationFunction) == ' '
                                            || lineNotStrip.charAt(indentationFunction) == '\t')) {
                                indentationFunction++;
                            }
                            if (currentFunctionLines > 1) {
                                functionLineCounts.add(currentFunctionLines - 1);
                                functionLines += currentFunctionLines - 1;
                            }

                            currentFunctionLines = 1;
                        }
                    }
                }

                // Compte la dernière fonction
                if (function && currentFunctionLines > 0) {
                    functionLineCounts.add(currentFunctionLines);
                    functionLines += currentFunctionLines;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            double sum = 0.;
            double nb = 0.;
            for (int value : functionLineCounts) {
                sum += value;
                nb++;
            }
            double mean = sum / nb;
            return "{ \"countLines\": " + countLines + ", \"countFunctions\": " + countFunctions
                    + ", \"meanFunctionLines\": " + mean + ", \"maxFunctionLines\": "
                    + Collections.max(functionLineCounts) + ", \"minFunctionLines\": "
                    + Collections.min(functionLineCounts) + " }";
        }

        private String handlePostRequest(HttpExchange httpExchange) {
            InputStream requestBody = httpExchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder stringBuilder = new StringBuilder();
            String countFunctionsandLines = "";
            String occurencesNumbers = "";
            String line;
            String code = "";
            List<String> words = new ArrayList<>();
            try {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Analyse des données du corps de la requête
            String body = stringBuilder.toString();
            JSONObject jsonObject = new JSONObject(body);

            // Iterate over the keys and values
            for (String key : jsonObject.keySet()) {
                String value = jsonObject.getString(key);
                if (key.equals("code")) {
                    code = value; // Restaurer les sauts de ligne
                } else {
                    words.add(value);
                }
            }
            return "{ \"countFunctionsandLines\": " + countFunctionsLines(code) + ", \"occurencesNumbers\":"
                    + occurencesNumbers(words, code) + " }";
        }

        /**
         * Generate simple response html page
         * 
         * @param httpExchange
         * @param requestParamVaue
         */
        private void handleResponse(HttpExchange httpExchange, String requestParamValue) throws IOException {
            OutputStream outputStream = httpExchange.getResponseBody();
            // this line is a must
            httpExchange.sendResponseHeaders(200, requestParamValue.length());
            outputStream.write(requestParamValue.getBytes());
            outputStream.flush();
            outputStream.close();
        }

        // Interface method to be implemented
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;
            if ("GET".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handleGetRequest(httpExchange);
            } else if ("POST".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handlePostRequest(httpExchange);
            }
            handleResponse(httpExchange, requestParamValue);

        }
    }
}