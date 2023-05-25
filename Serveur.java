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
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Serveur {
    // logger pour trace
    private static final Logger LOGGER = Logger.getLogger( Serveur.class.getName() );
    private static final String SERVEUR = "localhost"; // url de base du service
    private static final int PORT = 8001; // port serveur
    private static final String URL = "/test"; // url de base du service
    // boucle principale qui lance le serveur sur le port 8001, à l'url test
    public static void main(String[] args) {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(SERVEUR, PORT), 0);

            server.createContext(URL, new  MyHttpHandler());
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server.setExecutor(threadPoolExecutor);
            server.start();
            LOGGER.info(" Server started on port " + PORT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFileContent(String filePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    private static class MyHttpHandler implements HttpHandler {
        /**
         * Manage GET request param
         * @param httpExchange
         * @return first value
         */
        private String handleGetRequest(HttpExchange httpExchange) {
            return httpExchange.getRequestURI()
                    .toString()
                    .split("\\?")[1]
                    .split("=")[1];
        }

        private String occurencesNumbers(List<String> words, String filename){
            // A faire : récupérer le texte du fichier, conmpter le nombre d'occurences
            String response = "{";
            try {
                String fileContent = readFileContent(filename);
                for(String word : words){
                    response += "\"" +word + "\":";
                    response += Integer.toString(fileContent.split(word, -1).length - 1) + ",";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response.substring(0, response.length() - 1)+"}";
        }

        private String countFunctionsLines(String filename){
            int countLines = 0;
            int countFunctions = 0;
            int currentFunctionLines = 0;
            int functionLines = 0;
            int indentationCount=0;
            int indentationFunction=0;
            List<Integer> functionLineCounts = new ArrayList<>();
            try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
                String fileLine;
                String fileLineNotStrip;
                Boolean function = false;
                while ((fileLineNotStrip = fileReader.readLine()) != null) {
                    
                    fileLine = fileLineNotStrip.strip();

                    if (!fileLine.isEmpty() && !fileLine.startsWith("#")) {
                        countLines++;
                        indentationCount=0;
                        while (indentationCount < fileLineNotStrip.length() && (fileLineNotStrip.charAt(indentationCount) == ' ' || fileLineNotStrip.charAt(indentationCount) == '\t')) {
                            indentationCount++;
                        }
                        if(function && indentationCount==indentationFunction &&currentFunctionLines > 1){
                            functionLineCounts.add(currentFunctionLines - 1);
                            functionLines += currentFunctionLines - 1;
                            currentFunctionLines = 0;
                            function = false;
                        }else{
                            currentFunctionLines++;
                        }
                        
                        if (fileLine.startsWith("def ")) {
                            countFunctions++;
                            indentationFunction=0;
                            function = true;
                            while (indentationFunction < fileLineNotStrip.length() && (fileLineNotStrip.charAt(indentationFunction) == ' ' || fileLineNotStrip.charAt(indentationFunction) == '\t')) {
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
            double mean = sum/nb;
            return "{ \"countLines\": " + countLines + ", \"countFunctions\": " + countFunctions + ", \"meanFunctionLines\": "+ mean + ", \"maxFunctionLines\": " + Collections.max(functionLineCounts) + ", \"minFunctionLines\": " + Collections.min(functionLineCounts) + " }";
        }


        private String handlePostRequest(HttpExchange httpExchange) {
            InputStream requestBody = httpExchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder stringBuilder = new StringBuilder();
            String countFunctionsandLines = "";
            String occurencesNumbers = "";
            String line;
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
            String cleanedJsonString = body.substring(1, body.length() - 1);

            // Split the cleaned JSON string into key-value pairs
            String[] keyValuePairs = cleanedJsonString.split(",");
            String filename = "";

            // Iterate over the key-value pairs
            for (String keyValuePair : keyValuePairs) {
                // Split each key-value pair into key and value
                String[] parts = keyValuePair.split(":");
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Remove surrounding quotes from the value
                Pattern pattern = Pattern.compile("^\"(.*)\"$");
                Matcher matcher = pattern.matcher(value);
                key = key.replaceAll("\"","");
                if (matcher.matches()) {
                    value = matcher.group(1);
                }
                if(key.equals("filename")){
                    filename = value;
                }else{
                    words.add(value);
                }
            }
            return "{ \"countFunctionsandLines\": " + countFunctionsLines(filename)  +  ", \"occurencesNumbers\": " + occurencesNumbers(words,filename) + " }";
        }

        /** 
         * Generate simple response html page
         * @param httpExchange
         * @param requestParamVaue
         */
        private void handleResponse(HttpExchange httpExchange, String requestParamValue)  throws  IOException {
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
            String requestParamValue=null;
            if("GET".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handleGetRequest(httpExchange);
            }
            else if("POST".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handlePostRequest(httpExchange);
            }
            handleResponse(httpExchange,requestParamValue);

        }
    }
}