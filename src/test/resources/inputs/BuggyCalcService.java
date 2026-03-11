public class BuggyCalcService {
    public static int multiply(int a, int b) {
        // BUG: should multiply but currently adds
        return a + b;
    }
}
