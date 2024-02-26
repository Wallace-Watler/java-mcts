package wallacewatler.moismcts;

import java.util.List;
import java.util.Random;

public interface SimulationPolicy<MOVE> {
    MOVE chooseMove(List<MOVE> moves, Random rand);
}
