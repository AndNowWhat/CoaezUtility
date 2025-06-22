package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.quest.Quest;
import net.botwithus.rs3.game.js5.types.QuestType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * QuestHelper class for managing and tracking quests.
 * Provides functionality to list all quests, filter by completion status,
 * and get detailed information about quest requirements.
 */
public class QuestHelper {

    private final CoaezUtility script;
    private Quest selectedQuest;
    private QuestType selectedQuestType;
    private final List<Quest> allQuests;
    private final List<Quest> completedQuests;
    private final List<Quest> inProgressQuests;
    private final List<Quest> notStartedQuests;

    /**
     * Constructs a new QuestHelper.
     * Initializes quest lists and categorizes them by completion status.
     * @param script The main script instance.
     */
    public QuestHelper(CoaezUtility script) {
        this.script = script;
        this.allQuests = new ArrayList<>();
        this.completedQuests = new ArrayList<>();
        this.inProgressQuests = new ArrayList<>();
        this.notStartedQuests = new ArrayList<>();
        loadQuests();
    }

    /**
     * Loads all quests from the game and categorizes them by completion status.
     * This method is called during initialization.
     * Filters out quests that don't have meaningful data.
     */
    private void loadQuests() {
        ScriptConsole.println("[QuestHelper] Loading quests...");
        int filteredCount = 0;
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> questOpt = Quest.byId(i);
            if (questOpt.isPresent()) {
                Quest quest = questOpt.get();
                
                // Only add quests that have meaningful data
                if (isValidQuest(quest, i)) {
                    allQuests.add(quest);
                    
                    // Only check completion status for valid quests with proper QuestType data
                    try {
                        if (quest.isComplete()) {
                            completedQuests.add(quest);
                        } else if (quest.isStarted()) {
                            inProgressQuests.add(quest);
                        } else {
                            notStartedQuests.add(quest);
                        }
                    } catch (NullPointerException e) {
                        // If we can't determine status due to missing QuestType, assume not started
                        ScriptConsole.println("[QuestHelper] Warning: Could not determine status for quest " + quest.name() + " (ID: " + i + "), assuming not started");
                        notStartedQuests.add(quest);
                    }
                } else {
                    filteredCount++;
                    ScriptConsole.println("[QuestHelper] Filtered out quest ID " + i + ": " + quest.name() + " (insufficient data)");
                }
            }
        }
        
        Comparator<Quest> byName = Comparator.comparing(Quest::name);
        Collections.sort(allQuests, byName);
        Collections.sort(completedQuests, byName);
        Collections.sort(inProgressQuests, byName);
        Collections.sort(notStartedQuests, byName);
        
        ScriptConsole.println("[QuestHelper] Loaded " + allQuests.size() + " quests (filtered out " + filteredCount + " quests with insufficient data)");
    }

    /**
     * Validates if a quest has meaningful data and should be included in the quest lists.
     * Filters out quests that have empty or minimal quest data.
     * 
     * @param quest The quest to validate
     * @param questId The quest ID
     * @return true if the quest has meaningful data, false otherwise
     */
    private boolean isValidQuest(Quest quest, int questId) {
        if (quest == null || quest.name() == null || quest.name().trim().isEmpty()) {
            return false;
        }
        
        // Try to get QuestType data for additional validation
        QuestType questType = ConfigManager.getQuestType(questId);
        if (questType == null) {
            // If no QuestType data, check if the Quest object itself has meaningful data
            return hasBasicQuestData(quest);
        }
        
        // Check if QuestType has meaningful data
        Map<Integer, Object> params = questType.params();
        if (params == null || params.isEmpty()) {
            // No parameters, check if other QuestType data is meaningful
            return hasBasicQuestTypeData(questType) || hasBasicQuestData(quest);
        }
        
        // Check if parameters contain meaningful data (not just empty strings)
        boolean hasMeaningfulParams = false;
        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value != null && !value.toString().trim().isEmpty()) {
                // Skip the name parameter if it's the only non-empty one and matches quest name
                if (entry.getKey() == 1 && value.toString().equals(quest.name())) {
                    continue;
                }
                hasMeaningfulParams = true;
                break;
            }
        }
        
        return hasMeaningfulParams || hasBasicQuestTypeData(questType) || hasBasicQuestData(quest);
    }
    
    /**
     * Checks if the Quest object has basic meaningful data.
     * Safely handles Quest objects that might have missing QuestType data.
     * 
     * @param quest The quest to check
     * @return true if the quest has basic data, false otherwise
     */
    private boolean hasBasicQuestData(Quest quest) {
        try {
            // Check if quest has any meaningful properties
            return quest.getQuestPoints() > 0 || 
                   quest.getQuestPointReq() > 0 || 
                   !quest.getRequiredSkills().isEmpty() || 
                   !quest.getRequiredQuests().isEmpty();
        } catch (NullPointerException e) {
            // If we can't access quest data due to missing QuestType, consider it invalid
            return false;
        }
    }
    
    /**
     * Checks if the QuestType has basic meaningful data beyond just the name.
     * 
     * @param questType The QuestType to check
     * @return true if the QuestType has meaningful data, false otherwise
     */
    private boolean hasBasicQuestTypeData(QuestType questType) {
        return questType.questPoints() > 0 ||
               questType.questPointReq() > 0 ||
               (questType.skillRequirments() != null && questType.skillRequirments().length > 0) ||
               (questType.dependentQuests() != null && questType.dependentQuests().length > 0) ||
               (questType.startLocations() != null && questType.startLocations().length > 0) ||
               questType.category() > 0 ||
               questType.difficulty() > 0 ||
               questType.questItemSprite() > 0;
    }

    /**
     * Gets a list of all quests.
     * @return An unmodifiable list of all quests.
     */
    public List<Quest> getAllQuests() {
        return Collections.unmodifiableList(allQuests);
    }

    /**
     * Gets a list of completed quests.
     * @return An unmodifiable list of completed quests.
     */
    public List<Quest> getCompletedQuests() {
        return Collections.unmodifiableList(completedQuests);
    }

    /**
     * Gets a list of quests in progress.
     * @return An unmodifiable list of quests in progress.
     */
    public List<Quest> getInProgressQuests() {
        return Collections.unmodifiableList(inProgressQuests);
    }

    /**
     * Gets a list of quests not started.
     * @return An unmodifiable list of quests not started.
     */
    public List<Quest> getNotStartedQuests() {
        return Collections.unmodifiableList(notStartedQuests);
    }

    /**
     * Sets the selected quest and loads its QuestType data.
     * @param quest The quest to select.
     */
    public void setSelectedQuest(Quest quest) {
        this.selectedQuest = quest;
        
        if (quest != null) {
            int questId = getQuestId(quest);
            ScriptConsole.println("[QuestHelper] Selected quest: " + quest.name() + " (ID: " + questId + ")");
            
            if (questId != -1) {
                this.selectedQuestType = ConfigManager.getQuestType(questId);
                
                if (this.selectedQuestType != null) {
                    ScriptConsole.println("[QuestHelper] QuestType loaded for ID: " + questId);
                    
                    Map<Integer, Object> params = this.selectedQuestType.params();
                    if (params != null && !params.isEmpty()) {
                        ScriptConsole.println("[QuestHelper] Quest " + questId + " params:");
                        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                            Object value = entry.getValue();
                            if (value != null) {
                                ScriptConsole.println("  Param " + entry.getKey() + ": " + value + " (" + value.getClass().getSimpleName() + ")");
                            }
                        }
                    } else {
                        ScriptConsole.println("[QuestHelper] Quest " + questId + " has no params");
                    }
                } else {
                    ScriptConsole.println("[QuestHelper] No QuestType data for ID: " + questId);
                }
            } else {
                this.selectedQuestType = null;
                ScriptConsole.println("[QuestHelper] Could not determine quest ID for: " + quest.name());
            }
        } else {
            this.selectedQuestType = null;
        }
    }

    /**
     * Gets the currently selected quest.
     * @return The selected quest, or null if none is selected.
     */
    public Quest getSelectedQuest() {
        return selectedQuest;
    }
    
    /**
     * Gets the QuestType for the currently selected quest.
     * @return The QuestType for the selected quest, or null if none is selected or QuestType couldn't be loaded.
     */
    public QuestType getSelectedQuestType() {
        return selectedQuestType;
    }

    /**
     * Gets a formatted string of quest requirements for the selected quest.
     * @return A string containing skill requirements, quest requirements, and other requirements.
     */
    public String getQuestRequirementsInfo() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Requirements for ").append(selectedQuest.name()).append(":\n");
        
        int qpReq = selectedQuest.getQuestPointReq();
        if (qpReq > 0) {
            sb.append("- ").append(qpReq).append(" Quest Points\n");
        }
        
        List<net.botwithus.rs3.game.skills.Skills> skillReqs = selectedQuest.getRequiredSkills();
        if (!skillReqs.isEmpty()) {
            sb.append("Skill Requirements:\n");
            for (net.botwithus.rs3.game.skills.Skills skill : skillReqs) {
                sb.append("- ").append(skill.toString()).append("\n");
            }
        }
        
        List<Quest> questReqs = selectedQuest.getRequiredQuests();
        if (!questReqs.isEmpty()) {
            sb.append("Quest Requirements:\n");
            for (Quest quest : questReqs) {
                sb.append("- ").append(quest.name());
                if (quest.isComplete()) {
                    sb.append(" (Completed)");
                } else if (quest.isStarted()) {
                    sb.append(" (In Progress)");
                } else {
                    sb.append(" (Not Started)");
                }
                sb.append("\n");
            }
        }
        
        sb.append("\nDifficulty: ").append(selectedQuest.getDifficulty()).append("\n");
        sb.append("Members Only: ").append(selectedQuest.isMembers() ? "Yes" : "No").append("\n");
        sb.append("Quest Points Reward: ").append(selectedQuest.getQuestPoints()).append("\n");
        sb.append("Player has all requirements: ").append(selectedQuest.hasRequirements() ? "Yes" : "No").append("\n");
        
        return sb.toString();
    }
    
    /**
     * Converts a packed location value to readable coordinates.
     * @param value The packed location value
     * @return A formatted string with x, y, z coordinates
     */
    private String convertLocationToCoordinates(int value) {
        int x = (value >> 14) & 0x3fff;
        int y = value & 0x3fff;
        int z = value >> 28;
        return String.format("(%d, %d, %d)", x, y, z);
    }

    /**
     * Gets a formatted string of quest parameters from QuestType.
     * @return A string containing quest parameters.
     */
    public String getQuestTypeInfo() {
        if (selectedQuestType == null) {
            return "No QuestType data available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("QuestType Information for ").append(selectedQuestType.name()).append(":\n\n");
        
        sb.append("Category: ").append(selectedQuestType.category()).append("\n");
        sb.append("Difficulty: ").append(selectedQuestType.difficulty()).append("\n");
        sb.append("Members Only: ").append(selectedQuestType.membersOnly()).append("\n");
        sb.append("Quest Points: ").append(selectedQuestType.questPoints()).append("\n");
        sb.append("Quest Point Req: ").append(selectedQuestType.questPointReq()).append("\n");
        sb.append("Quest Item Sprite: ").append(selectedQuestType.questItemSprite()).append("\n");
        
        int[] startLocs = selectedQuestType.startLocations();
        if (startLocs != null && startLocs.length > 0) {
            sb.append("\nStart Locations:\n");
            for (int loc : startLocs) {
                sb.append("- ").append(convertLocationToCoordinates(loc)).append(" (Raw: ").append(loc).append(")\n");
            }
        }
        
        int[] depQuests = selectedQuestType.dependentQuests();
        if (depQuests != null && depQuests.length > 0) {
            sb.append("\nDependent Quests:\n");
            for (int questId : depQuests) {
                Optional<Quest> quest = Quest.byId(questId);
                String questName = quest.isPresent() ? quest.get().name() : "Unknown Quest";
                sb.append("- ").append(questName).append(" (ID: ").append(questId).append(")\n");
            }
        }
        
        int[][] skillReqs = selectedQuestType.skillRequirments();
        if (skillReqs != null && skillReqs.length > 0) {
            sb.append("\nSkill Requirements:\n");
            for (int[] skillReq : skillReqs) {
                if (skillReq.length >= 2) {
                    int skillId = skillReq[0];
                    int level = skillReq[1];
                    sb.append("- Skill ID ").append(skillId).append(": Level ").append(level).append("\n");
                }
            }
        }
        
        Map<Integer, Object> params = selectedQuestType.params();
        if (params != null && !params.isEmpty()) {
            sb.append("\nAdditional Parameters:\n");
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                sb.append("- Param ").append(entry.getKey()).append(": ");
                if (entry.getValue() != null) {
                    sb.append(entry.getValue().toString());
                } else {
                    sb.append("null");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * Gets a list of quest names with their IDs for display in the GUI.
     * @return A list of strings in the format "Quest Name [ID]".
     */
    public List<String> getQuestDisplayNames() {
        return allQuests.stream()
            .map(q -> q.name() + " [" + getQuestId(q) + "]")
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the ID of a quest by using the byId method and checking if it matches.
     * This is a workaround since the Quest class doesn't directly expose the ID.
     * @param quest The quest to get the ID for.
     * @return The quest ID, or -1 if not found.
     */
    private int getQuestId(Quest quest) {
        if (quest == null) {
            return -1;
        }
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> testQuest = Quest.byId(i);
            if (testQuest.isPresent() && testQuest.get().name().equals(quest.name())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Gets a quest by its ID.
     * @param questId The quest ID.
     * @return The Quest object, or null if not found.
     */
    public Quest getQuestById(int questId) {
        Optional<Quest> questOpt = Quest.byId(questId);
        return questOpt.orElse(null);
    }
    
    /**
     * Gets a quest by its name.
     * @param questName The quest name.
     * @return The Quest object, or null if not found.
     */
    public Quest getQuestByName(String questName) {
        return allQuests.stream()
            .filter(q -> q.name().equalsIgnoreCase(questName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets comprehensive quest information combining Quest and QuestType data.
     * Shows what data is available and gracefully handles missing information.
     * @return A formatted string with all available quest information.
     */
    public String getComprehensiveQuestInfo() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Quest Information for ").append(selectedQuest.name()).append(":\n\n");
        
        sb.append("=== Basic Information ===\n");
        sb.append("Status: ");
        if (selectedQuest.isComplete()) {
            sb.append("Completed\n");
        } else if (selectedQuest.isStarted()) {
            sb.append("In Progress\n");
        } else {
            sb.append("Not Started\n");
        }
        
        sb.append("Quest Points Reward: ").append(selectedQuest.getQuestPoints()).append("\n");
        sb.append("Members Only: ").append(selectedQuest.isMembers() ? "Yes" : "No").append("\n");
        sb.append("Difficulty: ").append(selectedQuest.getDifficulty()).append("\n");
        
        sb.append("\n=== Requirements ===\n");
        int qpReq = selectedQuest.getQuestPointReq();
        if (qpReq > 0) {
            sb.append("Quest Points Required: ").append(qpReq).append("\n");
        }
        
        List<net.botwithus.rs3.game.skills.Skills> skillReqs = selectedQuest.getRequiredSkills();
        if (!skillReqs.isEmpty()) {
            sb.append("Skill Requirements:\n");
            for (net.botwithus.rs3.game.skills.Skills skill : skillReqs) {
                sb.append("- ").append(skill.toString()).append("\n");
            }
        }
        
        List<Quest> questReqs = selectedQuest.getRequiredQuests();
        if (!questReqs.isEmpty()) {
            sb.append("Quest Requirements:\n");
            for (Quest quest : questReqs) {
                sb.append("- ").append(quest.name());
                if (quest.isComplete()) {
                    sb.append(" (Completed)");
                } else if (quest.isStarted()) {
                    sb.append(" (In Progress)");
                } else {
                    sb.append(" (Not Started)");
                }
                sb.append("\n");
            }
        }
        
        sb.append("Player has all requirements: ").append(selectedQuest.hasRequirements() ? "Yes" : "No").append("\n");
        
        if (selectedQuestType != null) {
            sb.append("\n=== Advanced Quest Data ===\n");
            sb.append("Category: ").append(selectedQuestType.category()).append("\n");
            sb.append("Quest Item Sprite: ").append(selectedQuestType.questItemSprite()).append("\n");
            
            int[] startLocs = selectedQuestType.startLocations();
            if (startLocs != null && startLocs.length > 0) {
                sb.append("\nStart Locations:\n");
                for (int loc : startLocs) {
                    sb.append("- ").append(convertLocationToCoordinates(loc)).append(" (Raw: ").append(loc).append(")\n");
                }
            }
            
            int[] depQuests = selectedQuestType.dependentQuests();
            if (depQuests != null && depQuests.length > 0) {
                sb.append("\nDependent Quests:\n");
                for (int questId : depQuests) {
                    Optional<Quest> quest = Quest.byId(questId);
                    String questName = quest.isPresent() ? quest.get().name() : "Unknown Quest";
                    sb.append("- ").append(questName).append(" (ID: ").append(questId).append(")\n");
                }
            }
            
            int[][] skillReqsDetailed = selectedQuestType.skillRequirments();
            if (skillReqsDetailed != null && skillReqsDetailed.length > 0) {
                sb.append("\nDetailed Skill Requirements:\n");
                for (int[] skillReq : skillReqsDetailed) {
                    if (skillReq.length >= 2) {
                        int skillId = skillReq[0];
                        int level = skillReq[1];
                        sb.append("- Skill ID ").append(skillId).append(": Level ").append(level).append("\n");
                    }
                }
            }
            
            Map<Integer, Object> params = selectedQuestType.params();
            if (params != null && !params.isEmpty()) {
                sb.append("\nAdditional Parameters:\n");
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    sb.append("- Param ").append(entry.getKey()).append(": ");
                    if (entry.getValue() != null) {
                        sb.append(entry.getValue().toString());
                    } else {
                        sb.append("null");
                    }
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\n=== Advanced Quest Data ===\n");
            sb.append("No advanced quest data available for this quest.\n");
            sb.append("This quest may be older or have limited configuration data.\n");
            
            int questId = getQuestId(selectedQuest);
            sb.append("Quest ID: ").append(questId).append("\n");
            if (questId == -1) {
                sb.append("Issue: Could not determine quest ID\n");
            } else {
                sb.append("Issue: ConfigManager.getQuestType(").append(questId).append(") returned null\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * Fetches quest dialog options from the RuneScape Wiki and prints them to console.
     * This method is for testing the dialog fetching functionality.
     */
    public void fetchAndPrintQuestDialogs() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] No quest selected for dialog fetching.");
            return;
        }
        
        String questName = selectedQuest.name();
        ScriptConsole.println("[QuestHelper] Fetching dialog options for quest: " + questName);
        
        try {
            Map<String, List<QuestDialogFetcher.DialogSequence>> dialogs = 
                QuestDialogFetcher.fetchQuestDialogs(questName);
            
            if (dialogs.isEmpty()) {
                ScriptConsole.println("[QuestHelper] No dialog options found for: " + questName);
                return;
            }
            
            ScriptConsole.println("[QuestHelper] === DIALOG OPTIONS FOR: " + questName + " ===");
            
            for (Map.Entry<String, List<QuestDialogFetcher.DialogSequence>> entry : dialogs.entrySet()) {
                ScriptConsole.println("[QuestHelper] " + entry.getKey() + ":");
                
                for (QuestDialogFetcher.DialogSequence sequence : entry.getValue()) {
                    ScriptConsole.println("[QuestHelper]   " + sequence.getContext());
                    
                    for (QuestDialogFetcher.DialogOption option : sequence.getOptions()) {
                        ScriptConsole.println("[QuestHelper]     " + option.getOptionNumber() + ": " + option.getOptionText());
                    }
                    
                    if (!sequence.getOptions().isEmpty()) {
                        ScriptConsole.println("[QuestHelper]   Sequence: " + sequence.getSequenceString());
                    }
                    ScriptConsole.println("[QuestHelper]   ---");
                }
            }
            
            ScriptConsole.println("[QuestHelper] === END DIALOG OPTIONS ===");
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error fetching dialogs for " + questName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets quest dialog options as a formatted string.
     * @return A formatted string containing dialog options, or an error message if none found.
     */
    public String getQuestDialogOptions() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        try {
            Map<String, List<QuestDialogFetcher.DialogSequence>> dialogs = 
                QuestDialogFetcher.fetchQuestDialogs(selectedQuest.name());
            
            if (dialogs.isEmpty()) {
                return "No dialog options found for: " + selectedQuest.name();
            }
            
            return QuestDialogFetcher.formatDialogInfo(dialogs);
            
        } catch (Exception e) {
            return "Error fetching dialog options: " + e.getMessage();
        }
    }
} 