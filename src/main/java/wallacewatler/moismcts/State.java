package wallacewatler.moismcts;

import java.util.List;

public interface State<ACTION> {
    int playerAboutToMove();
    List<ACTION> availableActions();
    boolean isTerminal();
    double[] rewards();
}
