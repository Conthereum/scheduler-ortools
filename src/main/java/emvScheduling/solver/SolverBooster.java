package emvScheduling.solver;


import emvScheduling.domain.ProblemFacts;
import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.domain.*;
import it.unitn.emvscheduling.greedy.solver.DispatcherSolver;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * this class uses declarative implementation to boost the speed of CP solver
 */
public class SolverBooster {

    //parameters:
    private final static Strategy.ProcessSortType processSortType = Strategy.ProcessSortType.MCDF;
    private final static int looseReviewRound = 33;

    //todo make the method for bulk process
    public static ExecutionOutput getSuggestedOutput(ProblemFacts factsIn,
                                                     emvScheduling.domain.ExecutionSettings settingsIn) {
        //todo consider merge both projects avoiding redundant classes and conversions - if performance preserve

        //the values at time is not processed in deterministic approach:
        Integer numberOfWorkers = -1;
        Integer maxSolverExecutionTimeInSeconds = -1;

        DispatcherSolver solver = new DispatcherSolver();
        Strategy strategy = new Strategy(processSortType, looseReviewRound);
        ExecutionFacts facts = new ExecutionFacts();

        List<Computer> computers = new ArrayList<>(factsIn.getComputers().size());
        for(emvScheduling.domain.Computer c:factsIn.getComputers()){
            Computer computer = new Computer(c.getId());
            computers.add(computer);
        }

        List<Process> processes = new ArrayList<>(factsIn.getProcesses().size());
        for(emvScheduling.domain.Process p:factsIn.getProcesses()){
            Process process = new Process(p.getId(), p.getExecutionTime());
            processes.add(process);
        }

        Integer timeWeight = factsIn.getTimeWeight();
        Integer randomSeed = settingsIn.getRandomSeed();

        facts.computers = computers;
        facts.processes = processes;
        facts.timeWeight = timeWeight;

        ExecutionSettings settings = new ExecutionSettings(numberOfWorkers, maxSolverExecutionTimeInSeconds, randomSeed);
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        return output;
    }
}
