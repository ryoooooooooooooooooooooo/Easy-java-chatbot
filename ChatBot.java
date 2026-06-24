import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Chatbot{
    private static final Path KNOWLEDGE_FILE = Paths.get("knowledge.properties");
    private final LinkedHashMap<String, String> knowledge = new LinkedHashMap<>();
    private final Properties props = new Properties();

    public static void main(String[] args){
        ChatBot bot = new ChatBot();
        bot.loadKnowledge();
        bot.run();
    }

    private void loadKnowledge(){
        if(Files.exists(KNOWLEDGE_FILE)){
            try(InputStream in = Files.newInputStream(KNOWLEDGE_FILE)){
                props.load(in);
                for (String k : props.stringPropertyNames()){
                    knowledge.put(k, props.getProperty(k));
                }
                System.out.println("Knowledge loaded: " + knowledge.size() + "entries.");
            }catch(IOException e){
                System.err.println("Failed to load knowledge: " + e.getMessage());
            }
        }else{
            Knowledge.put("hello");
        }
    }
}
    private void saveKnowledge() {
         try (OutputStream out = Files.newOutputStream(KNOWLEDGE_FILE)) {
             Properties p = new Properties();
             p.putAll(knowledge);
             p.store(out, "ChatBot knowledge");
             System.out.println("Knowledge saved (" + knowledge.size() + " entries).");
         } catch (IOException e) {
             System.err.println("Failed to save knowledge: " + e.getMessage());
         }
     }
 
     private void run() throws IOException {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
         System.out.println("CLI ChatBot — type '/help' for commands. Say hi!");
         while (true) {
             System.out.print("> ");
             String line = reader.readLine();
             if (line == null) break;
             line = line.trim();
             if (line.isEmpty()) continue;
 
             if (line.startsWith("/")) {
                 if (handleCommand(line)) continue;
             }
 
             String resp = respond(line);
             System.out.println(resp);
         }
     }
 
     private boolean handleCommand(String line) {
         String cmd = line.split("\\s+", 2)[0];
         switch (cmd) {
             case "/help":
                 printHelp();
                 return true;
             case "/teach":
                 teachCommand(line);
                 return true;
             case "/list":
                 listKnowledge();
                 return true;
             case "/save":
                 saveKnowledge();
                 return true;
             case "/forget":
                 forgetCommand(line);
                 return true;
             case "/exit":
                 saveKnowledge();
                 System.out.println("Bye.");
                 System.exit(0);
             default:
                 System.out.println("Unknown command. /help for list.");
                 return true;
         }
     }
 
     private void printHelp() {
         System.out.println("Commands:");
         System.out.println("  /help               Show this help");
         System.out.println("  /teach pat => resp  Teach pattern -> response (use '/'... '/' for regex)");
         System.out.println("  /list               List learned patterns");
         System.out.println("  /forget pattern     Remove a pattern (exact match)");
         System.out.println("  /save               Save knowledge to file");
         System.out.println("  /exit               Save and exit");
         System.out.println("Usage notes:");
         System.out.println("  - If pattern is like /regex/ it is treated as a Java regex.");
         System.out.println("  - Otherwise pattern is matched as case-insensitive substring or exact match.");
     }
 
     private void teachCommand(String line) {
         String rest = line.length() > 6 ? line.substring(6).trim() : "";
         String[] parts = rest.split("=>", 2);
         if (parts.length < 2) {
             System.out.println("Usage: /teach pattern => response");
             return;
         }
         String pat = parts[0].trim();
         String resp = parts[1].trim();
         if (pat.isEmpty() || resp.isEmpty()) {
             System.out.println("Both pattern and response must be non-empty.");
             return;
         }
         knowledge.put(pat, resp);
         System.out.println("Learned: \"" + pat + "\" => \"" + resp + "\"");
     }
 
     private void forgetCommand(String line) {
         String rest = line.length() > 7 ? line.substring(7).trim() : "";
         if (rest.isEmpty()) {
             System.out.println("Usage: /forget pattern");
             return;
         }
         String toRemove = null;
         for (String k : knowledge.keySet()) {
             if (k.equalsIgnoreCase(rest)) { toRemove = k; break; }
         }
         if (toRemove != null) {
             knowledge.remove(toRemove);
             System.out.println("Forgot: " + toRemove);
         } else {
             System.out.println("Pattern not found.");
         }
     }
 
     private void listKnowledge() {
         if (knowledge.isEmpty()) {
             System.out.println("(no knowledge)");
             return;
         }
         System.out.println("Known patterns:");
         int i = 1;
         for (Map.Entry<String,String> e : knowledge.entrySet()) {
             System.out.printf(" %d) %s => %s%n", i++, e.getKey(), e.getValue());
         }
     }
 
     private String respond(String input) {
         String trimmed = input.trim();
         // 1) exact match (case-insensitive)
         for (String k : knowledge.keySet()) {
             if (k.equalsIgnoreCase(trimmed)) {
                 return knowledge.get(k);
             }
         }
         // 2) regex patterns (/.../)
         for (String k : knowledge.keySet()) {
             if (isRegexKey(k)) {
                 String rx = k.substring(1, k.length()-1);
                 try {
                     if (Pattern.compile(rx, Pattern.CASE_INSENSITIVE).matcher(trimmed).find()) {
                         return knowledge.get(k);
                     }
                 } catch (PatternSyntaxException e) {
                     // ignore invalid regex
                 }
             }
         }
         // 3) substring match (case-insensitive)
         String low = trimmed.toLowerCase(Locale.ROOT);
         for (String k : knowledge.keySet()) {
             if (!isRegexKey(k) && low.contains(k.toLowerCase(Locale.ROOT))) {
                 return knowledge.get(k);
             }
         }
         // 4) built-in intents
         if (low.matches(".*\\b(time|what time)\\b.*")) {
             return "現在時刻: " + java.time.LocalTime.now().withNano(0).toString();
         }
         if (low.matches(".*\\b(date|today)\\b.*")) {
             return "今日の日付: " + java.time.LocalDate.now().toString();
         }
         if (low.matches(".*\\b(joke|ジョーク)\\b.*")) {
             return "Why do programmers prefer dark mode? Because light attracts bugs.";
         }
         // 5) fallback
         return defaultFallback(trimmed);
     }
 
     private boolean isRegexKey(String k) {
         return k.length() >= 2 && k.startsWith("/") && k.endsWith("/");
     }
 
     private String defaultFallback(String input) {
         List<String> replies = Arrays.asList(
             "なるほど。もう少し詳しく教えてくれる？",
             "それについてはまだ学習していません。/teach で教えてください。",
             "うーん、よくわかりません。別の言い方はありますか？"
         );
         return replies.get(Math.abs(input.hashCode()) % replies.size());
     }
 }
