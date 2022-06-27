/***********************************************************************
   Xpress Optimizer Examples
   =========================

   file loadlp.c
   ``````````````
   Load an LP problem directly into Optimizer and modify it by adding an
   extra constraint.

   The problem
     Maximise
            2x + y
     subject to
       c1:  x + 4y <= 24
            c2:       y <=  5
            c3: 3x +  y <= 20
            c4:  x +  y <=  9
    and
            0 <= x,y <= +infinity
    and the extra constraint
            c5: 6x + y <= 20
   are first stored in the user's data structures. The LP is then loaded
   into Optimizer, using loadprob, and solved using the primal simplex
   algorithm. Next, the extra constraint is added to the problem matrix,
   using addrows, and the revised problem solved using the dual algorithm.
   In each case, the problem matrix is output to a file, the objective
   function value displayed on screen, and the problem statistics are
   are stored in a log file.

   (c) 2020 Fair Isaac Corporation
***********************************************************************/
package xpress;
import com.dashoptimization.DefaultMessageListener;
import com.dashoptimization.XPRS;
import static com.dashoptimization.XPRSenumerations.ObjSense;
import com.dashoptimization.XPRSprob;

public final class LoadLP {

    public static void main(String[] args) {
        String problem1 = "lp";
        String problem2 = "revised";
        String logFile = "loadlp.log";

        // Initialise Optimizer
        try (XPRSprob prob = new XPRSprob(null)) {
            // Delete and define log file
            new java.io.File(logFile).delete();
            prob.setLogFile(logFile);

            // Enable standard message output
            prob.addMessageListener(new DefaultMessageListener());

            /*** Load and solve the problem ***/

            // Define the problem. Note that this could also be passed
            // directly to the optimzer. We create the arrays here explicitly
            // to make things more readable.
            // Row data: row types, right-hand sides, range values (not here)
            byte[]   rowType = new byte[]{'L','L','L','L'};
            double[] rhs     = new double[]{24,5,20,9};
            String[] rowName = new String[]{"c1", "c2", "c3", "c4"};
            // Column data: objective coefficient, lower and upper bounds
            double[] obj     = new double[]{2,1};
            double[] lb      = new double[]{0,0};
            double[] ub      = new double[]{XPRS.PLUSINFINITY,XPRS.PLUSINFINITY};
            String[] colName = new String[]{ "x", "y" };

            // Matrix data. The matrix is stored in sparse form.
            // rowInd[] and rowVal[] hold the non-zero indices and values.
            // For each column j, the non-zero indices are stored in
            //    rowInd[colStart[j]] ... rowInd[colStart[j+1]]
            // and the non-zero values are stored in
            //    rowVal[colStart[j]] ... rowVal[colStart[j+1]]
            // (the last element is exclusive). So the colStart[] array
            // has one more element than there are columns.
            int[] colStart  = new int[]{0,3,7};
            int[] rowInd    = new int[]{0,2,3,  0,1,2,3};
            double[] rowVal = new double[]{1,3,1,  4,1,1,1};

            // Load the matrix into Optimizer
            // We can pass the range argument as null since we don't create
            // any range constraint. We can also pass the elemcount array as
            // null since we pass the required information in colStart.
            prob.loadLp(problem1, obj.length, rhs.length, rowType, rhs, null,
                        obj, colStart, null, rowInd, rowVal, lb, ub);

            // Add row names
            prob.addNames(1, rowName, 0, rowName.length - 1);

            // Add column names
            prob.addNames(2, colName, 0, colName.length - 1);

            // Output the matrix
            prob.writeProb(problem1, "");
            System.out.printf("Matrix file %s.mat has been created.%n", problem1);

            // Solve the LP problem
            prob.chgObjSense(ObjSense.MAXIMIZE);
            prob.lpOptimize("");

            // Get and display the value of the objective function
            double objVal = prob.attributes().getLPObjVal();
            System.out.println("The optimal objective value is " + objVal);

            /*** Add the extra constraint and solve again ***/

            // Store extra constraint.
            // Storage layout is the same as for the original constraints.
            // The index of the new row will be the current row count.
            int newIdx      = prob.attributes().getRows();
            byte[] newType  = new byte[]{'L'};
            String newName  = "c5";
            double[] newRhs = new double[]{20};
            double[] newVal = new double[]{6,1};
            int[] newStart  = new int[]{0,2};
            int[] newInd    = new int[]{0,1};

            // Add new row
            int rows = prob.attributes().getRows();
            prob.addRows(1, newVal.length, newType, newRhs, null,
                         newStart, newInd, newVal);
            // Add new row name
            prob.addNames(1, new String[]{ newName }, rows, rows);

            // Output the revised matrix
            prob.writeProb(problem2, "");

            // Solve with dual - since the revised problem inherits dual
            // feasibility from the original
            prob.lpOptimize("d");

            // Get and display the value of the objective function
            objVal = prob.attributes().getLPObjVal();
            System.out.println("The revised optimal objective value is " + objVal);
        }
    }
}
