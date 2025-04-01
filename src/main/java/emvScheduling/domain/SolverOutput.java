package emvScheduling.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolverOutput {
    private Double solverWallTime;
    private Double makespan;
    private Integer horizon;//parallel execution time
    private String solverStatus;

    @Override
    public String toString() {
        return "SolverOutput{" +
                "solverWallTime=" + solverWallTime +
                ", makespan=" + makespan +
                ", horizon=" + horizon +
                ", solverStatus=" + solverStatus +
                '}';
    }
}
