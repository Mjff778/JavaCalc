package handcalc.interface_adapter;

/**
 * Text the calculator writes on the page after the "=": the answer, or "undefined" (an
 * error) when the expression has no value.
 */
public final class WrittenAnswer {

    private final String text;
    private final double x;
    private final double centreY;
    private final double height;
    private final boolean error;

    /**
     * @param x the left edge of the text
     * @param centreY the height the text's digits are centred on
     * @param height how tall the text's digits are
     */
    public WrittenAnswer(String text, double x, double centreY, double height, boolean error) {
        this.text = text;
        this.x = x;
        this.centreY = centreY;
        this.height = height;
        this.error = error;
    }

    public String getText() {
        return text;
    }

    public double getX() {
        return x;
    }

    public double getCentreY() {
        return centreY;
    }

    public double getHeight() {
        return height;
    }

    public boolean isError() {
        return error;
    }
}
