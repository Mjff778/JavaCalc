package handcalc.view;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Pinch-to-zoom on a Mac trackpad.
 *
 * <p>Java has no public API for trackpad gestures; macOS JDKs have one in
 * {@code com.apple.eawt.event}, which must be opened with
 * {@code --add-exports java.desktop/com.apple.eawt.event=ALL-UNNAMED} (see
 * {@code .mvn/jvm.config}). Where it is missing, pinching does nothing and the zoom key with
 * the scroll wheel still zooms.
 */
final class Pinch {

    private static final Logger LOGGER = Logger.getLogger(Pinch.class.getName());

    private Pinch() {
    }

    /**
     * @param onPinch told how much the fingers spread (positive) or closed (negative), as a
     *                fraction of the current size
     * @return true if pinching is available
     */
    static boolean addTo(JComponent component, DoubleConsumer onPinch) {
        try {
            Class<?> utilities = Class.forName("com.apple.eawt.event.GestureUtilities");
            Class<?> gestureListener = Class.forName("com.apple.eawt.event.GestureListener");
            Class<?> magnificationListener =
                    Class.forName("com.apple.eawt.event.MagnificationListener");
            InvocationHandler handler = (proxy, method, args) -> handle(proxy, method, args,
                    onPinch);
            Object listener = Proxy.newProxyInstance(Pinch.class.getClassLoader(),
                    new Class<?>[] {magnificationListener}, handler);
            utilities.getMethod("addGestureListenerTo", JComponent.class, gestureListener)
                    .invoke(null, component, listener);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "Pinch-to-zoom is not available", e);
            return false;
        }
    }

    private static Object handle(Object proxy, Method method, Object[] args,
                                 DoubleConsumer onPinch) throws ReflectiveOperationException {
        switch (method.getName()) {
            case "magnify":
                double amount = (Double) args[0].getClass().getMethod("getMagnification")
                        .invoke(args[0]);
                if (SwingUtilities.isEventDispatchThread()) {
                    onPinch.accept(amount);
                } else {
                    SwingUtilities.invokeLater(() -> onPinch.accept(amount));
                }
                return null;
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == args[0];
            case "toString":
                return "pinch-to-zoom";
            default:
                return null;
        }
    }
}
