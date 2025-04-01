package emvScheduling.application;

import emvScheduling.data.EmvDataGenerator;
import emvScheduling.domain.ExecutionSettings;
import emvScheduling.domain.ProblemFacts;
import emvScheduling.domain.SolverOutput;
import emvScheduling.solver.CpSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileBasedExecutor {
    protected final static transient Logger logger = LoggerFactory.getLogger(FileBasedExecutor.class);

    private static final String inputFile = "input.csv";
    private static final String outputFile = "output.csv";
    private static final String accumulativeOutputFile = "output-accumulative.csv";
    private static final String outputHeader = "no, groupNo, randomSeed, numberOfWorkers," +
            "maxSolverExecutionTimeInSeconds, processCount, processExecutionTimeMin, processExecutionTimeMax," +
            "computerCount,conflictPercentage, timeWeight, SolverWallTime,makespan,parallelTimeSum," +
            "serialTimeHorizon,solverStatus,currentTimestamp";

    public static void executeUsingFiles() {
        CpSolver solver = new CpSolver();

        // Use the updated method to read inputs from "input.csv"
        List<List<Integer>> inputs = readInputsFromCSV(inputFile);
        String outputFilePath = "src/main/resources/" + outputFile;

        List<SolverOutput> outputs = new ArrayList<>();

        String outputLines = "";
        logger.info("Output:\n\n" + "---------------------------\n" + outputHeader);
        Path outputPath = Paths.get(outputFilePath);
        try {
            // Delete the file if it exists, then create a new one
            if (Files.exists(outputPath)) {
                Files.delete(outputPath);
            }
            Files.createFile(outputPath);
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
                writer.println(outputHeader);
                for (List<Integer> input : inputs) {
                    int i = 2;
                    int randomSeed = input.get(i++);
                    int numberOfWorkers = input.get(i++);
                    int maxSolverExecutionTimeInSeconds = input.get(i++);
                    ExecutionSettings settings = new ExecutionSettings(numberOfWorkers, maxSolverExecutionTimeInSeconds, randomSeed);

                    ProblemFacts facts = EmvDataGenerator.getBenchmark(randomSeed, input.get(i++), input.get(i++),
                            input.get(i++), input.get(i++), input.get(i++), input.get(i++));
                    SolverOutput output = solver.solve(facts, settings);
                    outputs.add(output);

                    String outputLine = getOutputLine(input.get(0), // No.
                            input.get(1),// Group id
                            randomSeed,
                            numberOfWorkers,
                            maxSolverExecutionTimeInSeconds,
                            facts.getProcesses().size(), // processCount
                            input.get(6), // processExecutionTimeMin
                            input.get(7), // processExecutionTimeMax
                            facts.getComputers().size(), // computerCount
                            input.get(9), // conflictPercentage
                            input.get(10), // timeWeight
                            output.getSolverWallTime(),
                            output.getMakespan(),
                            output.getSolverWallTime() + output.getMakespan(), // parallel time (sum)
                            output.getHorizon(),
                            output.getSolverStatus()); // serial time (horizon));
                    writer.println(outputLine);
                    writer.flush();
                    writeInAccumulativeOutFileWithTimestamp(outputLine);
                    logger.info(outputLine);
                }
            }
            logger.info("---------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getOutputLine(Integer no, Integer groupId, Integer randomSeed, Integer numberOfWorkers,
                                       Integer maxSolverExecutionTimeInSeconds,
                                       Integer processCount, Integer processExecutionTimeMin,
                                       Integer processExecutionTimeMax, Integer computerCount,
                                       Integer conflictPercentage, Integer timeWeight, Double solverWallTime,
                                       Double makespan, Double parallelTimeSum, Integer serialTimeHorizon,
                                       String solverStatus) {
        String line = String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%f,%f,%f,%d,%s",
                no, groupId, randomSeed, numberOfWorkers, maxSolverExecutionTimeInSeconds, processCount,
                processExecutionTimeMin,
                processExecutionTimeMax, computerCount, conflictPercentage, timeWeight, solverWallTime,
                makespan, parallelTimeSum, serialTimeHorizon, solverStatus
        );
        return line;
    }

    private static List<List<Integer>> readInputsFromCSV(String fileName) {
        List<List<Integer>> inputs = new ArrayList<>();
        try (InputStream inputStream = FileBasedExecutor.class.getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                logger.error("File not found: " + fileName);
                return inputs;
            }

            // Skip header
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<Integer> inputList = new ArrayList<>();
                String[] parts = line.trim().split(",");
                for (String part : parts) {
                    inputList.add(Integer.parseInt(part));
                }
                inputs.add(inputList);
            }
        } catch (IOException e) {
            logger.error("Error reading input file", e);
        }
        return inputs;
    }

    /*public static void writeInAccumulativeOutFile(String outputLines) throws IOException {
        String accumulativeOutputFilePath = "src/main/resources/" + accumulativeOutputFile;
        Path accumOutputPath = Paths.get(accumulativeOutputFilePath);
        boolean accumCreated = false;
        if (!Files.exists(accumOutputPath)) {
            Files.createFile(accumOutputPath);
            accumCreated = true;
        }
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
                accumOutputPath,
                StandardOpenOption.CREATE, // Creates the file if it doesn't exist
                StandardOpenOption.APPEND  // Appends to the file if it exists
        ))) {
            if (accumCreated) {
                writer.println(outputHeader); // Only write the header if needed
            }
            writer.append(outputLines);
            writer.flush();
        }
    }*/

    public static void writeInAccumulativeOutFileWithTimestamp(String outputLines) throws IOException {
        String accumulativeOutputFilePath = "src/main/resources/" + accumulativeOutputFile;
        Path accumOutputPath = Paths.get(accumulativeOutputFilePath);
        boolean accumCreated = false;

        if (!Files.exists(accumOutputPath)) {
            Files.createFile(accumOutputPath);
            accumCreated = true;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                accumOutputPath,
                StandardOpenOption.CREATE, // Creates the file if it doesn't exist
                StandardOpenOption.APPEND  // Appends to the file if it exists
        )) {
            if (accumCreated) {
                writer.write(outputHeader); // Only write the header if needed
                writer.newLine();
            }
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


            writer.write(outputLines+","+now.format(formatter));
            writer.newLine();
        }
    }
}
