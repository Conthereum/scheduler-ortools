package emvScheduling.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProblemFacts {
    //facts
    private List<Computer> computers;
    private List<Process> processes;
    private List<UnorderedPair<Integer>> conflictingProcesses;
    private Integer timeWeight;//out of 100

    //dependant variable
    @Setter(AccessLevel.NONE)
    private Integer costWeight;
//    private Integer score;

    public void setTimeWeight(Integer timeWeight) {
        this.timeWeight = timeWeight;
        this.costWeight = 100 - timeWeight;
    }

    /**
     * Note: relies on incremental id from 0
     *
     * @param processId
     * @return
     */
    public Process getProcess(Integer processId) {
        return processes.get(processId);
    }

    @Override
    public String toString() {
        return "EmvBalance{" +
                "computerList=" + computers +
                ", processList=" + processes +
                ", timeWeight=" + timeWeight +
                ", costWeight=" + costWeight +
                ", conflictList=" + conflictingProcesses +
                '}';
    }
}
