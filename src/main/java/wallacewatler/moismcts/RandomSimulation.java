package wallacewatler.moismcts;

import java.util.List;
import java.util.Random;

/**
 * A simulation policy that chooses a player's action randomly from a uniform distribution.
 *
 * @since 0.1
 *
 * @author Wallace Watler
 */
public final class RandomSimulation<MOVE> implements SimulationPolicy<MOVE> {
    @Override
    public MOVE chooseMove(List<MOVE> moves, Random rand) {
        return moves.get(rand.nextInt(moves.size()));
    }
}
