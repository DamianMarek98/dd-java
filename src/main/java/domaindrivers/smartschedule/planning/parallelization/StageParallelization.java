package domaindrivers.smartschedule.planning.parallelization;

import java.util.Set;
import java.util.stream.Collectors;


public class StageParallelization {

    public ParallelStagesList of(Set<Stage> stages) {
        return findParallelTasks(stages, ParallelStagesList.empty());
    }

    private static ParallelStagesList findParallelTasks(Set<Stage> stages, ParallelStagesList parallelStagesList) {
        var withoutDependency = stages.stream().filter(stage -> !stage.hasAnyDependency()).collect(Collectors.toSet());
        if (withoutDependency.isEmpty()) {
            return parallelStagesList;
        }
        parallelStagesList = parallelStagesList.add(new ParallelStages(withoutDependency));

        var stagesWithDependencies = stages.stream()
                .filter(Stage::hasAnyDependency)
                .collect(Collectors.toSet());
        if (stagesWithDependencies.isEmpty() || stagesWithDependencies.size() == stages.size()) {
            return parallelStagesList;
        }

        stagesWithDependencies = stagesWithDependencies.stream()
                        .map(stage -> {
                            var dependencies = stage.dependencies()
                                    .stream().filter(dependency -> !withoutDependency.contains(dependency))
                                    .collect(Collectors.toSet());
                            return stage.withDependencies(dependencies);
                        }).collect(Collectors.toSet());
        return findParallelTasks(stagesWithDependencies, parallelStagesList);
    }


}
