package domaindrivers.smartschedule.simulation;

import java.util.*;


public class SimulationFacade {

    public Result whichProjectWithMissingDemandsIsMostProfitableToAllocateResourcesTo(List<SimulatedProject> projects, SimulatedCapabilities totalCapability) {
        // Initialize memoization map
        Map<MemoKey, Result> memo = new HashMap<>();
        return knapSack(projects, totalCapability.capabilities(), 0, new HashMap<>(), memo);

    }

    private static Result knapSack(List<SimulatedProject> projects, List<AvailableResourceCapability> capabilities, int currentIndex, Map<SimulatedProject, Set<AvailableResourceCapability>> allocatedResources, Map<MemoKey, Result> memo) {
        // Base case: No more projects to consider
        if (currentIndex >= projects.size()) {
            double totalProfit = allocatedResources.keySet().stream()
                    .mapToDouble(p -> p.earnings().doubleValue())
                    .sum();
            return new Result(totalProfit, new ArrayList<>(allocatedResources.keySet()), new HashMap<>(allocatedResources));
        }

        // Create a memoization key
        MemoKey memoKey = new MemoKey(currentIndex, new HashSet<>(capabilities), new HashMap<>(allocatedResources));

        // Check memoization map
        if (memo.containsKey(memoKey)) {
            return memo.get(memoKey);
        }

        SimulatedProject currentProject = projects.get(currentIndex);

        // Option 1: Do not include the current project
        Result withoutCurrentProject = knapSack(projects, capabilities, currentIndex + 1, allocatedResources, memo);

        // Option 2: Try to include the current project if we have the necessary capabilities
        Result withCurrentProject = new Result(0.0, new ArrayList<>(), new HashMap<>());
        if (canAllocateResources(currentProject, capabilities)) {
            List<AvailableResourceCapability> remainingCapabilities = new ArrayList<>(capabilities);
            Map<SimulatedProject, Set<AvailableResourceCapability>> newAllocatedResources = new HashMap<>(allocatedResources);
            Set<AvailableResourceCapability> allocated = allocateResources(currentProject, remainingCapabilities);
            newAllocatedResources.put(currentProject, allocated);
            withCurrentProject = knapSack(projects, remainingCapabilities, currentIndex + 1, newAllocatedResources, memo);
        }

        // Compare results
        Result bestResult = withoutCurrentProject.profit() > withCurrentProject.profit() ? withoutCurrentProject : withCurrentProject;
        memo.put(memoKey, bestResult);

        return bestResult;
    }

    private static boolean canAllocateResources(SimulatedProject project, List<AvailableResourceCapability> capabilities) {
        for (Demand demand : project.missingDemands().all()) {
            boolean satisfied = capabilities.stream().anyMatch(resource ->
                    resource.capability().equals(demand.capability()) &&
                            resource.timeSlot().getDuration().compareTo(demand.slot().getDuration()) >= 0
            );
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    private static Set<AvailableResourceCapability> allocateResources(SimulatedProject project, List<AvailableResourceCapability> capabilities) {
        Set<AvailableResourceCapability> allocated = new HashSet<>();
        for (Demand demand : project.missingDemands().all()) {
            Iterator<AvailableResourceCapability> iterator = capabilities.iterator();
            while (iterator.hasNext()) {
                AvailableResourceCapability resource = iterator.next();
                if (resource.capability().equals(demand.capability()) &&
                        resource.timeSlot().getDuration().compareTo(demand.slot().getDuration()) >= 0) {
                    allocated.add(resource);
                    iterator.remove();  // Remove the resource from the remaining capabilities
                    break;
                }
            }
        }
        return allocated;
    }


    // Helper class to use as memoization key
    private static class MemoKey {
        int currentIndex;
        Set<AvailableResourceCapability> capabilities;
        Map<SimulatedProject, Set<AvailableResourceCapability>> allocatedResources;

        MemoKey(int currentIndex, Set<AvailableResourceCapability> capabilities, Map<SimulatedProject, Set<AvailableResourceCapability>> allocatedResources) {
            this.currentIndex = currentIndex;
            this.capabilities = capabilities;
            this.allocatedResources = allocatedResources;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MemoKey memoKey = (MemoKey) o;
            return currentIndex == memoKey.currentIndex && capabilities.equals(memoKey.capabilities) && allocatedResources.equals(memoKey.allocatedResources);
        }

        @Override
        public int hashCode() {
            return Objects.hash(currentIndex, capabilities, allocatedResources);
        }
    }
}

