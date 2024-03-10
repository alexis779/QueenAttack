package tech.vineyard.ip;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;

import java.util.HashMap;
import java.util.Map;

public class Optimizer {
    private static final String COST_VARIABLE = "cost";
    public final CpSolver cpSolver = new CpSolver();
    public final CpModel cpModel = new CpModel();

    private final Map<String, BoolVar> booleanVars = new HashMap<>();
    private final Map<String, IntVar> intVars = new HashMap<>();

    public Optimizer() {
        Loader.loadNativeLibraries();
    }

    public int optimizeCost() {
        IntVar cost = getIntVar(costVariable());

        cpModel.minimize(cost);
        CpSolverStatus status = cpSolver.solve(cpModel);

        if (status != CpSolverStatus.OPTIMAL) {
            throw new RuntimeException("Can not find optimal solution");
        }

        return (int) cpSolver.value(cost);
    }

    private IntVar getIntVar(String name) {
        return intVars.get(name);
    }

    public IntVar newIntVar(long l, long u, String name) {
        IntVar intVar = cpModel.newIntVar(l, u, name);
        intVars.put(name, intVar);
        return intVar;
    }

    public BoolVar newBoolVar(String name) {
        BoolVar boolVar = cpModel.newBoolVar(name);
        booleanVars.put(name, boolVar);
        return boolVar;
    }

    public String costVariable() {
        return COST_VARIABLE;
    }
}
