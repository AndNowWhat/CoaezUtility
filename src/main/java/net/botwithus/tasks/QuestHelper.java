package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.api.game.hud.Dialog;
import net.botwithus.rs3.game.quest.Quest;
import net.botwithus.rs3.game.js5.types.QuestType;
import net.botwithus.rs3.game.js5.types.configs.ConfigManager;
import net.botwithus.rs3.script.ScriptConsole;
// Navigation imports
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * QuestHelper class for managing and tracking quests.
 * Provides functionality to list all quests, filter by completion status,
 * and get detailed information about quest requirements.
 * Also provides main loop functionality for dialog assistance.
 */
public class QuestHelper implements Task {

    private final CoaezUtility script;
    private Quest selectedQuest;
    private QuestType selectedQuestType;
    private final List<Quest> allQuests;
    private final List<Quest> completedQuests;
    private final List<Quest> inProgressQuests;
    private final List<Quest> notStartedQuests;
    
    // Quest ID mapping for efficient lookup
    private final Map<Quest, Integer> questToIdMap;
    
    // Dialog assistance fields
    private Map<String, List<QuestDialogFetcher.DialogSequence>> fetchedDialogs;
    private boolean dialogsFetched = false;
    private String currentRecommendedOption = null;
    private int currentRecommendedOptionIndex = -1;
    private boolean isDialogAssistanceActive = false;
    private String currentDialogText = null;
    private List<String> previousDialogOptions = null;
    
    // GUI state management fields
    private List<String> questDisplayNames = new ArrayList<>();
    private boolean showCompletedQuests = true;
    private boolean showInProgressQuests = true;
    private boolean showNotStartedQuests = true;
    private boolean showFreeToPlayQuests = true;
    private boolean showMembersQuests = true;
    private String questSearchText = "";
    private int selectedQuestIndex = 0;

    private String cachedComprehensiveInfo = null;
    private Quest lastCachedQuest = null;
    
    // Navigation fields
    private boolean isNavigatingToQuestStart = false;
    private Area questStartArea = null;
    private Coordinate questStartCoordinate = null;

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
        this.fetchedDialogs = new HashMap<>();
        this.questToIdMap = new HashMap<>();
        loadQuests();
    }

    /**
     * Loads all quests from the game and categorizes them by completion status.
     * This method is called during initialization.
     * Filters out quests that don't have meaningful data and handles duplicate quest names
     * by preferring the quest entry with more comprehensive data.
     */
    private void loadQuests() {
        ScriptConsole.println("[QuestHelper] Loading quests...");
        int filteredCount = 0;
        
        Map<String, QuestCandidate> questCandidates = new HashMap<>();
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> questOpt = Quest.byId(i);
            if (questOpt.isPresent()) {
                Quest quest = questOpt.get();
                
                if (isValidQuest(quest, i)) {
                    String questName = quest.name();
                    QuestType questType = ConfigManager.getQuestType(i);
                    int dataRichness = calculateQuestDataRichness(quest, questType, i);
                    
                    QuestCandidate candidate = new QuestCandidate(quest, questType, i, dataRichness);
                    
                    QuestCandidate existingCandidate = questCandidates.get(questName);
                    if (existingCandidate == null || candidate.dataRichness > existingCandidate.dataRichness) {
                        if (existingCandidate != null) {
                            ScriptConsole.println("[QuestHelper] Found duplicate quest '" + questName + 
                                                 "': Replacing ID " + existingCandidate.questId + 
                                                 " (richness: " + existingCandidate.dataRichness + 
                                                 ") with ID " + i + " (richness: " + dataRichness + ")");
                        }
                        questCandidates.put(questName, candidate);
                    } else {
                        ScriptConsole.println("[QuestHelper] Found duplicate quest '" + questName + 
                                             "': Keeping ID " + existingCandidate.questId + 
                                             " (richness: " + existingCandidate.dataRichness + 
                                             ") over ID " + i + " (richness: " + dataRichness + ")");
                    }
                } else {
                    filteredCount++;
                    ScriptConsole.println("[QuestHelper] Filtered out quest ID " + i + ": " + quest.name() + " (insufficient data)");
                }
            }
        }
        
        for (QuestCandidate candidate : questCandidates.values()) {
            Quest quest = candidate.quest;
            allQuests.add(quest);
            
            try {
                if (quest.isComplete()) {
                    completedQuests.add(quest);
                } else if (quest.isStarted()) {
                    inProgressQuests.add(quest);
                } else {
                    notStartedQuests.add(quest);
                }
            } catch (NullPointerException e) {
                ScriptConsole.println("[QuestHelper] Warning: Could not determine status for quest " + quest.name() + " (ID: " + candidate.questId + "), assuming not started");
                notStartedQuests.add(quest);
            }
            
            questToIdMap.put(quest, candidate.questId);
        }
        
        Comparator<Quest> byName = Comparator.comparing(Quest::name);
        Collections.sort(allQuests, byName);
        Collections.sort(completedQuests, byName);
        Collections.sort(inProgressQuests, byName);
        Collections.sort(notStartedQuests, byName);
        
        ScriptConsole.println("[QuestHelper] Loaded " + allQuests.size() + " quests (filtered out " + filteredCount + " quests with insufficient data)");
    }

    /**
     * Helper class to track quest candidates with their data richness scores.
     */
    private static class QuestCandidate {
        final Quest quest;
        final QuestType questType;
        final int questId;
        final int dataRichness;
        
        QuestCandidate(Quest quest, QuestType questType, int questId, int dataRichness) {
            this.quest = quest;
            this.questType = questType;
            this.questId = questId;
            this.dataRichness = dataRichness;
        }
    }
    
    /**
     * Calculates a richness score for quest data to help prefer more complete quest entries.
     * Higher scores indicate more comprehensive data.
     * 
     * @param quest The quest object
     * @param questType The quest type (may be null)
     * @param questId The quest ID
     * @return A richness score (higher = more data)
     */
    private int calculateQuestDataRichness(Quest quest, QuestType questType, int questId) {
        int score = 0;
        
        try {
            if (quest.getQuestPoints() > 0) score += 10;
            if (quest.getQuestPointReq() > 0) score += 5;
            if (!quest.getRequiredSkills().isEmpty()) score += 15;
            if (!quest.getRequiredQuests().isEmpty()) score += 15;
        } catch (Exception e) {
        }
        
        if (questType != null) {
            score += 20;
            
            if (questType.questPoints() > 0) score += 10;
            if (questType.questPointReq() > 0) score += 5;
            if (questType.skillRequirments() != null && questType.skillRequirments().length > 0) score += 20;
            if (questType.dependentQuests() != null && questType.dependentQuests().length > 0) score += 20;
            if (questType.startLocations() != null && questType.startLocations().length > 0) score += 25;
            if (questType.category() > 0) score += 5;
            if (questType.difficulty() > 0) score += 5;
            if (questType.questItemSprite() > 0) score += 5;
            
            Map<Integer, Object> params = questType.params();
            if (params != null && !params.isEmpty()) {
                score += 30;
                
                int meaningfulParams = 0;
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !value.toString().trim().isEmpty()) {
                        if (entry.getKey() == 1 && value.toString().equals(quest.name())) {
                            continue;
                        }
                        meaningfulParams++;
                    }
                }
                score += meaningfulParams * 3;
            }
        }
        
        return score;
    }

    /**
     * Validates if a quest has meaningful data and should be included in the quest lists.
     * Now less restrictive to allow quests that can be used for dialog assistance.
     * 
     * @param quest The quest to validate
     * @param questId The quest ID
     * @return true if the quest has meaningful data, false otherwise
     */
    private boolean isValidQuest(Quest quest, int questId) {
        if (quest == null || quest.name() == null || quest.name().trim().isEmpty()) {
            return false;
        }
        
        String questName = quest.name().trim();
        
        if (questName.equalsIgnoreCase("null") || 
            questName.equalsIgnoreCase("unknown") || 
            questName.equalsIgnoreCase("test") ||
            questName.length() < 2) {
            return false;
        }
        
        QuestType questType = ConfigManager.getQuestType(questId);
        if (questType != null) {
            Map<Integer, Object> params = questType.params();
            if (params != null && !params.isEmpty()) {
                boolean hasMeaningfulParams = false;
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !value.toString().trim().isEmpty()) {
                        if (entry.getKey() == 1 && value.toString().equals(quest.name())) {
                            continue;
                        }
                        hasMeaningfulParams = true;
                        break;
                    }
                }
                
                if (hasMeaningfulParams || hasBasicQuestTypeData(questType)) {
                    return true;
                }
            }
            
            if (hasBasicQuestTypeData(questType)) {
                return true;
            }
        }
        
        if (hasBasicQuestData(quest)) {
            return true;
        }
        
        return questName.length() >= 3 && !questName.matches(".*\\d{3,}.*"); // Exclude names with long numbers
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
            return quest.getQuestPoints() > 0 || 
                   quest.getQuestPointReq() > 0 || 
                   !quest.getRequiredSkills().isEmpty() || 
                   !quest.getRequiredQuests().isEmpty();
        } catch (NullPointerException e) {
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
        
        cachedComprehensiveInfo = null;
        lastCachedQuest = null;
        
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
        
        try {
            sb.append("Player has all requirements: ").append(selectedQuest.hasRequirements() ? "Yes" : "No").append("\n");
        } catch (Exception e) {
            sb.append("Player has all requirements: Unable to determine (quest data incomplete)\n");
        }
        
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
        return Collections.unmodifiableList(questDisplayNames);
    }
    
    /**
     * Gets the ID of a quest by using the efficient questToIdMap.
     * This method uses the same logic as loadQuests to prefer quests with richer data
     * when multiple quest IDs have the same name.
     * @param quest The quest to get the ID for.
     * @return The quest ID, or -1 if not found.
     */
    private int getQuestId(Quest quest) {
        if (quest == null) {
            return -1;
        }
        
        Integer questId = questToIdMap.get(quest);
        if (questId != null) {
            return questId;
        }
        
        String questName = quest.name();
        int bestQuestId = -1;
        int bestDataRichness = -1;
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> testQuest = Quest.byId(i);
            if (testQuest.isPresent() && testQuest.get().name().equals(questName)) {
                QuestType questType = ConfigManager.getQuestType(i);
                int dataRichness = calculateQuestDataRichness(testQuest.get(), questType, i);
                
                if (dataRichness > bestDataRichness) {
                    bestQuestId = i;
                    bestDataRichness = dataRichness;
                }
            }
        }
        
        return bestQuestId;
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
     * Uses caching to prevent expensive operations from running every frame.
     * @return A formatted string with all available quest information.
     */
    public String getComprehensiveQuestInfo() {
        if (selectedQuest == null) {
            return "No quest selected";
        }
        
        if (cachedComprehensiveInfo != null && selectedQuest.equals(lastCachedQuest)) {
            return cachedComprehensiveInfo;
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
        
        try {
            List<net.botwithus.rs3.game.skills.Skills> skillReqs = selectedQuest.getRequiredSkills();
            if (skillReqs != null && !skillReqs.isEmpty()) {
                sb.append("Skill Requirements:\n");
                for (net.botwithus.rs3.game.skills.Skills skill : skillReqs) {
                    sb.append("- ").append(skill.toString()).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("Skill Requirements: Unable to load (quest data incomplete)\n");
        }
        
        try {
            List<Quest> questReqs = selectedQuest.getRequiredQuests();
            if (questReqs != null && !questReqs.isEmpty()) {
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
        } catch (Exception e) {
            sb.append("Quest Requirements: Unable to load (quest data incomplete)\n");
        }
        
        try {
            sb.append("Player has all requirements: ").append(selectedQuest.hasRequirements() ? "Yes" : "No").append("\n");
        } catch (Exception e) {
            sb.append("Player has all requirements: Unable to determine (quest data incomplete)\n");
        }
        
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
        
        cachedComprehensiveInfo = sb.toString();
        lastCachedQuest = selectedQuest;
        
        return cachedComprehensiveInfo;
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

    /**
     * Main execution method for the Task interface.
     * Implements the dialog assistance loop for the selected quest.
     */
    @Override
    public void execute() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] No quest selected for dialog assistance.");
            return;
        }
        
        if (isNavigatingToQuestStart) {
            processQuestStartNavigation();
        }
        
        if (!dialogsFetched) {
            fetchDialogsForSelectedQuest();
        }
        
        if (Dialog.isOpen()) {
            processOpenDialog();
        } else {
            clearCurrentRecommendation();
        }
    }
    
    /**
     * Fetches dialog options for the currently selected quest.
     */
    private void fetchDialogsForSelectedQuest() {
        if (selectedQuest == null) {
            return;
        }
        
        try {
            ScriptConsole.println("[QuestHelper] Fetching dialogs for: " + selectedQuest.name());
            fetchedDialogs = QuestDialogFetcher.fetchQuestDialogs(selectedQuest.name());
            dialogsFetched = true;
            
            if (fetchedDialogs.isEmpty()) {
                ScriptConsole.println("[QuestHelper] No dialogs found for: " + selectedQuest.name());
            } else {
                ScriptConsole.println("[QuestHelper] Successfully fetched dialogs for: " + selectedQuest.name());
                ScriptConsole.println("[QuestHelper] Found " + fetchedDialogs.size() + " dialog sections");
            }
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error fetching dialogs: " + e.getMessage());
            dialogsFetched = false;
        }
    }
    
    /**
     * Process the open dialog and find recommendations
     */
    public void processOpenDialog() {
        if (fetchedDialogs == null || fetchedDialogs.isEmpty()) {
            clearCurrentRecommendation();
            return;
        }
        
        try {
            List<String> currentDialogOptions = Dialog.getOptions();
            if (currentDialogOptions == null || currentDialogOptions.isEmpty()) {
                clearCurrentRecommendation();
                currentDialogText = null;
                previousDialogOptions = null;
                return;
            }
            
            boolean optionsChanged = !currentDialogOptions.equals(previousDialogOptions);
            
            if (optionsChanged) {
                clearCurrentRecommendation();
                ScriptConsole.println("[QuestHelper] Dialog options changed, clearing previous recommendation");
                
                currentDialogText = String.join(" | ", currentDialogOptions);
                
                findDialogRecommendation(currentDialogOptions);
                
                previousDialogOptions = new ArrayList<>(currentDialogOptions);
            }
            
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error processing dialog: " + e.getMessage());
        }
    }
    
    /**
     * Finds and sets the recommended dialog option based on current dialog options and fetched dialog sequences.
     * Only recommends options that are actually present in the current dialog.
     * @param currentDialogOptions The options currently displayed in the dialog
     */
    private void findDialogRecommendation(List<String> currentDialogOptions) {
        if (currentDialogOptions == null || currentDialogOptions.isEmpty()) {
            clearCurrentRecommendation();
            return;
        }
        
        ScriptConsole.println("[QuestHelper] Looking for matches in current dialog options:");
        for (int i = 0; i < currentDialogOptions.size(); i++) {
            ScriptConsole.println("[QuestHelper]   Option " + (i + 1) + ": '" + currentDialogOptions.get(i) + "'");
        }
        
        List<MatchCandidate> candidates = new ArrayList<>();
        
        for (Map.Entry<String, List<QuestDialogFetcher.DialogSequence>> entry : fetchedDialogs.entrySet()) {
            String sectionName = entry.getKey();
            List<QuestDialogFetcher.DialogSequence> sequences = entry.getValue();
            
            ScriptConsole.println("[QuestHelper] Checking section: " + sectionName);
            
            for (int seqIndex = 0; seqIndex < sequences.size(); seqIndex++) {
                QuestDialogFetcher.DialogSequence sequence = sequences.get(seqIndex);
                List<QuestDialogFetcher.DialogOption> options = sequence.getOptions();
                
                ScriptConsole.println("[QuestHelper]   Sequence " + seqIndex + " has " + options.size() + " options");
                
                for (int optIndex = 0; optIndex < options.size(); optIndex++) {
                    QuestDialogFetcher.DialogOption option = options.get(optIndex);
                    String optionText = option.getOptionText();
                    if (optionText == null || optionText.trim().isEmpty()) {
                        continue;
                    }
                    
                    ScriptConsole.println("[QuestHelper]     Checking fetched option: '" + optionText + "'");
                    
                    int matchingIndex = findMatchingOptionIndex(currentDialogOptions, optionText);
                    if (matchingIndex != -1) {
                        ScriptConsole.println("[QuestHelper]     ✓ MATCH FOUND at current dialog index " + matchingIndex);
                        candidates.add(new MatchCandidate(optionText, matchingIndex, sectionName, seqIndex, optIndex));
                    } else {
                        ScriptConsole.println("[QuestHelper]     ✗ No match");
                    }
                }
            }
        }
        
        if (!candidates.isEmpty()) {
            MatchCandidate bestMatch = candidates.get(0);
            setCurrentRecommendation(bestMatch.optionText, bestMatch.dialogIndex);
            ScriptConsole.println("[QuestHelper] Selected match: '" + bestMatch.optionText + "' at dialog index " + bestMatch.dialogIndex + 
                                 " (from section: " + bestMatch.sectionName + ", sequence: " + bestMatch.sequenceIndex + ")");
        } else {
            clearCurrentRecommendation();
            ScriptConsole.println("[QuestHelper] No matching dialog options found in current dialog");
        }
    }
    
    private static class MatchCandidate {
        final String optionText;
        final int dialogIndex;
        final String sectionName;
        final int sequenceIndex;
        final int optionIndex;
        
        MatchCandidate(String optionText, int dialogIndex, String sectionName, int sequenceIndex, int optionIndex) {
            this.optionText = optionText;
            this.dialogIndex = dialogIndex;
            this.sectionName = sectionName;
            this.sequenceIndex = sequenceIndex;
            this.optionIndex = optionIndex;
        }
    }
    
    /**
     * Finds the index of a fetched option text within the current dialog options.
     * Uses strict matching strategies for accurate comparison.
     * @param currentOptions The current dialog options array
     * @param fetchedOptionText The fetched option text to match
     * @return The index of the matching option, or -1 if no match found
     */
    private int findMatchingOptionIndex(List<String> currentOptions, String fetchedOptionText) {
        if (currentOptions == null || fetchedOptionText == null) {
            return -1;
        }
        
        String fetchedLower = fetchedOptionText.toLowerCase().trim();
        
        for (int i = 0; i < currentOptions.size(); i++) {
            String currentOption = currentOptions.get(i);
            if (currentOption == null) continue;
            
            String currentLower = currentOption.toLowerCase().trim();
            
            if (currentLower.equals(fetchedLower)) {
                ScriptConsole.println("[QuestHelper]       ✓ EXACT MATCH: '" + currentOption + "' == '" + fetchedOptionText + "'");
                return i;
            }
            
            double similarity = calculateStringSimilarity(currentLower, fetchedLower);
            if (similarity >= 0.9) {
                ScriptConsole.println("[QuestHelper]       ✓ HIGH SIMILARITY MATCH (" + String.format("%.1f", similarity * 100) + "%): '" + currentOption + "' ~= '" + fetchedOptionText + "'");
                return i;
            }
            
            if (currentLower.contains(fetchedLower)) {
                double containmentRatio = (double) fetchedLower.length() / currentLower.length();
                if (containmentRatio >= 0.7) {
                    ScriptConsole.println("[QuestHelper]       ✓ CONTAINMENT MATCH (current contains fetched, " + String.format("%.1f", containmentRatio * 100) + "%): '" + currentOption + "' contains '" + fetchedOptionText + "'");
                    return i;
                }
            }
            
            if (fetchedLower.contains(currentLower)) {
                double containmentRatio = (double) currentLower.length() / fetchedLower.length();
                if (containmentRatio >= 0.7) {
                    ScriptConsole.println("[QuestHelper]       ✓ CONTAINMENT MATCH (fetched contains current, " + String.format("%.1f", containmentRatio * 100) + "%): '" + fetchedOptionText + "' contains '" + currentOption + "'");
                    return i;
                }
            }
            
            ScriptConsole.println("[QuestHelper]       ✗ NO MATCH: '" + currentOption + "' vs '" + fetchedOptionText + "' (similarity: " + String.format("%.1f", similarity * 100) + "%)");
        }
        
        return -1;
    }
    
    /**
     * Calculates string similarity using Levenshtein distance.
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity ratio (0.0 to 1.0, where 1.0 is identical)
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int editDistance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLength;
    }
    
    /**
     * Calculates the Levenshtein distance between two strings.
     * @param s1 First string
     * @param s2 Second string
     * @return The edit distance
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Sets the current dialog recommendation.
     * @param optionText The text of the recommended option
     * @param optionIndex The index of the recommended option (0-based)
     */
    private void setCurrentRecommendation(String optionText, int optionIndex) {
        if (!optionText.equals(currentRecommendedOption) || optionIndex != currentRecommendedOptionIndex) {
            currentRecommendedOption = optionText;
            currentRecommendedOptionIndex = optionIndex;
            ScriptConsole.println("[QuestHelper] Recommended dialog option: " + (optionIndex + 1) + " - " + optionText);
        }
    }
    
    /**
     * Clears the current dialog recommendation.
     */
    private void clearCurrentRecommendation() {
        if (currentRecommendedOption != null) {
            currentRecommendedOption = null;
            currentRecommendedOptionIndex = -1;
        }
    }
    
    /**
     * Enables dialog assistance for the selected quest.
     */
    public void enableDialogAssistance() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] No quest selected. Cannot enable dialog assistance.");
            return;
        }
        
        isDialogAssistanceActive = true;
        dialogsFetched = false;
        ScriptConsole.println("[QuestHelper] Dialog assistance enabled for: " + selectedQuest.name());
    }
    
    /**
     * Disables dialog assistance.
     */
    public void disableDialogAssistance() {
        isDialogAssistanceActive = false;
        clearCurrentRecommendation();
        ScriptConsole.println("[QuestHelper] Dialog assistance disabled.");
    }
    
    /**
     * Checks if dialog assistance is currently active.
     * @return true if dialog assistance is active, false otherwise
     */
    public boolean isDialogAssistanceActive() {
        return isDialogAssistanceActive;
    }
    
    /**
     * Gets the current recommended dialog option text.
     * @return The recommended option text, or null if no recommendation
     */
    public String getCurrentRecommendedOption() {
        return currentRecommendedOption;
    }
    
    /**
     * Gets the current recommended dialog option index.
     * @return The recommended option index (0-based), or -1 if no recommendation
     */
    public int getCurrentRecommendedOptionIndex() {
        return currentRecommendedOptionIndex;
    }
    
    /**
     * Checks if dialogs have been fetched for the current quest.
     * @return true if dialogs are fetched, false otherwise
     */
    public boolean areDialogsFetched() {
        return dialogsFetched;
    }
    
    /**
     * Gets the fetched dialogs for the current quest.
     * @return Map of dialog sequences, or empty map if none fetched
     */
    public Map<String, List<QuestDialogFetcher.DialogSequence>> getFetchedDialogs() {
        return fetchedDialogs != null ? new HashMap<>(fetchedDialogs) : new HashMap<>();
    }
    
    /**
     * Updates the quest display list based on current filter settings.
     */
    public void updateQuestDisplayList() {
        questDisplayNames.clear();
        
        Set<String> uniqueQuestNames = new HashSet<>();
        
        if (showCompletedQuests) {
            uniqueQuestNames.addAll(completedQuests.stream()
                .filter(this::matchesQuestFilters)
                .map(q -> q.name() + " [" + getQuestId(q) + "]")
                .collect(Collectors.toSet()));
        }
        
        if (showInProgressQuests) {
            uniqueQuestNames.addAll(inProgressQuests.stream()
                .filter(this::matchesQuestFilters)
                .map(q -> q.name() + " [" + getQuestId(q) + "]")
                .collect(Collectors.toSet()));
        }
        
        if (showNotStartedQuests) {
            uniqueQuestNames.addAll(notStartedQuests.stream()
                .filter(this::matchesQuestFilters)
                .map(q -> q.name() + " [" + getQuestId(q) + "]")
                .collect(Collectors.toSet()));
        }
        
        questDisplayNames.addAll(uniqueQuestNames);
        Collections.sort(questDisplayNames, (a, b) -> {
            int idA = extractQuestId(a);
            int idB = extractQuestId(b);
            return Integer.compare(idA, idB);
        });
    }
    
    /**
     * Checks if a quest matches the current filter criteria.
     * @param quest The quest to check
     * @return true if the quest matches the filters
     */
    private boolean matchesQuestFilters(Quest quest) {
        boolean matchesMembershipFilter = false;
        if (quest.isMembers() && showMembersQuests) {
            matchesMembershipFilter = true;
        } else if (!quest.isMembers() && showFreeToPlayQuests) {
            matchesMembershipFilter = true;
        }
        
        if (!matchesMembershipFilter) {
            return false;
        }
        
        if (questSearchText != null && !questSearchText.trim().isEmpty()) {
            String searchLower = questSearchText.toLowerCase().trim();
            String questNameLower = quest.name().toLowerCase();
            return questNameLower.contains(searchLower);
        }
        
        return true;
    }
    
    /**
     * Extracts the quest ID from a display name with format "Quest Name [ID]".
     * @param displayName The display name
     * @return The quest ID, or -1 if not found
     */
    private int extractQuestId(String displayName) {
        try {
            int startIndex = displayName.lastIndexOf('[');
            int endIndex = displayName.lastIndexOf(']');
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String idStr = displayName.substring(startIndex + 1, endIndex);
                return Integer.parseInt(idStr);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            ScriptConsole.println("[QuestHelper] Error parsing quest ID from: " + displayName);
        }
        return -1;
    }
    
    /**
     * Selects a quest by its index in the display list.
     * @param index The index in the display list
     */
    public void selectQuestByIndex(int index) {
        if (index >= 0 && index < questDisplayNames.size()) {
            selectedQuestIndex = index;
            String selectedDisplayName = questDisplayNames.get(index);
            int questId = extractQuestId(selectedDisplayName);
            
            if (questId != -1) {
                Quest quest = getQuestById(questId);
                if (quest != null) {
                    setSelectedQuest(quest);
                    enableDialogAssistance();
                    ScriptConsole.println("[QuestHelper] Quest selected: " + quest.name() + " - Dialog assistance enabled");
                }
            }
        }
    }
    
    public boolean isShowCompletedQuests() {
        return showCompletedQuests;
    }
    
    public void setShowCompletedQuests(boolean showCompletedQuests) {
        if (this.showCompletedQuests != showCompletedQuests) {
            this.showCompletedQuests = showCompletedQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowInProgressQuests() {
        return showInProgressQuests;
    }
    
    public void setShowInProgressQuests(boolean showInProgressQuests) {
        if (this.showInProgressQuests != showInProgressQuests) {
            this.showInProgressQuests = showInProgressQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowNotStartedQuests() {
        return showNotStartedQuests;
    }
    
    public void setShowNotStartedQuests(boolean showNotStartedQuests) {
        if (this.showNotStartedQuests != showNotStartedQuests) {
            this.showNotStartedQuests = showNotStartedQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowFreeToPlayQuests() {
        return showFreeToPlayQuests;
    }
    
    public void setShowFreeToPlayQuests(boolean showFreeToPlayQuests) {
        if (this.showFreeToPlayQuests != showFreeToPlayQuests) {
            this.showFreeToPlayQuests = showFreeToPlayQuests;
            updateQuestDisplayList();
        }
    }
    
    public boolean isShowMembersQuests() {
        return showMembersQuests;
    }
    
    public void setShowMembersQuests(boolean showMembersQuests) {
        if (this.showMembersQuests != showMembersQuests) {
            this.showMembersQuests = showMembersQuests;
            updateQuestDisplayList();
        }
    }
    
    public String getQuestSearchText() {
        return questSearchText;
    }
    
    public void setQuestSearchText(String questSearchText) {
        if (!Objects.equals(this.questSearchText, questSearchText)) {
            this.questSearchText = questSearchText;
            updateQuestDisplayList();
        }
    }
    
    public int getSelectedQuestIndex() {
        return selectedQuestIndex;
    }
    
    /**
     * Initializes the quest display list. Should be called when the GUI is first opened.
     */
    public void initializeQuestDisplay() {
        if (questDisplayNames.isEmpty()) {
            updateQuestDisplayList();
        }
    }

    /**
     * Get the current dialog text for display in the overlay
     */
    public String getCurrentDialogText() {
        return currentDialogText;
    }
    
    /**
     * Starts navigation to the quest start location for the currently selected quest.
     * Converts the packed location coordinates and creates a circular area for navigation.
     */
    public void startNavigationToQuestStart() {
        if (selectedQuest == null) {
            ScriptConsole.println("[QuestHelper] No quest selected for navigation.");
            return;
        }
        
        if (selectedQuestType == null) {
            ScriptConsole.println("[QuestHelper] No QuestType data available for navigation.");
            return;
        }
        
        int[] startLocations = selectedQuestType.startLocations();
        if (startLocations == null || startLocations.length == 0) {
            ScriptConsole.println("[QuestHelper] No start locations found for quest: " + selectedQuest.name());
            return;
        }
        
        int packedLocation = startLocations[0];
        questStartCoordinate = convertPackedLocationToCoordinate(packedLocation);
        
        if (questStartCoordinate == null) {
            ScriptConsole.println("[QuestHelper] Failed to convert start location for quest: " + selectedQuest.name());
            return;
        }
        
        questStartArea = new Area.Circular(questStartCoordinate, 5);
        isNavigatingToQuestStart = true;
        
        ScriptConsole.println("[QuestHelper] Starting navigation to quest start location: " + questStartCoordinate + 
                             " for quest: " + selectedQuest.name());
    }
    
    /**
     * Stops navigation to quest start location.
     */
    public void stopNavigationToQuestStart() {
        isNavigatingToQuestStart = false;
        questStartArea = null;
        questStartCoordinate = null;
        ScriptConsole.println("[QuestHelper] Navigation to quest start stopped.");
    }
    
    /**
     * Checks if currently navigating to quest start location.
     * @return true if navigating, false otherwise
     */
    public boolean isNavigatingToQuestStart() {
        return isNavigatingToQuestStart;
    }
    
    /**
     * Gets the quest start coordinate.
     * @return The quest start coordinate, or null if not set
     */
    public Coordinate getQuestStartCoordinate() {
        return questStartCoordinate;
    }
    
    /**
     * Processes navigation to quest start location.
     * Should be called from the main execute loop when navigation is active.
     */
    private void processQuestStartNavigation() {
        if (!isNavigatingToQuestStart || questStartArea == null) {
            return;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return;
        }
        
        if (questStartArea.contains(player.getCoordinate())) {
            ScriptConsole.println("[QuestHelper] Arrived at quest start location for: " + selectedQuest.name());
            stopNavigationToQuestStart();
            return;
        }
        
        try {
            Coordinate targetCoordinate = questStartArea.getRandomWalkableCoordinate();
            ScriptConsole.println("[QuestHelper] Target coordinate: " + targetCoordinate);
            if (targetCoordinate != null) {
                NavPath path = NavPath.resolve(targetCoordinate);
                if (path != null) {
                    Movement.traverse(path);
                    ScriptConsole.println("[QuestHelper] Navigating to quest start location...");
                } else {
                    ScriptConsole.println("[QuestHelper] Failed to resolve path to quest start location.");
                    stopNavigationToQuestStart();
                }
            } else {
                ScriptConsole.println("[QuestHelper] Failed to get walkable coordinate in quest start area.");
                stopNavigationToQuestStart();
            }
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error during navigation: " + e.getMessage());
            stopNavigationToQuestStart();
        }
    }
    
    /**
     * Converts a packed location value to a Coordinate object.
     * @param packedLocation The packed location value
     * @return A Coordinate object, or null if conversion fails
     */
    private Coordinate convertPackedLocationToCoordinate(int packedLocation) {
        try {
            int x = (packedLocation >> 14) & 0x3fff;
            int y = packedLocation & 0x3fff;
            int z = packedLocation >> 28;
            return new Coordinate(x, y, z);
        } catch (Exception e) {
            ScriptConsole.println("[QuestHelper] Error converting packed location " + packedLocation + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Debug method to verify quest loading logic for duplicate names.
     * This method can be called to check if quests with duplicate names are being handled correctly.
     */
    public void debugQuestDuplicates() {
        ScriptConsole.println("[QuestHelper] === QUEST DUPLICATE DEBUG ===");
        
        Map<String, List<Integer>> questNameToIds = new HashMap<>();
        
        for (int i = 0; i < 509; i++) {
            Optional<Quest> questOpt = Quest.byId(i);
            if (questOpt.isPresent()) {
                Quest quest = questOpt.get();
                if (quest.name() != null && !quest.name().trim().isEmpty()) {
                    questNameToIds.computeIfAbsent(quest.name(), k -> new ArrayList<>()).add(i);
                }
            }
        }
        
        for (Map.Entry<String, List<Integer>> entry : questNameToIds.entrySet()) {
            if (entry.getValue().size() > 1) {
                String questName = entry.getKey();
                List<Integer> questIds = entry.getValue();
                
                ScriptConsole.println("[QuestHelper] Duplicate quest found: " + questName);
                
                for (Integer questId : questIds) {
                    Optional<Quest> questOpt = Quest.byId(questId);
                    if (questOpt.isPresent()) {
                        Quest quest = questOpt.get();
                        QuestType questType = ConfigManager.getQuestType(questId);
                        int richness = calculateQuestDataRichness(quest, questType, questId);
                        
                        boolean isInOurList = allQuests.stream().anyMatch(q -> q.name().equals(questName) && getQuestId(q) == questId);
                        
                        ScriptConsole.println("[QuestHelper]   ID " + questId + ": richness=" + richness + 
                                             ", inOurList=" + isInOurList + 
                                             ", hasQuestType=" + (questType != null) +
                                             ", questPoints=" + (questType != null ? questType.questPoints() : "N/A"));
                    }
                }
                
                Quest selectedForName = getQuestByName(questName);
                if (selectedForName != null) {
                    int selectedId = getQuestId(selectedForName);
                    ScriptConsole.println("[QuestHelper]   -> Selected ID: " + selectedId + " for " + questName);
                }
                
                ScriptConsole.println("[QuestHelper]   ---");
            }
        }
        
        ScriptConsole.println("[QuestHelper] === END QUEST DUPLICATE DEBUG ===");
    }
} 