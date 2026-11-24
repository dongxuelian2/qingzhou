package qingzhou.app.certicontrol;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.math3.linear.*;

/** Numerical certificates for finite-dimensional continuous-time LTI models. */
final class ControlAnalysis {
    private static final int LIMIT = 12;
    private static final double EPS = Math.ulp(1.0);

    private ControlAnalysis() { }

    static RealMatrix parse(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 12000) {
            throw new IllegalArgumentException("Matrix is required (maximum 12000 characters)");
        }
        String[] rows = value.trim().split("[;\\r\\n]+", -1);
        if (rows.length > LIMIT) throw new IllegalArgumentException("At most 12 rows");
        double[][] data = new double[rows.length][];
        int cols = -1;
        for (int i = 0; i < rows.length; i++) {
            String[] tokens = rows[i].trim().split("\\s*,\\s*|\\s+", -1);
            if (cols < 0) cols = tokens.length;
            if (cols != tokens.length || cols > LIMIT) throw new IllegalArgumentException("Rectangular matrix, at most 12 columns");
            data[i] = new double[cols];
            for (int j = 0; j < cols; j++) {
                double v = Double.parseDouble(tokens[j]);
                if (!Double.isFinite(v) || Math.abs(v) > 1e6) throw new IllegalArgumentException("Finite entries with magnitude <= 1000000 required");
                data[i][j] = v;
            }
        }
        return new Array2DRowRealMatrix(data, false);
    }

    static final class SystemModel {
        final RealMatrix a, b, c, k;
        final int n;
        SystemModel(Map<String, String> values) {
            a = parse(values.get("a"));
            b = parse(values.get("b"));
            c = parse(values.get("c"));
            String gain = values.get("k");
            k = gain == null || gain.trim().isEmpty() ? null : parse(gain);
            n = a.getRowDimension();
            if (a.getColumnDimension() != n || b.getRowDimension() != n || c.getColumnDimension() != n
                    || (k != null && (k.getRowDimension() != b.getColumnDimension() || k.getColumnDimension() != n))) {
                throw new IllegalArgumentException("Expected A:n*n, B:n*m, C:p*n, K:m*n");
            }
        }
    }

    static RealMatrix reachability(RealMatrix a, RealMatrix b) {
        int n = a.getRowDimension(), m = b.getColumnDimension();
        RealMatrix result = MatrixUtils.createRealMatrix(n, n * m);
        RealMatrix block = b;
        for (int i = 0; i < n; i++) {
            result.setSubMatrix(block.getData(), 0, i * m);
            if (i + 1 < n) block = a.multiply(block);
        }
        return result;
    }

    static final class Rank {
        final double[] singular;
        final double threshold;
        final int value;
        final boolean sensitive;
        Rank(RealMatrix matrix) {
            singular = new SingularValueDecomposition(matrix).getSingularValues();
            threshold = singular[0] * Math.max(matrix.getRowDimension(), matrix.getColumnDimension()) * EPS;
            int count = 0;
            boolean near = false;
            for (double s : singular) {
                if (s > threshold) count++;
                if (s > 0 && s >= threshold / 10 && s <= threshold * 10) near = true;
            }
            value = count;
            sensitive = near;
        }
        String verdict(int n) {
            return sensitive ? "INCONCLUSIVE" : value == n ? "PASS" : "FAIL";
        }
    }

    /** Real embedding of the complex PBH matrix [lambda I-A, B]. */
    private static Rank pbh(RealMatrix a, RealMatrix b, double re, double im) {
        int n = a.getRowDimension(), cols = n + b.getColumnDimension();
        RealMatrix r = MatrixUtils.createRealMatrix(n, cols);
        r.setSubMatrix(MatrixUtils.createRealIdentityMatrix(n).scalarMultiply(re).subtract(a).getData(), 0, 0);
        r.setSubMatrix(b.getData(), 0, n);
        if (im == 0) return new Rank(r);
        RealMatrix z = MatrixUtils.createRealMatrix(n, cols);
        for (int i = 0; i < n; i++) z.setEntry(i, i, im);
        RealMatrix embedded = MatrixUtils.createRealMatrix(2*n, 2*cols);
        embedded.setSubMatrix(r.getData(), 0, 0);
        embedded.setSubMatrix(z.scalarMultiply(-1).getData(), 0, cols);
        embedded.setSubMatrix(z.getData(), n, 0);
        embedded.setSubMatrix(r.getData(), n, cols);
        return new Rank(embedded);
    }

    private static String pbhVerdict(RealMatrix a, RealMatrix b, EigenDecomposition eig) {
        boolean uncertain = false, failed = false;
        for (int i = 0; i < a.getRowDimension(); i++) {
            double im = eig.getImagEigenvalue(i);
            Rank rank = pbh(a, b, eig.getRealEigenvalue(i), im);
            int expected = (im == 0 ? 1 : 2) * a.getRowDimension();
            uncertain |= rank.sensitive;
            failed |= !rank.sensitive && rank.value < expected;
        }
        return failed ? "FAIL" : uncertain ? "INCONCLUSIVE" : "PASS";
    }

    private static String combined(Rank rank, int n, String pbh) {
        String v = rank.verdict(n);
        return v.equals(pbh) ? v : "INCONCLUSIVE";
    }

    private static void spectral(RealMatrix a, String prefix, Map<String, String> out) {
        EigenDecomposition eig = new EigenDecomposition(a);
        double max = Arrays.stream(eig.getRealEigenvalues()).max().getAsDouble();
        double tol = Math.max(1, new SingularValueDecomposition(a).getNorm()) * a.getRowDimension() * EPS;
        out.put(prefix + "Stability", max < -tol ? "HURWITZ" : max > tol ? "UNSTABLE" : "BOUNDARY_OR_INCONCLUSIVE");
        out.put(prefix + "Margin", Double.toString(-max));
        out.put(prefix + "Tolerance", Double.toString(tol));
        StringBuilder poles = new StringBuilder();
        for (int i = 0; i < a.getRowDimension(); i++) {
            if (i > 0) poles.append("; ");
            poles.append(eig.getRealEigenvalue(i)).append(" + (").append(eig.getImagEigenvalue(i)).append(")i");
        }
        out.put(prefix + "Poles", poles.toString());
        if (max < -tol) lyapunov(a, prefix, out);
    }

    /** Solve A^T P + PA = -I and independently check residual and positivity. */
    private static void lyapunov(RealMatrix a, String prefix, Map<String, String> out) {
        int n = a.getRowDimension();
        RealMatrix operator = MatrixUtils.createRealMatrix(n*n, n*n);
        RealVector rhs = new ArrayRealVector(n*n);
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            int row = i*n+j;
            if (i == j) rhs.setEntry(row, -1);
            for (int q = 0; q < n; q++) {
                operator.addToEntry(row, q*n+j, a.getEntry(q,i));
                operator.addToEntry(row, i*n+q, a.getEntry(q,j));
            }
        }
        DecompositionSolver solver = new SingularValueDecomposition(operator).getSolver();
        RealVector solution = solver.solve(rhs);
        RealMatrix p = MatrixUtils.createRealMatrix(n,n);
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) p.setEntry(i,j,solution.getEntry(i*n+j));
        p = p.add(p.transpose()).scalarMultiply(0.5);
        double residual = a.transpose().multiply(p).add(p.multiply(a)).add(MatrixUtils.createRealIdentityMatrix(n)).getFrobeniusNorm();
        double min = Arrays.stream(new EigenDecomposition(p).getRealEigenvalues()).min().getAsDouble();
        out.put(prefix + "LyapunovResidual", Double.toString(residual));
        out.put(prefix + "LyapunovMinEigenvalue", Double.toString(min));
        out.put(prefix + "Lyapunov", residual <= 1e-8 && min > Math.max(1, p.getNorm()) * n * EPS ? "PASS" : "INCONCLUSIVE");
    }

    static Map<String, String> analyze(SystemModel s) {
        Map<String,String> out = new LinkedHashMap<>();
        EigenDecomposition eig = new EigenDecomposition(s.a);
        Rank cr = new Rank(reachability(s.a,s.b));
        Rank or = new Rank(reachability(s.a.transpose(),s.c.transpose()));
        String cp = pbhVerdict(s.a,s.b,eig), op = pbhVerdict(s.a.transpose(),s.c.transpose(),eig);
        out.put("states", Integer.toString(s.n));
        out.put("controllable", combined(cr,s.n,cp));
        out.put("observable", combined(or,s.n,op));
        out.put("controlRank", Integer.toString(cr.value));
        out.put("observeRank", Integer.toString(or.value));
        out.put("controlPbh", cp);
        out.put("observePbh", op);
        out.put("controlSingularValues", Arrays.toString(cr.singular));
        out.put("observeSingularValues", Arrays.toString(or.singular));
        out.put("controlTolerance", Double.toString(cr.threshold));
        out.put("observeTolerance", Double.toString(or.threshold));
        spectral(s.a,"open",out);
        if (s.k != null) spectral(s.a.subtract(s.b.multiply(s.k)),"closed",out);
        return out;
    }

    static Map<String,String> faults(SystemModel s) {
        Map<String,String> result = new LinkedHashMap<>();
        for (int i = 0; i < s.b.getColumnDimension(); i++) {
            RealMatrix b = s.b.copy();
            for (int r = 0; r < s.n; r++) b.setEntry(r,i,0);
            String label = "actuator " + (i+1);
            result.put(label + " controllability", new Rank(reachability(s.a,b)).verdict(s.n));
            if (s.k != null) {
                Map<String,String> values = new LinkedHashMap<>();
                spectral(s.a.subtract(b.multiply(s.k)),"closed",values);
                result.put(label + " closed-loop stability",values.get("closedStability"));
                result.put(label + " closed-loop margin",values.get("closedMargin"));
            }
        }
        for (int i = 0; i < s.c.getRowDimension(); i++) {
            RealMatrix c = s.c.copy();
            for (int j = 0; j < s.n; j++) c.setEntry(i,j,0);
            result.put("sensor " + (i+1) + " observability", new Rank(reachability(s.a.transpose(),c.transpose())).verdict(s.n));
        }
        return result;
    }
}
