package emvScheduling.data;

import emvScheduling.domain.Computer;
import emvScheduling.domain.ProblemFacts;
import emvScheduling.domain.Process;
import emvScheduling.domain.UnorderedPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EmvDataGenerator {
    protected final static transient Logger logger = LoggerFactory.getLogger(EmvDataGenerator.class);

    /**
     * execution time of th processes is a random number in [processExecutionTimeMin, processTimeExecutionMax]
     *
     * @param processCount
     * @param processExecutionTimeMin
     * @param processExecutionTimeMax
     * @param computerCount
     * @param conflictPercentage
     * @param timeWeight
     * @return
     */
    public static ProblemFacts getBenchmark(Integer randomSeed, Integer processCount, Integer processExecutionTimeMin,
                                            Integer processExecutionTimeMax,
                                            Integer computerCount, Integer conflictPercentage, Integer timeWeight) {
        Random random = new Random(randomSeed);
        if(processExecutionTimeMin>processExecutionTimeMax)
            throw new RuntimeException("processExecutionTimeMin must be less than or equal to processTimeExecutionMax");

        logger.trace("start generating data(processCount=" + processCount + ", computerCount=" + computerCount +
                ", conflictPercentage=" + conflictPercentage + ", timeWeight=" + timeWeight + ")");
        ProblemFacts problemFacts = new ProblemFacts();

        List<Computer> computers = new ArrayList<>();
        problemFacts.setComputers(computers);
        for (int computerIdx = 0; computerIdx < computerCount; computerIdx++) {
            Computer computer = new Computer(computerIdx, 1, 1);
            computers.add(computer);
        }

        List<Process> processes = new ArrayList<>();
        problemFacts.setProcesses(processes);

        String processDurations = "";
        for (int processIdx = 0; processIdx < processCount; processIdx++) {

            Integer executionTime =
                    random.nextInt(processExecutionTimeMax - processExecutionTimeMin + 1) + processExecutionTimeMin;
            Process process = new Process(processIdx, executionTime, 1);
            processes.add(process);
            processDurations += " " + executionTime;
        }
        logger.trace("processDurations: " + processDurations);

        List<UnorderedPair<Integer>> conflicts = generateConflictPairs(randomSeed, processCount, conflictPercentage);
        problemFacts.setConflictingProcesses(conflicts);

        problemFacts.setTimeWeight(timeWeight);

        String conflictsStr = "";
        for (UnorderedPair<Integer>pair: conflicts){
            conflictsStr += "("+pair.getI()+","+pair.getJ()+"),";
        }
        logger.trace("finished generating data, conflicts are:\n"+conflictsStr);
        return problemFacts;
    }

    public static List<UnorderedPair<Integer>> generateConflictPairs(Integer randomSeed, Integer processCount,
                                                                     Integer conflictPercentage) {
        Random random = new Random(randomSeed);
        List<UnorderedPair<Integer>> conflictPairs = new ArrayList<>();
        Set<String> uniquePairs = new HashSet<>();
        int totalPairs = processCount * (processCount - 1) / 2; // Total unique pairs
        int requiredConflicts = totalPairs * conflictPercentage / 100; // Number of conflicts based on percentage

        while (uniquePairs.size() < requiredConflicts) {
            int processA = random.nextInt(processCount);
            int processB = random.nextInt(processCount);

            // Ensure processA and processB are different and order pair consistently to avoid duplicates
            if (processA != processB) {
                int minProcess = Math.min(processA, processB);
                int maxProcess = Math.max(processA, processB);
                String pairKey = minProcess + "-" + maxProcess;

                if (!uniquePairs.contains(pairKey)) {
                    uniquePairs.add(pairKey);
                    conflictPairs.add(new UnorderedPair<>(minProcess, maxProcess));
                }
            }
        }
        Collections.sort(conflictPairs);
        return conflictPairs;
    }
}
