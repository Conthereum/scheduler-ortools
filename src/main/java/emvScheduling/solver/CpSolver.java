package emvScheduling.solver;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import emvScheduling.domain.Process;
import emvScheduling.domain.*;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class CpSolver {
    protected final static transient Logger logger = LoggerFactory.getLogger(CpSolver.class);
    boolean useBooster = true;
    boolean useHint = true;//booster ->hint

    /**
     * Method to add no_overlap constraint between p1 and p2 if equ is true in the model
     *
     * @param model
     * @param p1
     * @param p2
     * @param sameComputerVar
     */
    private void noOverlap(CpModel model, Process p1, Process p2, BoolVar sameComputerVar, ExecutionOutput declarativeOutput) {
        // sameComputerVar implies p2.start >= p1.end || p1.start >= p2.end

        BoolVar v1 = model.newBoolVar("ov_" + p1.getId() + "_" + p2.getId() + "_1");
        model.addLessOrEqual(p1.getEndTime(), p2.getStartTime()).onlyEnforceIf(v1);
        model.addGreaterThan(p1.getEndTime(), p2.getStartTime()).onlyEnforceIf(v1.not());

        BoolVar v2 = model.newBoolVar("ov_" + p1.getId() + "_" + p2.getId() + "_2");
        model.addLessOrEqual(p2.getEndTime(), p1.getStartTime()).onlyEnforceIf(v2);
        model.addGreaterThan(p2.getEndTime(), p1.getStartTime()).onlyEnforceIf(v2.not());

        model.addBoolOr(new BoolVar[]{v1, v2}).onlyEnforceIf(sameComputerVar);

        if(useHint){
            Boolean P1EndBeforeP2Start =
                    declarativeOutput.processes.get(p1.getId()).endTime <=  declarativeOutput.processes.get(p2.getId()).startTime;
            model.addHint(v1, P1EndBeforeP2Start?1:0);

            Boolean P2EndBeforeP1Start =
                    declarativeOutput.processes.get(p2.getId()).endTime <=  declarativeOutput.processes.get(p1.getId()).startTime;
            model.addHint(v2, P2EndBeforeP1Start?1:0);
        }
    }

    public SolverOutput solve(ProblemFacts facts, ExecutionSettings settings) {
        SolverOutput solverOutput = new SolverOutput();
        Loader.loadNativeLibraries();
        // Computes horizon dynamically as the sum of all durations.
        int horizon = 0;
        for (Process process : facts.getProcesses()) {
            horizon += process.getExecutionTime();
        }
        solverOutput.setHorizon(horizon);

        // Creates the model.
        CpModel model = new CpModel();

        // Creates a solver and solves the model.
        com.google.ortools.sat.CpSolver solver = new com.google.ortools.sat.CpSolver();

        //For speed up the process:
        solver.getParameters().setNumWorkers(settings.getNumberOfWorkers());//processors
        solver.getParameters().setCpModelPresolve(true);// false: make the "makespan" the way worst
        solver.getParameters().setEnumerateAllSolutions(false); // need to investigate more
        solver.getParameters().setBinaryMinimizationAlgorithm(SatParameters.BinaryMinizationAlgorithm.BINARY_MINIMIZATION_FIRST_WITH_TRANSITIVE_REDUCTION);
        solver.getParameters().setMaxTimeInSeconds(settings.getMaxSolverExecutionTimeInSeconds());
//        solver.getParameters().setUsePrecedencesInDisjunctiveConstraint(true); // deteriorated the performance
//        solver.getParameters().setSearchBranching(SatParameters.SearchBranching.PORTFOLIO_SEARCH);  // Made the
//        performance better when there was no conflict
        //solver.getParameters().setSymmetryLevel(2); // better performance in absence of it

        // Force the solver to follow the decision strategy exactly.
    //        solver.getParameters().setSearchBranching(SatParameters.SearchBranching.FIXED_SEARCH);//improves the
        // performance in absence of conflicts (using this line, all the processes starts from zero but in absence of
        // it they don't start from zero even though there would be no conflict) but deteriorate in presence of
        // conflicting processes
        solver.getParameters().setRandomizeSearch(true);// Enable randomization and improve the wall time for optimal solutions
        solver.getParameters().setRandomSeed(settings.getRandomSeed()); // Set random seed for reproducibility
        solver.getParameters().setUseLns(true);// Enable large neighborhood search: by using false value the wall time
        // of optimal answers are getting better and worst at times and the feasible answer for a given time is the
        // same for cases and needs further test to be decided.


        int upperBound;
        ExecutionOutput declarativeOutput = null;
        if(useBooster){
            declarativeOutput = SolverBooster.getSuggestedOutput(facts, settings);
            upperBound = declarativeOutput.scheduleMakespan;
        } else {
            upperBound = horizon;
        }

        //variable assignments:
        for (Process process : facts.getProcesses()) {
            process.setComputerId(model.newIntVar(0, facts.getComputers().size() - 1, "computer_" + process.getId()));
            process.setStartTime(model.newIntVar(0, upperBound, "start_" + process.getId()));
            process.setEndTime(model.newIntVar(0, upperBound, "end_" + process.getId()));
            process.setInterval(model.newIntervalVar(process.getStartTime(),
                    LinearExpr.constant(process.getExecutionTime()), process.getEndTime(), "interval_" + process.getId()));

            if(useHint){
                model.addHint(process.getComputerId(), declarativeOutput.processes.get(process.getId()).computer.computerId);
                model.addHint(process.getStartTime(), declarativeOutput.processes.get(process.getId()).startTime);
                model.addHint(process.getEndTime(), declarativeOutput.processes.get(process.getId()).endTime);
//                model.addHint(process.getInterval(), declarativeOutput.processes.get(process.getId()).executionTime);
                //todo: remove end time and interval in case of possibility from InVars
            }
        }

        // Constraints:
        // 1- Enforce the constraint that conflicting processes cannot overlap
        if (facts.getConflictingProcesses() != null) {
            for (UnorderedPair<Integer> conflictingProcesses : facts.getConflictingProcesses()) {
                List<IntervalVar> conflictingIntervals = new ArrayList<>();
                conflictingIntervals.add(facts.getProcess(conflictingProcesses.getI()).getInterval());
                conflictingIntervals.add(facts.getProcess(conflictingProcesses.getJ()).getInterval());
                model.addNoOverlap(conflictingIntervals);
            }
        }

        // 2- Enforce the constraint that processes on the same computer cannot overlap
        for (int i = 0; i < facts.getProcesses().size(); i++) {
            for (int j = i + 1; j < facts.getProcesses().size(); j++) {
                // For all unique pairs of processes
                // Create a boolean variable indicating whether process i and j are assigned to the same computer
                Process pi = facts.getProcesses().get(i);
                Process pj = facts.getProcesses().get(j);
                BoolVar sameComputer = model.newBoolVar("eq_comp_" + i + "_" + j);

                if (useHint) {
                    Boolean sameComp =
                            declarativeOutput.processes.get(i).computer.computerId == declarativeOutput.processes.get(j).computer.computerId;
                    model.addHint(sameComputer, sameComp ? 1 : 0);
                }

                //eq_com_i_j == (pi.computer =pj.computer)
                model.addEquality(pi.getComputerId(), pj.getComputerId()).onlyEnforceIf(sameComputer);
                model.addDifferent(pi.getComputerId(), pj.getComputerId()).onlyEnforceIf(sameComputer.not());

                // Enforce the no_overlap constraint if both processes are on the same computer
                noOverlap(model, pi, pj, sameComputer, declarativeOutput);

                //onlyEnforceIf is not working on addNoOverlap
                /*List<IntervalVar> intervalPairs = new ArrayList(List.of(pi.getInterval(), pj.getInterval()));
                model.addNoOverlap(intervalPairs).onlyEnforceIf(sameComputer);*/
            }
        }
        //Objective: Weighted sum of Makespan and cost
        IntVar maximumProcessEndTime = model.newIntVar(0, upperBound, "makespan");
        List<IntVar> ends = new ArrayList<>();
        for (Process process : facts.getProcesses()) {
            ends.add(process.getEndTime());
        }
        model.addMaxEquality(maximumProcessEndTime, ends);
        if(useHint){
            model.addHint(maximumProcessEndTime, declarativeOutput.scheduleMakespan);
        }

//        -----------------

        /*IntVar totalCost = model.newIntVar(0, Integer.MAX_VALUE, "total_cost");// todo: Sum of all costs
        allIntVars.add(totalCost);
        for (Computer computer : facts.getComputers()) {
            // Collect all processes assigned to this computer
            for (Process process : facts.getProcesses()) {
                BoolVar isAssignedToThisComputer = model.newBoolVar("assigned_" + process.getId() + "_to_computer_" + computer.getId());
                allBoolVars.add(isAssignedToThisComputer);
                model.addEquality(process.getComputerId(), computer.getId()).onlyEnforceIf(isAssignedToThisComputer);
                model.addDifferent(process.getComputerId(), computer.getId()).onlyEnforceIf(isAssignedToThisComputer.not());

                // Calculate the process cost and add to total costs
                Integer cost = process.getOperationCount()*computer.getCostPerOperation();
//                totalCostVar1 = costConst + totalCostVar; if(x)
                //todo intermediate variables
                model.addEquality(LinearExpr.sum(
                        new LinearArgument[]{totalCost, LinearExpr.constant(cost)}), totalCost)
                        .onlyEnforceIf(isAssignedToThisComputer);
            }
            // Sort processes by their start time to compute idle times
            *//*processesOnComputer.sort(Comparator.comparingInt(process -> solver.value(process.getStartTime())));

            // Calculate idle time costs between consecutive processes
            for (int i = 0; i < processesOnComputer.size() - 1; i++) {
                Process prevProcess = processesOnComputer.get(i);
                Process nextProcess = processesOnComputer.get(i + 1);
                IntVar idleTime = model.newIntVar(0, Integer.MAX_VALUE, "idle_time_" + prevProcess.getId() + "_" + nextProcess.getId());
                model.addGreaterThan(idleTime, nextProcess.getStartTime().sub(prevProcess.getEndTime()));

                IntVar idleTimeCost = idleTime.mul(computer.getCostPerIdleTime());
                totalCosts.add(idleTimeCost);
            }*//*
        }

        IntVar weightedObjective = model.newIntVar(0, Integer.MAX_VALUE, "weighted_objective");
            allIntVars.add(weightedObjective);
            model.addEquality(LinearExpr.weightedSum(
                    new IntVar[]{maximumProcessEndTime, totalCost},
                    new long[]{facts.getTimeWeight(), facts.getCostWeight()}
            ), weightedObjective);
            model.minimize(weightedObjective);

        */

        if (facts.getTimeWeight() == 100) {
            model.minimize(maximumProcessEndTime);
        } else {
            logger.error("todo");
        }

        CpSolverStatus status = solver.solve(model);
        solverOutput.setSolverStatus(status.toString());
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            // Print the solution:


            /**
             * representative class as the output of the whole process
             */

            @Getter
            class AssignedProcess {
                Integer computerId;
                Integer processId;
                Integer start;
                Integer duration;

                public AssignedProcess(Integer computerId, Integer processId, Integer start, Integer duration) {
                    this.computerId = computerId;
                    this.processId = processId;
                    this.start = start;
                    this.duration = duration;
                }
            }

            /**
             * compare in this order: computerId, start, duration
             */
            class SortProcess implements Comparator<AssignedProcess> {
                @Override
                public int compare(AssignedProcess a, AssignedProcess b) {
                    return Comparator
                            .comparingLong(AssignedProcess::getComputerId)
                            .thenComparingLong(AssignedProcess::getStart)
                            .thenComparingLong(AssignedProcess::getDuration)
                            .compare(a, b);
                }
            }

            logger.trace("Solution:");
            // Create one list of assigned processes per computer.
            Map<Integer, List<AssignedProcess>> computerToProcesses = new HashMap<>();
            for (Process process : facts.getProcesses()) {
                AssignedProcess assignedProcess = new AssignedProcess(((Long) solver.value(process.getComputerId())).intValue(),
                        process.getId(), ((Long) solver.value(process.getStartTime())).intValue(), process.getExecutionTime());
                computerToProcesses.computeIfAbsent(assignedProcess.getComputerId(), (k) -> new ArrayList<>()).add(assignedProcess);
            }

            // Create per computer output lines.
            String output = "";
            for (int computer = 0; computer < facts.getComputers().size(); computer++) {
                String solLineProcesses = "Computer " + computer + ": ";
                String solLine = "           ";
                if (computerToProcesses.get(computer) != null) {
                    Collections.sort(computerToProcesses.get(computer), new SortProcess());
                    for (AssignedProcess assignedProcess : computerToProcesses.get(computer)) {
                        String name = "process-" + assignedProcess.getProcessId();
                        solLineProcesses += String.format("%-15s", name);

                        String solTmp =
                                "[" + assignedProcess.start + "," + (assignedProcess.start + assignedProcess.duration) + "]";
                        solLine += String.format("%-15s", solTmp);
                    }
                }
                output += solLineProcesses + System.lineSeparator();
                output += solLine + System.lineSeparator();
            }
            logger.trace("Optimal objective value: {} out of {}", solver.objectiveValue(), horizon);
            logger.trace("   Time: {}, timeWeight: {}", solver.value(maximumProcessEndTime), facts.getTimeWeight());
            logger.trace("   Cost: {}, costWeight: {}", "-", facts.getCostWeight());
            logger.trace(System.lineSeparator() + output);
        } else if (status == CpSolverStatus.INFEASIBLE) {
            logger.trace("Infeasible");
            logger.trace("sufficientAssumptionsForInfeasibility: " + solver.sufficientAssumptionsForInfeasibility());
        } else {
            logger.trace("No solution found, status = " + status);
        }
        // Statistics.
        logger.trace("Statistics");
        logger.trace("  conflicts: {}", solver.numConflicts());
        logger.trace("  branches : {}", solver.numBranches());
        logger.trace("  wall time: {} s", solver.wallTime());


        // variable values:
        logger.trace("Solver Status:" + solver.responseStats());
        logger.trace("Model Validate:" + model.validate());

        //Solution:
        logger.trace("Solver SolutionInfo:" + solver.getSolutionInfo());
//        logger.trace("Solver Response:" + solver.response());
//        logger.trace("Solver getParameters:" + solver.getParameters());

        solverOutput.setSolverWallTime(solver.wallTime());
        if (facts.getTimeWeight() == 100) {
            solverOutput.setMakespan(solver.objectiveValue());
        } else {
            throw new RuntimeException("not supported yet");
        }
        return solverOutput;
    }
}
