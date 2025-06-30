package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.ScriptConsole;

import java.util.Arrays;
import java.util.List;

public class PenguinTrackingTask implements Task {
    private final CoaezUtility script;
    private int currentLocationIndex = 0;
    private boolean isNavigating = false;
    private long lastLocationCheck = 0;
    private static final long LOCATION_CHECK_DELAY = 5000;
    
    private final List<PenguinLocation> penguinLocations = Arrays.asList(

        new PenguinLocation("Lumbridge Castle", new Coordinate(3222, 3218, 0)),
        new PenguinLocation("Lumbridge Swamp", new Coordinate(3203, 3169, 0)),
        new PenguinLocation("Draynor Village", new Coordinate(3093, 3244, 0)),
        new PenguinLocation("Draynor Manor", new Coordinate(3109, 3353, 0)),
        
        // Varrock area
        new PenguinLocation("Varrock Square", new Coordinate(3214, 3428, 0)),
        new PenguinLocation("Varrock East Bank", new Coordinate(3253, 3420, 0)),
        new PenguinLocation("Varrock West Bank", new Coordinate(3185, 3436, 0)),
        new PenguinLocation("Grand Exchange", new Coordinate(3164, 3487, 0)),
        
        // Falador area
        new PenguinLocation("Falador Park", new Coordinate(2997, 3378, 0)),
        new PenguinLocation("Falador East Bank", new Coordinate(3013, 3355, 0)),
        new PenguinLocation("Falador West Bank", new Coordinate(2946, 3368, 0)),
        new PenguinLocation("White Knights Castle", new Coordinate(2963, 3340, 0)),
        
        // Ardougne area
        new PenguinLocation("Ardougne North Bank", new Coordinate(2615, 3332, 0)),
        new PenguinLocation("Ardougne South Bank", new Coordinate(2655, 3283, 0)),
        new PenguinLocation("Ardougne Market", new Coordinate(2662, 3307, 0)),
        new PenguinLocation("Ardougne Zoo", new Coordinate(2613, 3266, 0)),
        
        // Catherby area
        new PenguinLocation("Catherby Bank", new Coordinate(2808, 3441, 0)),
        new PenguinLocation("Catherby Fishing", new Coordinate(2837, 3432, 0)),
        
        // Seers Village area
        new PenguinLocation("Seers Village Bank", new Coordinate(2726, 3493, 0)),
        new PenguinLocation("Seers Village Flax", new Coordinate(2738, 3444, 0)),
        
        // Camelot area
        new PenguinLocation("Camelot Castle", new Coordinate(2757, 3507, 0)),
        new PenguinLocation("Camelot Teleport", new Coordinate(2757, 3477, 0)),
        
        // Yanille area
        new PenguinLocation("Yanille Bank", new Coordinate(2612, 3093, 0)),
        new PenguinLocation("Yanille Magic Guild", new Coordinate(2590, 3086, 0)),
        
        // Port Sarim area
        new PenguinLocation("Port Sarim Docks", new Coordinate(3038, 3192, 0)),
        new PenguinLocation("Port Sarim Jail", new Coordinate(3010, 3178, 0)),
        
        // Rimmington area
        new PenguinLocation("Rimmington", new Coordinate(2957, 3214, 0)),
        new PenguinLocation("Rimmington Mine", new Coordinate(2970, 3240, 0)),
        
        // Burthorpe area
        new PenguinLocation("Burthorpe Bank", new Coordinate(2886, 3537, 0)),
        new PenguinLocation("Burthorpe Games Room", new Coordinate(2880, 3559, 0)),
        
        // Taverley area
        new PenguinLocation("Taverley Bank", new Coordinate(2878, 3417, 0)),
        new PenguinLocation("Taverley Dungeon", new Coordinate(2884, 3397, 0)),
        
        // Barbarian Village area
        new PenguinLocation("Barbarian Village", new Coordinate(3081, 3421, 0)),
        new PenguinLocation("Barbarian Outpost", new Coordinate(2552, 3560, 0)),
        
        // Al Kharid area
        new PenguinLocation("Al Kharid Bank", new Coordinate(3269, 3167, 0)),
        new PenguinLocation("Al Kharid Palace", new Coordinate(3293, 3174, 0)),
        
        // Edgeville area
        new PenguinLocation("Edgeville Bank", new Coordinate(3094, 3492, 0)),
        new PenguinLocation("Edgeville Monastery", new Coordinate(3053, 3488, 0)),
        
        // Wilderness locations (lower level)
        new PenguinLocation("Wilderness Volcano", new Coordinate(3143, 3635, 0)),
        new PenguinLocation("Wilderness Ruins", new Coordinate(3094, 3652, 0))
    );
    
    public PenguinTrackingTask(CoaezUtility script) {
        this.script = script;
    }
    
    @Override
    public void execute() {
        try {
            if (System.currentTimeMillis() - lastLocationCheck < LOCATION_CHECK_DELAY) {
                return;
            }
            
            if (!isNavigating) {
                if (checkForPenguinsAtCurrentLocation()) {
                    lastLocationCheck = System.currentTimeMillis();
                    return;
                }
            }
            
            if (!isNavigating) {
                navigateToNextLocation();
            } else {
                checkNavigationStatus();
            }
            
        } catch (Exception e) {
            ScriptConsole.println("Error in penguin tracking: " + e.getMessage());
            e.printStackTrace();
            isNavigating = false;
        }
    }
    
    private boolean checkForPenguinsAtCurrentLocation() {
        Npc penguin = NpcQuery.newQuery()
            .name("Penguin")
            .results()
            .nearestTo(Client.getLocalPlayer());
            
        if (penguin != null) {
            ScriptConsole.println("Found penguin at current location: " + penguin.getCoordinate());
            //todo spy logic            
            return true;
        }
        
        return false;
    }
    
    private void navigateToNextLocation() {
        if (currentLocationIndex >= penguinLocations.size()) {
            currentLocationIndex = 0;
        }
        
        PenguinLocation targetLocation = penguinLocations.get(currentLocationIndex);
        ScriptConsole.println("Navigating to: " + targetLocation.getName() + " at " + targetLocation.getCoordinate());
        
        NavPath path = NavPath.resolve(targetLocation.getCoordinate());
        if (path != null) {
            Movement.traverse(path);
            isNavigating = true;
        } else {
            ScriptConsole.println("Could not create path to: " + targetLocation.getName());
            currentLocationIndex++;
        }
    }
    
    private void checkNavigationStatus() {
        PenguinLocation currentTarget = penguinLocations.get(currentLocationIndex);
        Coordinate playerPos = Client.getLocalPlayer().getCoordinate();
        
        if (playerPos.distanceTo(currentTarget.getCoordinate()) < 10) {
            ScriptConsole.println("Arrived at: " + currentTarget.getName());
            isNavigating = false;
            lastLocationCheck = System.currentTimeMillis();
            currentLocationIndex++;
            Execution.delay(1000);
            checkForPenguinsAtCurrentLocation();
        } else {
            Execution.delay(2000);
        }
    }
    
    private static class PenguinLocation {
        private final String name;
        private final Coordinate coordinate;
        
        public PenguinLocation(String name, Coordinate coordinate) {
            this.name = name;
            this.coordinate = coordinate;
        }
        
        public String getName() {
            return name;
        }
        
        public Coordinate getCoordinate() {
            return coordinate;
        }
    }
} 