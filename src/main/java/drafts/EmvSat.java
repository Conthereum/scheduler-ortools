package drafts;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Math.max;

/** Minimal Jobshop problem adjusted for EMV scheduling problem.
 *
 * No order is in tasks of a job
 * */
public class EmvSat {
    public static void main(String[] args) {
        Loader.loadNativeLibraries();
        class Task {
            int machine;
            int duration;
            Task(int machine, int duration) {
                this.machine = machine;
                this.duration = duration;
            }
        }

        //tasks inside a job does nt have order
        //tasks inside a job ca not execute at the same time
        //one machine can not execute two tasks at the same time
        /*final List<List<Task>> allJobs =
                Arrays.asList(Arrays.asList(new Task(0, 30), new Task(1, 20), new Task(2, 20)), // Job0
                        Arrays.asList(new Task(0, 2), new Task(2, 1), new Task(1, 4)), // Job1
                        Arrays.asList(new Task(1, 4), new Task(2, 3)) // Job2
                );*/
        final List<List<Task>> allJobs = Arrays.asList(
                // Job0: Tasks on different machines but task 2 (duration 7) should ideally come before task 1 (duration 10) for minimal makespan.
                Arrays.asList(new Task(0, 10), new Task(1, 7), new Task(2, 3)), // Job0

                // Job1: Reordering matters. If task 1 (duration 5) runs first, it will block another machine.
                Arrays.asList(new Task(1, 5), new Task(0, 4), new Task(2, 6)), // Job1

                // Job2: Reordering helps avoid conflicts on machine 0 if task 2 (duration 8) is scheduled before task 1 (duration 12).
                Arrays.asList(new Task(0, 12), new Task(2, 8), new Task(1, 2))  // Job2
        );


        int numMachines = 1;
        for (List<Task> job : allJobs) {
            for (Task task : job) {
                numMachines = max(numMachines, 1 + task.machine);
            }
        }
        final int[] allMachines = IntStream.range(0, numMachines).toArray();

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
        }
        Map<List<Integer>, TaskType> allTasks = new HashMap<>(); // [jobId,taskId] -> taskType
        Map<Integer, List<IntervalVar>> machineToIntervals = new HashMap<>();//machine ->  List<IntervalVar>

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

                List<Integer> key = Arrays.asList(jobID, taskID);
                allTasks.put(key, taskType);
                machineToIntervals.computeIfAbsent(task.machine, (Integer k) -> new ArrayList<>());
                machineToIntervals.get(task.machine).add(taskType.interval);
            }
        }

        // Create and add disjunctive constraints.
        for (int machine : allMachines) {
            List<IntervalVar> list = machineToIntervals.get(machine);
            model.addNoOverlap(list);
        }

        //we don't have any predefined order for conflicting transactions so needs to be changed in our case
        // Precedences inside a job:
       /*for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);
            for (int taskID = 0; taskID < job.size() - 1; ++taskID) {
                List<Integer> prevKey = Arrays.asList(jobID, taskID);
                List<Integer> nextKey = Arrays.asList(jobID, taskID + 1);
                model.addGreaterOrEqual(allTasks.get(nextKey).start, allTasks.get(prevKey).end);
            }
        }*/
//        Enforce the Constraint That Tasks in the Same Job Cannot Overlap
        for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);
            List<IntervalVar> jobIntervals = new ArrayList<>();

            for (int taskID = 0; taskID < job.size(); ++taskID) {
                List<Integer> key = Arrays.asList(jobID, taskID);
                jobIntervals.add(allTasks.get(key).interval);
            }

            // Add no overlap constraint for the job
            model.addNoOverlap(jobIntervals);
        }




        // Makespan objective.
        IntVar objVar = model.newIntVar(0, horizon, "makespan");
        List<IntVar> ends = new ArrayList<>();
        for (int jobID = 0; jobID < allJobs.size(); ++jobID) {
            List<Task> job = allJobs.get(jobID);

            //because I have removed the task order inside one job, I added this loop:
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                List<Integer> key = Arrays.asList(jobID, taskID); //job.size() - 1);
                ends.add(allTasks.get(key).end);
            }
        }
        model.addMaxEquality(objVar, ends);
        model.minimize(objVar);

        // Creates a solver and solves the model.
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            //print the solution:
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
                            jobID, taskID, (int) solver.value(allTasks.get(key).start), task.duration);
                    assignedJobs.computeIfAbsent(task.machine, (Integer k) -> new ArrayList<>());
                    assignedJobs.get(task.machine).add(assignedTask);
                }
            }

            // Create per machine output lines.
            String output = "";
            for (int machine : allMachines) {
                // Sort by starting time.
                Collections.sort(assignedJobs.get(machine), new SortTasks());
                String solLineTasks = "Machine " + machine + ": ";
                String solLine = "           ";

                for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                    String name = "job_" + assignedTask.jobID + "_task_" + assignedTask.taskID;
                    // Add spaces to output to align columns.
                    solLineTasks += String.format("%-15s", name);

                    String solTmp =
                            "[" + assignedTask.start + "," + (assignedTask.start + assignedTask.duration) + "]";
                    // Add spaces to output to align columns.
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

    private EmvSat() {}
}