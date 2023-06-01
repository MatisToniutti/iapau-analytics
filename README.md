# iapau-analytics

code ananylis tool

## Dependencies

openjdk11

## Usage :

  $ javac -cp json.jar Serveur.java
  $ java -cp json.jar Serveur.java

then curl : 

  $ curl -d '{"code":"def if", "word1":"def"}' -H "Content-Type: application/json" -X POST http://localhost:8081/raw


## Deploy 

A docker file is availible



