package wallacewatler.sheepshead;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import wallacewatler.moismcts.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;

public final class TestSheepshead {
    private static final Logger LOGGER;

    static {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        final LayoutComponentBuilder layout = builder.newLayout("PatternLayout");
        layout.addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss} [%-5p] %c{1}:%L - %m%n");

        final AppenderComponentBuilder file = builder.newAppender("file", "File");
        file.addAttribute("fileName", "out/log/TestSheepshead.log");
        file.add(layout);
        builder.add(file);

        final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.WARN);
        rootLogger.addAttribute("level", Level.INFO);
        rootLogger.add(builder.newAppenderRef("file"));
        builder.add(rootLogger);

        Configurator.initialize(builder.build());

        LOGGER = LogManager.getLogger(TestSheepshead.class);
    }

    public static void main(String[] args) {
        try {
            testAiPlayers();
        } catch(Exception e) {
            LOGGER.error("Error. Welp...", e);
        }
    }

    private static void testAiPlayers() {
        final Random rand = new Random();
        long seed = rand.nextLong();
        //seed = -4701042718665950934L;
        rand.setSeed(seed);
        LOGGER.info("Set Random seed to '" + seed + "'.");

        final GameState rootState = new GameState(rand);
        final UCT uctPolicy = new UCT(60 * Math.sqrt(2), true);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();
        final MOISMCTS<GameState, PlayCard, InfoSet, PlayCard> mcts = new MOISMCTS<>(rootState, infoSets, uctPolicy, new RandomSimulation<>());
        final SearchConstraints constraints = new SearchConstraints().withMinTime(0).withMaxTime(Long.MAX_VALUE).withMaxIters(1000);
        LOGGER.info("Search constraints: '" + constraints + "'.");

        LOGGER.debug(rootState.toString());

        while(!rootState.isTerminal()) {
            mcts.search(constraints, rand);
            final PlayCard action = mcts.recommendAction(rand);
            LOGGER.debug("Player " + mcts.rootState.playerAboutToMove + " plays " + action.card() + ".");
            mcts.advanceRootState(action, rand);
            LOGGER.debug(rootState.toString());
        }

        LOGGER.info("Game complete.");
    }

    private static void testWithHumanPlayer() {
        final Scanner in = new Scanner(System.in);
        final Random rand = new Random();
        long seed = rand.nextLong();
        //seed = -4701042718665950934L;
        rand.setSeed(seed);
        LOGGER.info("Set Random seed to '" + seed + "'.");

        final GameState rootState = new GameState(rand);
        final UCT uctPolicy = new UCT(60 * Math.sqrt(2), true);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();
        final MOISMCTS<GameState, PlayCard, InfoSet, PlayCard> mcts = new MOISMCTS<>(rootState, infoSets, uctPolicy, new RandomSimulation<>());
        final SearchConstraints constraints = new SearchConstraints().withMinTime(1000).withMaxTime(5000).withMaxIters(100000);
        LOGGER.info("Search constraints: '" + constraints + "'.");

        LOGGER.debug(rootState.toString());

        while(!rootState.isTerminal()) {
            System.out.println("Table: " + Arrays.toString(mcts.rootState.trickOnTable.cards) + ", led " + mcts.rootState.trickOnTable.ledCard());

            final PlayCard action;
            if(mcts.rootState.playerAboutToMove == 0) {
                System.out.println("Hand: " + mcts.rootState.players[0].hand);
                final List<PlayCard> availableActions = mcts.rootState.availableActions();
                for(int i = 0; i < availableActions.size(); i++)
                    System.out.printf("- %d) %s%n", i + 1, availableActions.get(i).card());

                action = availableActions.get(in.nextInt() - 1);
            } else {
                mcts.search(constraints, rand);
                action = mcts.recommendAction(rand);
            }
            System.out.println("Player " + mcts.rootState.playerAboutToMove + " plays " + action.card() + ".");
            LOGGER.debug("Player " + mcts.rootState.playerAboutToMove + " plays " + action.card() + ".");
            mcts.advanceRootState(action, rand);
            LOGGER.debug(rootState.toString());
        }

        final double[] rewards = mcts.rootState.rewards();
        System.out.println("Players 0 and 2 score: " + (int) rewards[0]);
        System.out.println("Players 1 and 3 score: " + (int) rewards[1]);

        LOGGER.info("Game complete.");
    }
}
