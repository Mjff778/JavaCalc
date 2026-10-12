package handcalc.use_case.recognize_and_solve;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How numbers are written on screen: no trailing zeros, at most eight decimals.
 */
public final class ResultFormat {

    private static final int DECIMALS = 8;

    private ResultFormat() {
    }

    public static String format(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP);
        if (rounded.signum() == 0) {
            return "0";
        }
        return rounded.stripTrailingZeros().toPlainString();
    }
}
