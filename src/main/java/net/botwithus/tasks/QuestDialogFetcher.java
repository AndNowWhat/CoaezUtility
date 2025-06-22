package net.botwithus.tasks;

import net.botwithus.rs3.script.ScriptConsole;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QuestDialogFetcher class for retrieving quest dialog options from RuneScape Wiki.
 * Parses quick guide pages to extract dialog choices and their sequences.
 */
public class QuestDialogFetcher {
    
    private static final String WIKI_BASE_URL = "https://runescape.wiki/w/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    
    /**
     * Represents a dialog choice with its option number and text.
     */
    public static class DialogOption {
        private final String optionNumber;
        private final String optionText;
        
        public DialogOption(String optionNumber, String optionText) {
            this.optionNumber = optionNumber;
            this.optionText = optionText;
        }
        
        public String getOptionNumber() {
            return optionNumber;
        }
        
        public String getOptionText() {
            return optionText;
        }
        
        @Override
        public String toString() {
            return optionNumber + ": " + optionText;
        }
    }
    
    /**
     * Represents a sequence of dialog options for a particular conversation.
     */
    public static class DialogSequence {
        private final List<DialogOption> options;
        private final String context;
        
        public DialogSequence(String context) {
            this.context = context;
            this.options = new ArrayList<>();
        }
        
        public void addOption(DialogOption option) {
            options.add(option);
        }
        
        public List<DialogOption> getOptions() {
            return new ArrayList<>(options);
        }
        
        public String getContext() {
            return context;
        }
        
        public String getSequenceString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(options.get(i).getOptionNumber());
            }
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return "Context: " + context + " | Sequence: " + getSequenceString();
        }
    }
    
    /**
     * Fetches quest dialog options from the RuneScape Wiki quick guide.
     * 
     * @param questName The name of the quest (will be converted to wiki URL format)
     * @return Map of dialog sequences found in the quest guide
     */
    public static Map<String, List<DialogSequence>> fetchQuestDialogs(String questName) {
        Map<String, List<DialogSequence>> dialogMap = new HashMap<>();
        
        try {
            String wikiUrl = buildWikiUrl(questName);
            ScriptConsole.println("[QuestDialogFetcher] Fetching dialogs from: " + wikiUrl);
            
            String htmlContent = fetchHtmlContent(wikiUrl);
            if (htmlContent == null) {
                ScriptConsole.println("[QuestDialogFetcher] Failed to fetch content for: " + questName);
                return dialogMap;
            }
            
            dialogMap = parseDialogOptions(htmlContent);
            ScriptConsole.println("[QuestDialogFetcher] Found " + dialogMap.size() + " dialog sections for: " + questName);
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestDialogFetcher] Error fetching dialogs for " + questName + ": " + e.getMessage());
        }
        
        return dialogMap;
    }
    
    /**
     * Builds the wiki URL for a quest's quick guide.
     */
    private static String buildWikiUrl(String questName) {
        String formattedName = questName.replace(" ", "_")
                                       .replace("'", "%27")
                                       .replace(":", "%3A");
        return WIKI_BASE_URL + formattedName + "/Quick_guide";
    }
    
    /**
     * Fetches HTML content from the given URL.
     */
    private static String fetchHtmlContent(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                ScriptConsole.println("[QuestDialogFetcher] HTTP " + responseCode + " for URL: " + urlString);
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestDialogFetcher] Error fetching HTML: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses HTML content to extract dialog options and sequences.
     */
    private static Map<String, List<DialogSequence>> parseDialogOptions(String htmlContent) {
        Map<String, List<DialogSequence>> dialogMap = new HashMap<>();
        
        // Pattern to match the chat options span with tooltip reference
        Pattern chatOptionsPattern = Pattern.compile(
            "<span class=\"chat-options\">.*?data-tooltip-name=\"([^\"]+)\".*?</span>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        // Pattern to match the corresponding tooltip div with dialog table
        Pattern tooltipDivPattern = Pattern.compile(
            "<div[^>]*data-tooltip-for=\"([^\"]+)\"[^>]*>.*?<table><tbody>(.*?)</tbody></table>.*?</div>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        // Pattern to match chat sequence display (like "1•✓•1")
        Pattern sequenceDisplayPattern = Pattern.compile(
            "<span class=\"chat-options-underline\"[^>]*>([^<]+)</span>",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern to match table rows with dialog options
        Pattern tableRowPattern = Pattern.compile(
            "<tr><td><b>([^<]+)</b></td><td>([^<]+)</td></tr>",
            Pattern.CASE_INSENSITIVE
        );
        
        ScriptConsole.println("[QuestDialogFetcher] Starting to parse HTML content...");
        
        // Find all chat options spans and their corresponding tooltips
        Matcher chatMatcher = chatOptionsPattern.matcher(htmlContent);
        int chatCount = 0;
        
        while (chatMatcher.find()) {
            chatCount++;
            String tooltipName = chatMatcher.group(1);
            String chatOptionsContent = chatMatcher.group(0);
            
            ScriptConsole.println("[QuestDialogFetcher] Found chat options " + chatCount + " with tooltip: " + tooltipName);
            
            // Extract the sequence display from the chat options span
            List<String> sequenceParts = new ArrayList<>();
            Matcher seqMatcher = sequenceDisplayPattern.matcher(chatOptionsContent);
            while (seqMatcher.find()) {
                String part = seqMatcher.group(1).trim();
                // Clean up HTML entities and split by bullet points if they exist
                part = cleanOptionText(part);
                
                // Skip empty parts after cleaning
                if (part.isEmpty()) continue;
                
                // If the part contains bullet points, split it further
                if (part.contains("•")) {
                    String[] subParts = part.split("•");
                    for (String subPart : subParts) {
                        String cleanSubPart = subPart.trim().replace("?", "").trim();
                        if (!cleanSubPart.isEmpty()) {
                            sequenceParts.add(cleanSubPart);
                        }
                    }
                } else {
                    sequenceParts.add(part);
                }
            }
            
            // Create a clean sequence display with proper separators
            String sequenceDisplay = "";
            if (!sequenceParts.isEmpty()) {
                sequenceDisplay = String.join(" → ", sequenceParts);
            }
            ScriptConsole.println("[QuestDialogFetcher] Sequence display: " + sequenceDisplay);
            
            // Find the corresponding tooltip div
            Matcher tooltipMatcher = tooltipDivPattern.matcher(htmlContent);
            while (tooltipMatcher.find()) {
                String divTooltipName = tooltipMatcher.group(1);
                
                if (divTooltipName.equals(tooltipName)) {
                    String tableContent = tooltipMatcher.group(2);
                    ScriptConsole.println("[QuestDialogFetcher] Found matching tooltip div for: " + tooltipName);
                    
                    DialogSequence dialogSeq = new DialogSequence("Chat sequence: " + sequenceDisplay);
                    
                    // Parse table rows to extract dialog options
                    Matcher rowMatcher = tableRowPattern.matcher(tableContent);
                    while (rowMatcher.find()) {
                        String optionNum = rowMatcher.group(1).trim();
                        String optionText = rowMatcher.group(2).trim();
                        
                        // Clean up HTML entities
                        optionNum = cleanOptionText(optionNum);
                        optionText = cleanOptionText(optionText);
                        
                        if (optionNum.equals("?") || optionNum.isEmpty()) {
                            ScriptConsole.println("[QuestDialogFetcher] Skipping continuation dialog: " + optionNum + " = " + optionText);
                            continue;
                        }
                        
                        // Only keep entries with actual numbered options
                        if (!optionNum.matches("\\d+")) {
                            ScriptConsole.println("[QuestDialogFetcher] Skipping non-numbered option: " + optionNum + " = " + optionText);
                            continue;
                        }
                        
                        dialogSeq.addOption(new DialogOption(optionNum, optionText));
                        ScriptConsole.println("[QuestDialogFetcher] Found dialog option: " + optionNum + " = " + optionText);
                    }
                    
                    if (!dialogSeq.getOptions().isEmpty()) {
                        dialogMap.computeIfAbsent("Chat Options", k -> new ArrayList<>()).add(dialogSeq);
                    }
                    break;
                }
            }
        }
        
        ScriptConsole.println("[QuestDialogFetcher] Processed " + chatCount + " chat option sequences");
        
        // Fallback: Original tooltip pattern for other formats
        Pattern oldTooltipPattern = Pattern.compile(
            "<span[^>]*class=\"[^\"]*tooltip[^\"]*\"[^>]*data-tooltip=\"([^\"]+)\"[^>]*>([^<]+)</span>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Pattern dialogOptionPattern = Pattern.compile(
            "([0-9]+):\\s*([^\\n\\r]+)",
            Pattern.MULTILINE
        );
        
        Pattern specialOptionPattern = Pattern.compile(
            "(✓|\\[Accept Quest\\]|\\[Continue\\]):\\s*([^\\n\\r]+)",
            Pattern.MULTILINE
        );
        
        Matcher oldTooltipMatcher = oldTooltipPattern.matcher(htmlContent);
        int oldTooltipCount = 0;
        
        while (oldTooltipMatcher.find()) {
            oldTooltipCount++;
            String tooltipData = oldTooltipMatcher.group(1);
            String displayText = oldTooltipMatcher.group(2).trim();
            
            ScriptConsole.println("[QuestDialogFetcher] Found old-style tooltip " + oldTooltipCount + ": " + displayText);
            
            // Check if this tooltip contains dialog information
            if (tooltipData.contains("Chat") || displayText.matches(".*[0-9]•.*") || displayText.contains("✓")) {
                DialogSequence dialogSeq = new DialogSequence("Dialog sequence: " + displayText);
                
                // Parse dialog options from tooltip data
                Matcher optionMatcher = dialogOptionPattern.matcher(tooltipData);
                while (optionMatcher.find()) {
                    String optionNum = optionMatcher.group(1).trim();
                    String optionText = optionMatcher.group(2).trim();
                    
                    optionText = cleanOptionText(optionText);
                    if (optionNum.isEmpty()) optionNum = "1"; // Default if empty
                    
                    dialogSeq.addOption(new DialogOption(optionNum, optionText));
                    ScriptConsole.println("[QuestDialogFetcher] Found old-style option: " + optionNum + " = " + optionText);
                }
                
                // Also look for special options
                Matcher specialMatcher = specialOptionPattern.matcher(tooltipData);
                while (specialMatcher.find()) {
                    String optionNum = specialMatcher.group(1).trim();
                    String optionText = specialMatcher.group(2).trim();
                    
                    optionText = cleanOptionText(optionText);
                    if (optionNum.isEmpty()) optionNum = "✓"; // Keep checkmark or default
                    
                    dialogSeq.addOption(new DialogOption(optionNum, optionText));
                    ScriptConsole.println("[QuestDialogFetcher] Found old-style special option: " + optionNum + " = " + optionText);
                }
                
                if (!dialogSeq.getOptions().isEmpty()) {
                    dialogMap.computeIfAbsent("Dialog Tooltips", k -> new ArrayList<>()).add(dialogSeq);
                }
            }
        }
        
        // Fallback: Look for chat sequences in the main text
        Pattern chatSequencePattern = Pattern.compile(
            "\\(Chat\\s+([0-9•✓\\-]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher chatSeqMatcher = chatSequencePattern.matcher(htmlContent);
        while (chatSeqMatcher.find()) {
            String sequence = chatSeqMatcher.group(1).trim();
            DialogSequence dialogSeq = new DialogSequence("Chat sequence: " + sequence);
            
            // Parse the sequence (e.g., "1•3•3•1•2•2•3")
            String[] parts = sequence.split("•");
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty() && !part.equals("✓")) {
                    dialogSeq.addOption(new DialogOption(part, "Option " + part));
                }
            }
            
            if (!dialogSeq.getOptions().isEmpty()) {
                dialogMap.computeIfAbsent("Chat Sequences", k -> new ArrayList<>()).add(dialogSeq);
            }
        }
        
        // Legacy table parsing (keep as additional fallback)
        Pattern dialogTablePattern = Pattern.compile(
            "\\(_Chat_\\s+([^)]+)\\).*?<table[^>]*>.*?</table>", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Pattern optionPattern = Pattern.compile(
            "<td[^>]*>\\s*<strong>([^<]+)</strong>\\s*</td>\\s*<td[^>]*>([^<]+)</td>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher tableMatcher = dialogTablePattern.matcher(htmlContent);
        while (tableMatcher.find()) {
            String sequence = tableMatcher.group(1).trim();
            String tableContent = tableMatcher.group(0);
            
            DialogSequence dialogSeq = new DialogSequence("Dialog table: " + sequence);
            
            Matcher optionMatcher = optionPattern.matcher(tableContent);
            while (optionMatcher.find()) {
                String optionNum = optionMatcher.group(1).trim();
                String optionText = optionMatcher.group(2).trim();
                optionText = cleanOptionText(optionText);
                dialogSeq.addOption(new DialogOption(optionNum, optionText));
            }
            
            if (!dialogSeq.getOptions().isEmpty()) {
                dialogMap.computeIfAbsent("Dialog Tables", k -> new ArrayList<>()).add(dialogSeq);
            }
        }
        
        ScriptConsole.println("[QuestDialogFetcher] Total dialog sections found: " + dialogMap.size());
        return dialogMap;
    }
    
    /**
     * Cleans up option text by removing HTML entities and extra whitespace.
     */
    private static String cleanOptionText(String text) {
        if (text == null) return "";
        
        return text.replace("&quot;", "\"")
                  .replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&#39;", "'")
                  .replace("&nbsp;", " ")
                  .replace("&#x2713;", "✓")      // Checkmark
                  .replace("&#8226;", "•")       // Bullet point
                  .replace("&#8230;", "...")     // Ellipsis
                  .replace("&#x27;", "'")        // Apostrophe
                  .replace("&#x3A;", ":")        // Colon
                  .replace("&#91;", "[")         // Left bracket
                  .replace("&#93;", "]")         // Right bracket
                  .replace("&lsquo;", "'")       // Left single quote
                  .replace("&rsquo;", "'")       // Right single quote
                  .replace("&ldquo;", "\"")      // Left double quote
                  .replace("&rdquo;", "\"")      // Right double quote
                  .replace("&hellip;", "...")    // Horizontal ellipsis
                  .replace("&mdash;", "—")       // Em dash
                  .replace("&ndash;", "–")       // En dash
                  .replaceAll("&#x([0-9A-Fa-f]+);", "")  // Remove any remaining hex entities
                  .replaceAll("&#([0-9]+);", "")         // Remove any remaining decimal entities
                  .replaceAll("\\s+", " ")               // Normalize whitespace
                  .trim();
    }
    
    /**
     * Formats dialog information for display.
     */
    public static String formatDialogInfo(Map<String, List<DialogSequence>> dialogMap) {
        if (dialogMap.isEmpty()) {
            return "No dialog options found.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Quest Dialog Options:\n\n");
        
        for (Map.Entry<String, List<DialogSequence>> entry : dialogMap.entrySet()) {
            sb.append("=== ").append(entry.getKey()).append(" ===\n");
            
            for (DialogSequence sequence : entry.getValue()) {
                sb.append(sequence.getContext()).append("\n");
                for (DialogOption option : sequence.getOptions()) {
                    sb.append("  ").append(option.toString()).append("\n");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Test method to demonstrate usage.
     */
    public static void testFetch(String questName) {
        ScriptConsole.println("[QuestDialogFetcher] Testing fetch for: " + questName);
        Map<String, List<DialogSequence>> dialogs = fetchQuestDialogs(questName);
        String formatted = formatDialogInfo(dialogs);
        ScriptConsole.println(formatted);
    }
} 