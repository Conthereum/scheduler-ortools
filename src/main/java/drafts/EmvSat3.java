package drafts;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.ConstraintSolverParameters;
import com.google.ortools.sat.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class EmvSat3 {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
//        System.setProperty("CP_MODEL_LOG_SEARCH_PROGRESS", "true"); // did not work for logging
        Loader.loadNativeLibraries();
        class Task {
            int duration;

            Task(int duration) {
                this.duration = duration;
            }
        }

        // Sample job data with tasks. Machines will be assigned dynamically.
        final List<List<Task>> allJobs = Arrays.asList(
                Arrays.asList(new Task(10), new Task(7), new Task(3)), // Job0
                Arrays.asList(new Task(5), new Task(4), new Task(6)), // Job1
                Arrays.asList(new Task(12), new Task(8), new Task(2))  // Job2
        );

        int numMachines = 3;  // Set a fixed number of machines to assign from.

        // Computes horizon dynamically as the sum of all durations.
        int horizon = 0;
        for (List<Task> job : allJobs) {
            for (Task task : job) {
                horizon += task.duration;
            }
        }

        // Creates the model.
        CpModel model = new CpModel();

        class TaskType {
            IntVar start;
            IntVar end;
            IntervalVar interval;
            IntVar machine;  // Add machine assignment variable
        }
        Map<List<Integer>, TaskType> allTasksMap = new HashMap<>(); // [jobId, taskId] -> TaskType

        for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                Task task = job.get(taskID);
                String suffix = "_" + jobID + "_" + taskID;

                TaskType taskType = new TaskType();
                taskType.start = model.newIntVar(0, horizon, "start" + suffix);
                taskType.end = model.newIntVar(0, horizon, "end" + suffix);
                taskType.interval = model.newIntervalVar(
                        taskType.start, LinearExpr.constant(task.duration), taskType.end, "interval" + suffix);

                // Machine assignment variable
                taskType.machine = model.newIntVar(0, numMachines - 1, "machine" + suffix);

                List<Integer> key = Arrays.asList(jobID, taskID);
                allTasksMap.put(key, taskType);
            }
        }


        // Enforce the constraint that tasks in the same job cannot overlap
        for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);
            List<IntervalVar> jobIntervals = new ArrayList<>();

            for (int taskID = 0; taskID < job.size(); ++taskID) {
                List<Integer> key = Arrays.asList(jobID, taskID);
                jobIntervals.add(allTasksMap.get(key).interval);
            }

            // Add no overlap constraint for the job
            model.addNoOverlap(jobIntervals);
        }

//        ---------

        List<TaskType> allJobIntervals = new ArrayList<>();
        List<IntVar> allIntVars = new ArrayList<>();
        for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                List<Integer> key = Arrays.asList(jobID, taskID);
                allJobIntervals.add(allTasksMap.get(key));
                allIntVars.add(allTasksMap.get(key).start);
                allIntVars.add(allTasksMap.get(key).machine);
            }

        }

        List<List<Integer>> allTaskKeys = allTasksMap.keySet().stream().toList();


        for (int i = 0; i < allTaskKeys.size(); i++) {
            for (int j = i + 1; j < allTaskKeys.size(); j++) {
//                suffix = jobId_taskId
//                key:{jobId, taskId}
                String suffix_i = "_" + allTaskKeys.get(i).get(0) + "_" + allTaskKeys.get(i).get(1);
                String suffix_j = "_" + allTaskKeys.get(j).get(0) + "_" + allTaskKeys.get(j).get(1);

                // Create a boolean variable indicating whether jobs i and j are assigned to the same machine
                BoolVar sameMachine = model.newBoolVar("m-" + suffix_i + "," + suffix_j);

                TaskType ti = allTasksMap.get(allTaskKeys.get(i));
                TaskType tj = allTasksMap.get(allTaskKeys.get(j));

                // Add constraints to check if both jobs are assigned to the same machine
//                sameMachine == (ti.machine=? tj.machine)
                model.addEquality(ti.machine, tj.machine).onlyEnforceIf(sameMachine);

//                model.addDifferent(ti.machine, tj.machine).onlyEnforceIf(sameMachine.not());
                model.addGreaterThan(ti.machine, tj.machine).onlyEnforceIf(sameMachine.not());
                model.addLessThan(ti.machine, tj.machine).onlyEnforceIf(sameMachine.not());

                // Add a no-overlap constraint if both jobs are on the same machine
                model.addNoOverlap(Arrays.asList(ti.interval, tj.interval))
                        .onlyEnforceIf(sameMachine);
            }
        }


        // Makespan objective.
        IntVar objVar = model.newIntVar(0, horizon, "makespan");
        List<IntVar> ends = new ArrayList<>();
        for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);

            // Because task order inside one job is not important:
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                List<Integer> key = Arrays.asList(jobID, taskID);
                ends.add(allTasksMap.get(key).end);
            }
        }
        model.addMaxEquality(objVar, ends);
        model.minimize(objVar);

//         Search for x values in increasing order.
        model.addDecisionStrategy(allIntVars,
                DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
                DecisionStrategyProto.DomainReductionStrategy.SELECT_MIN_VALUE);

        // Creates a solver and solves the model.
        CpSolver solver = new CpSolver();


       /* // Set parameters for verbose logging
        ConstraintSolverParameters params = ConstraintSolverParameters.newBuilder().build();
        params.setLogSearchProgress(true); // Enable detailed logging
        // Solve with parameters
        CpSolverStatus status = solver.solve(model, params);*/



        // Force the solver to follow the decision strategy exactly.
        solver.getParameters().setSearchBranching(SatParameters.SearchBranching.FIXED_SEARCH);
        // Tell the solver to enumerate all solutions.
        solver.getParameters().setEnumerateAllSolutions(true);

        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            // Print the solution:
            class AssignedTask {
                int jobID;
                int taskID;
                int start;
                int duration;

                // Ctor
                AssignedTask(int jobID, int taskID, int start, int duration) {
                    this.jobID = jobID;
                    this.taskID = taskID;
                    this.start = start;
                    this.duration = duration;
                }
            }

            class SortTasks implements Comparator<AssignedTask> {
                @Override
                public int compare(AssignedTask a, AssignedTask b) {
                    if (a.start != b.start) {
                        return a.start - b.start;
                    } else {
                        return a.duration - b.duration;
                    }
                }
            }

            System.out.println("Solution:");
            // Create one list of assigned tasks per machine.
            Map<Integer, List<AssignedTask>> assignedJobs = new HashMap<>();
            for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
                List<Task> job = allJobs.get(jobID);
                for (int taskID = 0; taskID < job.size(); ++taskID) {
                    Task task = job.get(taskID);
                    List<Integer> key = Arrays.asList(jobID, taskID);
                    AssignedTask assignedTask = new AssignedTask(
                            jobID, taskID, (int) solver.value(allTasksMap.get(key).start), task.duration);
                    assignedJobs.computeIfAbsent(taskID, (k) -> new ArrayList<>()).add(assignedTask);
                }
            }

            // Create per machine output lines.
            String output = "";
            for (int machine = 0; machine < numMachines; machine++) {
                // Sort by starting time.
                Collections.sort(assignedJobs.get(machine), new SortTasks());
                String solLineTasks = "Machine " + machine + ": ";
                String solLine = "           ";

                for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                    String name = "job_" + assignedTask.jobID + "_task_" + assignedTask.taskID;
                    solLineTasks += String.format("%-15s", name);

                    String solTmp =
                            "[" + assignedTask.start + "," + (assignedTask.start + assignedTask.duration) + "]";
                    solLine += String.format("%-15s", solTmp);
                }
                output += solLineTasks + "%n";
                output += solLine + "%n";
            }
            System.out.printf("Optimal Schedule Length: %f%n", solver.objectiveValue());
            System.out.printf(output);
        } else {
            System.out.println("No solution found.");
        }

        // Statistics.
        System.out.println("Statistics");
        System.out.printf("  conflicts: %d%n", solver.numConflicts());
        System.out.printf("  branches : %d%n", solver.numBranches());
        System.out.printf("  wall time: %f s%n", solver.wallTime());
    }
}
