package net.botwithus.tasks;

import net.botwithus.CoaezUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple concrete implementation of Portable for types that don't have
 * specific complex logic like PortableWorkbench.
 */
public class SimplePortable extends Portable {

    public SimplePortable(CoaezUtility script, PortableType type) {
        super(script, type);
    }

    @Override
    public String getInteractionOption() {
        // Use the portable's name as the default interaction option
        // This might need adjustment based on specific portable interactions
        // e.g., "Use", "Craft-at", etc.
        return this.type.getName(); 
    }

    @Override
    public List<Ingredient> getRequiredItems() {
        // Return an empty list as this simple version doesn't define specific requirements.
        // Actual implementations would load/define ingredients here.
        return Collections.emptyList(); 
    }

    // Inherits hasRequiredItems(), interact(), confirmAction(), etc. from Portable
    // Note: hasRequiredItems will likely return false due to empty list, unless overridden
    // or the base class logic is changed.
    // interact() will use the name returned by getInteractionOption().
} 