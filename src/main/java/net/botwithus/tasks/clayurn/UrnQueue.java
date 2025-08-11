package net.botwithus.tasks.clayurn;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the queue of urns to be crafted
 */
public class UrnQueue {
    private final Map<UrnType, Integer> queue = new LinkedHashMap<>();
    
    /**
     * Add urns to the crafting queue
     */
    public void addUrn(UrnType urnType, int quantity) {
        if (urnType == null || quantity <= 0) {
            return;
        }
        queue.put(urnType, queue.getOrDefault(urnType, 0) + quantity);
    }
    
    /**
     * Remove a specific urn type from the queue
     */
    public void removeUrn(UrnType urnType) {
        queue.remove(urnType);
    }
    
    /**
     * Called when an urn is crafted - decrements the queue count
     */
    public void onUrnCrafted(UrnType urnType) {
        if (queue.containsKey(urnType)) {
            int remaining = queue.get(urnType) - 1;
            if (remaining <= 0) {
                queue.remove(urnType);
            } else {
                queue.put(urnType, remaining);
            }
        }
    }
    
    /**
     * Get the next urn type to craft (first in queue)
     */
    public UrnType getNextUrn() {
        return queue.isEmpty() ? null : queue.keySet().iterator().next();
    }
    
    /**
     * Check if the queue is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Get the current queue as a read-only map
     */
    public Map<UrnType, Integer> getQueue() {
        return new LinkedHashMap<>(queue);
    }
    
    /**
     * Clear the entire queue
     */
    public void clear() {
        queue.clear();
    }
    
    /**
     * Get the quantity of a specific urn type in the queue
     */
    public int getQuantity(UrnType urnType) {
        return queue.getOrDefault(urnType, 0);
    }
}