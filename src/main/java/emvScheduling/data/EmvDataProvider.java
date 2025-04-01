package emvScheduling.data;

import emvScheduling.domain.Computer;
import emvScheduling.domain.ProblemFacts;
import emvScheduling.domain.Process;
import emvScheduling.domain.UnorderedPair;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will be replaced with providing data from Json files.
 */
public class EmvDataProvider {
    public static ProblemFacts getProblemSpecifications() {
        ProblemFacts problemFacts = new ProblemFacts();

        Computer c1 = new Computer(0, 1000, 1);
        Computer c2 = new Computer(1, 1, 1);
        Computer c3 = new Computer(2, 1000, 1);
        List<Computer> computers = new ArrayList<>(List.of(c1, c2, c3));
        problemFacts.setComputers(computers);

        Process p0 = new Process(0, 10, 100);
        Process p1 = new Process(1, 7, 70);
        Process p2 = new Process(2, 3, 30);
        Process p3 = new Process(3, 5, 50);
        Process p4 = new Process(4, 4, 40);
        Process p5 = new Process(5, 6, 60);
        Process p6 = new Process(6, 12, 120);
        Process p7 = new Process(7, 8, 80);
        Process p8 = new Process(8, 2, 20);
        List<Process> processes = new ArrayList<>(List.of(p0, p1, p2, p3, p4, p5, p6, p7, p8));
        problemFacts.setProcesses(processes);

        UnorderedPair<Integer> r0 = new UnorderedPair<>(p0.getId(), p1.getId());
        UnorderedPair<Integer> r1 = new UnorderedPair<>(p1.getId(), p2.getId());
        UnorderedPair<Integer> r2 = new UnorderedPair<>(p0.getId(), p2.getId());
        UnorderedPair<Integer> r3 = new UnorderedPair<>(p3.getId(), p4.getId());
        UnorderedPair<Integer> r4 = new UnorderedPair<>(p4.getId(), p5.getId());
        UnorderedPair<Integer> r5 = new UnorderedPair<>(p3.getId(), p5.getId());
        UnorderedPair<Integer> r6 = new UnorderedPair<>(p6.getId(), p7.getId());
        UnorderedPair<Integer> r7 = new UnorderedPair<>(p7.getId(), p8.getId());
        UnorderedPair<Integer> r8 = new UnorderedPair<>(p6.getId(), p8.getId());
        List<UnorderedPair<Integer>> conflictingProcesses = new ArrayList<>(List.of(r0, r1, r2, r3, r4, r5, r6, r7, r8));
        problemFacts.setConflictingProcesses(conflictingProcesses);

        problemFacts.setTimeWeight(100);

        return problemFacts;
    }
}
