package emvScheduling.application;

import emvScheduling.data.EmvDataGenerator;
import emvScheduling.domain.ExecutionSettings;
import emvScheduling.domain.ProblemFacts;
import emvScheduling.domain.SolverOutput;
import emvScheduling.solver.CpSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ArgsBasedExecutor {
    protected final static transient Logger logger = LoggerFactory.getLogger(ArgsBasedExecutor.class);

    public static void executeUsingArgs(int randomSeed, int numberOfWorkers, int maxSolverExecutionTimeInSeconds
            , int processCount, int processExecutionTimeMin, int processExecutionTimeMax, int computerCount, int conflictPercentage, int timeWeight) throws IOException {
        CpSolver solver = new CpSolver();
        ProblemFacts facts = EmvDataGenerator.getBenchmark(randomSeed, processCount, processExecutionTimeMin,
                processExecutionTimeMax, computerCount, conflictPercentage, timeWeight);

        ExecutionSettings settings = new ExecutionSettings(numberOfWorkers, maxSolverExecutionTimeInSeconds, randomSeed);

        // Solve the problem
        SolverOutput solverOutput = solver.solve(facts, settings);
        String line = FileBasedExecutor.getOutputLine(0, 0, randomSeed, numberOfWorkers,
                maxSolverExecutionTimeInSeconds,
                processCount, processExecutionTimeMin, processExecutionTimeMax, computerCount, conflictPercentage,
                timeWeight, solverOutput.getSolverWallTime(), solverOutput.getMakespan(),
                solverOutput.getSolverWallTime() + solverOutput.getMakespan(),
                solverOutput.getHorizon(), solverOutput.getSolverStatus());
        FileBasedExecutor.writeInAccumulativeOutFileWithTimestamp(line);
        logger.info("Result: " + line);
    }
}
