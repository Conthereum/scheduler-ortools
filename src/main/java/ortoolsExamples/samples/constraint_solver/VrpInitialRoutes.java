// Copyright 2010-2024 Google LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// [START program]
package ortoolsExamples.samples.constraint_solver;
// [START import]
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.RoutingDimension;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import java.util.logging.Logger;
// [END import]

/** Minimal VRP. */
public class VrpInitialRoutes {
  private static final Logger logger = Logger.getLogger(VrpInitialRoutes.class.getName());

  // [START data_model]
  static class DataModel {
    public final long[][] distanceMatrix = {
        {0, 548, 776, 696, 582, 274, 502, 194, 308, 194, 536, 502, 388, 354, 468, 776, 662},
        {548, 0, 684, 308, 194, 502, 730, 354, 696, 742, 1084, 594, 480, 674, 1016, 868, 1210},
        {776, 684, 0, 992, 878, 502, 274, 810, 468, 742, 400, 1278, 1164, 1130, 788, 1552, 754},
        {696, 308, 992, 0, 114, 650, 878, 502, 844, 890, 1232, 514, 628, 822, 1164, 560, 1358},
        {582, 194, 878, 114, 0, 536, 764, 388, 730, 776, 1118, 400, 514, 708, 1050, 674, 1244},
        {274, 502, 502, 650, 536, 0, 228, 308, 194, 240, 582, 776, 662, 628, 514, 1050, 708},
        {502, 730, 274, 878, 764, 228, 0, 536, 194, 468, 354, 1004, 890, 856, 514, 1278, 480},
        {194, 354, 810, 502, 388, 308, 536, 0, 342, 388, 730, 468, 354, 320, 662, 742, 856},
        {308, 696, 468, 844, 730, 194, 194, 342, 0, 274, 388, 810, 696, 662, 320, 1084, 514},
        {194, 742, 742, 890, 776, 240, 468, 388, 274, 0, 342, 536, 422, 388, 274, 810, 468},
        {536, 1084, 400, 1232, 1118, 582, 354, 730, 388, 342, 0, 878, 764, 730, 388, 1152, 354},
        {502, 594, 1278, 514, 400, 776, 1004, 468, 810, 536, 878, 0, 114, 308, 650, 274, 844},
        {388, 480, 1164, 628, 514, 662, 890, 354, 696, 422, 764, 114, 0, 194, 536, 388, 730},
        {354, 674, 1130, 822, 708, 628, 856, 320, 662, 388, 730, 308, 194, 0, 342, 422, 536},
        {468, 1016, 788, 1164, 1050, 514, 514, 662, 320, 274, 388, 650, 536, 342, 0, 764, 194},
        {776, 868, 1552, 560, 674, 1050, 1278, 742, 1084, 810, 1152, 274, 388, 422, 764, 0, 798},
        {662, 1210, 754, 1358, 1244, 708, 480, 856, 514, 468, 354, 844, 730, 536, 194, 798, 0},
    };
    // [START initial_routes]
    public final long[][] initialRoutes = {
        {8, 16, 14, 13, 12, 11},
        {3, 4, 9, 10},
        {15, 1},
        {7, 5, 2, 6},
    };
    // [END initial_routes]
    public final int vehicleNumber = 4;
    public final int depot = 0;
  }
  // [END data_model]

  // [START solution_printer]
  /// @brief Print the solution.
  static void printSolution(
      DataModel data, RoutingModel routing, RoutingIndexManager manager, Assignment solution) {
    // Solution cost.
    logger.info("Objective : " + solution.objectiveValue());
    // Inspect solution.
    long maxRouteDistance = 0;
    for (int i = 0; i < data.vehicleNumber; ++i) {
      long index = routing.start(i);
      logger.info("Route for Vehicle " + i + ":");
      long routeDistance = 0;
      String route = "";
      while (!routing.isEnd(index)) {
        route += manager.indexToNode(index) + " -> ";
        long previousIndex = index;
        index = solution.value(routing.nextVar(index));
        routeDistance += routing.getArcCostForVehicle(previousIndex, index, i);
      }
      logger.info(route + manager.indexToNode(index));
      logger.info("Distance of the route: " + routeDistance + "m");
      maxRouteDistance = Math.max(routeDistance, maxRouteDistance);
    }
    logger.info("Maximum of the route distances: " + maxRouteDistance + "m");
  }
  // [END solution_printer]

  public static void main(String[] args) throws Exception {
    Loader.loadNativeLibraries();
    // Instantiate the data problem.
    // [START data]
    final DataModel data = new DataModel();
    // [END data]

    // Create Routing Index Manager
    // [START index_manager]
    RoutingIndexManager manager =
        new RoutingIndexManager(data.distanceMatrix.length, data.vehicleNumber, data.depot);
    // [END index_manager]

    // Create Routing Model.
    // [START routing_model]
    RoutingModel routing = new RoutingModel(manager);
    // [END routing_model]

    // Create and register a transit callback.
    // [START transit_callback]
    final int transitCallbackIndex =
        routing.registerTransitCallback((long fromIndex, long toIndex) -> {
          // Convert from routing variable Index to user NodeIndex.
          int fromNode = manager.indexToNode(fromIndex);
          int toNode = manager.indexToNode(toIndex);
          return data.distanceMatrix[fromNode][toNode];
        });
    // [END transit_callback]

    // Define cost of each arc.
    // [START arc_cost]
    routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);
    // [END arc_cost]

    // Add Distance constraint.
    // [START distance_constraint]
    routing.addDimension(transitCallbackIndex, 0, 3000,
        true, // start cumul to zero
        "Distance");
    RoutingDimension distanceDimension = routing.getMutableDimension("Distance");
    distanceDimension.setGlobalSpanCostCoefficient(100);
    // [END distance_constraint]

    // [START print_initial_solution]
    Assignment initialSolution = routing.readAssignmentFromRoutes(data.initialRoutes, true);
    logger.info("Initial solution:");
    printSolution(data, routing, manager, initialSolution);
    // [END print_initial_solution]

    // Setting first solution heuristic.
    // [START parameters]
    RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters();
    // [END parameters]

    // Solve the problem.
    // [START solve]
    Assignment solution = routing.solveFromAssignmentWithParameters(
        initialSolution, searchParameters);
    // [END solve]

    // Print solution on console.
    // [START print_solution]
    logger.info("Solution after search:");
    printSolution(data, routing, manager, solution);
    // [END print_solution]
  }
}
// [END program]
