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

// Minimal example to call the GLOP solver.
// [START program]
package ortoolsExamples.samples.linear_solver;
// [START import]
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
// [END import]

/** Minimal Linear Programming example to showcase calling the solver. */
public final class SimpleLpProgram {
  public static void main(String[] args) {
    Loader.loadNativeLibraries();
    // [START solver]
    // Create the linear solver with the GLOP backend.
    MPSolver solver = MPSolver.createSolver("GLOP");
    if (solver == null) {
      System.out.println("Could not create solver SCIP");
      return;
    }
    // [END solver]

    // [START variables]
    double infinity = Double.POSITIVE_INFINITY;
    // Create the variables x and y.
    MPVariable x = solver.makeNumVar(0.0, infinity, "x");
    MPVariable y = solver.makeNumVar(0.0, infinity, "y");

    System.out.println("Number of variables = " + solver.numVariables());
    // [END variables]

    // [START constraints]
    // x + 7 * y <= 17.5.
    MPConstraint c0 = solver.makeConstraint(-infinity, 17.5, "c0");
    c0.setCoefficient(x, 1);
    c0.setCoefficient(y, 7);

    // x <= 3.5.
    MPConstraint c1 = solver.makeConstraint(-infinity, 3.5, "c1");
    c1.setCoefficient(x, 1);
    c1.setCoefficient(y, 0);

    System.out.println("Number of constraints = " + solver.numConstraints());
    // [END constraints]

    // [START objective]
    // Maximize x + 10 * y.
    MPObjective objective = solver.objective();
    objective.setCoefficient(x, 1);
    objective.setCoefficient(y, 10);
    objective.setMaximization();
    // [END objective]

    // [START solve]
    final MPSolver.ResultStatus resultStatus = solver.solve();
    // [END solve]

    // [START print_solution]
    if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
      System.out.println("Solution:");
      System.out.println("Objective value = " + objective.value());
      System.out.println("x = " + x.solutionValue());
      System.out.println("y = " + y.solutionValue());
    } else {
      System.err.println("The problem does not have an optimal solution!");
    }
    // [END print_solution]

    // [START advanced]
    System.out.println("\nAdvanced usage:");
    System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");
    System.out.println("Problem solved in " + solver.iterations() + " iterations");
    // [END advanced]
  }

  private SimpleLpProgram() {}
}
// [END program]
