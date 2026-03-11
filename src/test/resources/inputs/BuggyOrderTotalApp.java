public class BuggyOrderTotalApp {
    public static int totalWithTax(int unitPrice, int quantity) {
        int subtotal = BuggyCalcService.multiply(unitPrice, quantity);
        // BUG: tax should be 10%, not 30%
        int tax = subtotal * 30 / 100;
        return subtotal + tax;
    }

    public static void main(String[] args) {
        int total = totalWithTax(20, 3);
        if (total == 66) {
            System.out.println("BEHAVIOR_OK");
        } else {
            System.out.println("BEHAVIOR_BAD total=" + total);
        }
    }
}
