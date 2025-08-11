package net.botwithus.tasks;

import net.botwithus.CoaezUtility;
import net.botwithus.tasks.clayurn.UrnCategory;
import net.botwithus.tasks.clayurn.UrnType;

import java.util.Map;

/**
 * @deprecated Use ClayUrnTaskRefactored instead. This class is kept for backward compatibility.
 * The refactored version provides better OOP structure with proper separation of concerns.
 * 
 * This wrapper delegates all calls to the refactored implementation to maintain API compatibility.
 */
@Deprecated
public class ClayUrnTask implements Task {
    private final ClayUrnTaskRefactored refactoredTask;

    public ClayUrnTask(CoaezUtility script) {
        this.refactoredTask = new ClayUrnTaskRefactored(script);
    }

    @Override
    public void execute() {
        refactoredTask.execute();
    }

    // State transition methods - delegate to refactored version
    public void setStateToMineClay() { 
        refactoredTask.setStateToMineClay(); 
    }
    
    public void setStateToGoUpstairs() { 
        refactoredTask.setStateToGoUpstairs(); 
    }
    
    public void setStateToSoftenClay() { 
        refactoredTask.setStateToSoftenClay(); 
    }
    
    public void setStateToSpinUrns() { 
        refactoredTask.setStateToSpinUrns(); 
    }
    
    public void setStateToFireUrns() { 
        refactoredTask.setStateToFireUrns(); 
    }
    
    public void setStateToAddRunes() { 
        refactoredTask.setStateToAddRunes(); 
    }
    
    public void setStateToDepositUrns() { 
        refactoredTask.setStateToDepositUrns(); 
    }

    // Configuration methods - delegate to refactored version
    public UrnCategory getSelectedCategory() {
        return UrnCategory.wrap(refactoredTask.getSelectedCategory());
    }
    
    public void setSelectedCategory(UrnCategory category) {
        refactoredTask.setSelectedCategory(category != null ? category.getDelegate() : null);
    }
    
    public UrnType getSelectedUrn() {
        return UrnType.wrap(refactoredTask.getSelectedUrn());
    }
    
    public void setSelectedUrn(UrnType urn) {
        refactoredTask.setSelectedUrn(urn != null ? urn.getDelegate() : null);
    }
    
    public boolean isSkipAddRunes() {
        return refactoredTask.isSkipAddRunes();
    }
    
    public void setSkipAddRunes(boolean skipAddRunes) {
        refactoredTask.setSkipAddRunes(skipAddRunes);
    }

    // Queue management methods - delegate to refactored version
    public void queueUrn(UrnType urnType, int quantity) {
        refactoredTask.queueUrn(urnType != null ? urnType.getDelegate() : null, quantity);
    }
    
    public void removeUrnFromQueue(UrnType urnType) {
        refactoredTask.removeUrnFromQueue(urnType != null ? urnType.getDelegate() : null);
    }
    
    public void onUrnCrafted(UrnType urnType) {
        refactoredTask.onUrnCrafted(urnType != null ? urnType.getDelegate() : null);
    }
    
    public Map<UrnType, Integer> getUrnQueue() {
        Map<net.botwithus.tasks.clayurn.UrnType, Integer> originalQueue = refactoredTask.getUrnQueue().getQueue();
        Map<UrnType, Integer> wrappedQueue = new java.util.LinkedHashMap<>();
        for (Map.Entry<net.botwithus.tasks.clayurn.UrnType, Integer> entry : originalQueue.entrySet()) {
            wrappedQueue.put(UrnType.wrap(entry.getKey()), entry.getValue());
        }
        return wrappedQueue;
    }
    
    public void clearUrnQueue() {
        refactoredTask.clearUrnQueue();
    }

    // Data access methods - delegate to refactored version
    public UrnCategory[] getAvailableCategories() {
        net.botwithus.tasks.clayurn.UrnCategory[] original = refactoredTask.getAvailableCategories();
        UrnCategory[] wrapped = new UrnCategory[original.length];
        for (int i = 0; i < original.length; i++) {
            wrapped[i] = UrnCategory.wrap(original[i]);
        }
        return wrapped;
    }
    
    public UrnType[] getUrnsInCategory(UrnCategory category) {
        net.botwithus.tasks.clayurn.UrnType[] original = refactoredTask.getUrnsInCategory(
            category != null ? category.getDelegate() : null);
        UrnType[] wrapped = new UrnType[original.length];
        for (int i = 0; i < original.length; i++) {
            wrapped[i] = UrnType.wrap(original[i]);
        }
        return wrapped;
    }
    
    public UrnType[] getAllAvailableUrns() {
        net.botwithus.tasks.clayurn.UrnType[] original = refactoredTask.getAllAvailableUrns();
        UrnType[] wrapped = new UrnType[original.length];
        for (int i = 0; i < original.length; i++) {
            wrapped[i] = UrnType.wrap(original[i]);
        }
        return wrapped;
    }
    
    public boolean isDataLoaded() {
        return refactoredTask.getUrnDataManager().isDataLoaded();
    }
    
    public void reloadUrnData() {
        refactoredTask.reloadUrnData();
    }
    
    public String getStatus() {
        return refactoredTask.getStatus();
    }
    
    public boolean setSelectedUrnById(int urnId) {
        return refactoredTask.setSelectedUrnById(urnId);
    }
    
    public void printAvailableUrns() {
        refactoredTask.printAvailableUrns();
    }

    // Legacy inner classes for backward compatibility
    @Deprecated
    public static class UrnCategory {
        private final net.botwithus.tasks.clayurn.UrnCategory delegate;
        
        public UrnCategory(int index, String displayName, int enumId) {
            this.delegate = new net.botwithus.tasks.clayurn.UrnCategory(index, displayName, enumId);
        }
        
        private UrnCategory(net.botwithus.tasks.clayurn.UrnCategory delegate) {
            this.delegate = delegate;
        }
        
        public int getIndex() { return delegate.getIndex(); }
        public String getDisplayName() { return delegate.getDisplayName(); }
        public int getEnumId() { return delegate.getEnumId(); }
        
        @Override
        public String toString() { return delegate.toString(); }
        
        public net.botwithus.tasks.clayurn.UrnCategory getDelegate() { return delegate; }
        
        public static UrnCategory wrap(net.botwithus.tasks.clayurn.UrnCategory category) {
            return category == null ? null : new UrnCategory(category);
        }
    }
    
    @Deprecated
    public static class UrnType {
        private final net.botwithus.tasks.clayurn.UrnType delegate;
        
        public UrnType(int id, String displayName, UrnCategory category) {
            this.delegate = new net.botwithus.tasks.clayurn.UrnType(id, displayName, category.getDelegate());
        }
        
        private UrnType(net.botwithus.tasks.clayurn.UrnType delegate) {
            this.delegate = delegate;
        }
        
        public int getId() { return delegate.getId(); }
        public String getDisplayName() { return delegate.getDisplayName(); }
        public UrnCategory getCategory() { return UrnCategory.wrap(delegate.getCategory()); }
        
        @Override
        public String toString() { return delegate.toString(); }
        
        public net.botwithus.tasks.clayurn.UrnType getDelegate() { return delegate; }
        
        public static UrnType wrap(net.botwithus.tasks.clayurn.UrnType urnType) {
            return urnType == null ? null : new UrnType(urnType);
        }
        
        @Deprecated
        public UrnType getById(int id, java.util.List<UrnType> urns) {
            for (UrnType urn : urns) {
                if (urn.getId() == id) {
                    return urn;
                }
            }
            return null;
        }
    }
}