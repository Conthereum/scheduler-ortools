package emvScheduling.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionSettings {
    private Integer numberOfWorkers;
    private Integer maxSolverExecutionTimeInSeconds;
    private Integer randomSeed;

    public ExecutionSettings(Integer numberOfWorkers, Integer maxSolverExecutionTimeInSeconds, Integer randomSeed) {
        this.numberOfWorkers = numberOfWorkers;
        this.maxSolverExecutionTimeInSeconds = maxSolverExecutionTimeInSeconds;
        this.randomSeed = randomSeed;
    }
}
