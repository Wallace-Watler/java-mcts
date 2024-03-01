package wallacewatler.javamcts.sheepshead;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import wallacewatler.javamcts.SearchParameters;
import wallacewatler.javamcts.UCT;
import wallacewatler.javamcts.moismcts.*;

import java.util.*;
import java.util.stream.IntStream;

public final class TestWithHumanPlayer {
    private static final Logger LOGGER;

    static {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        final LayoutComponentBuilder layout = builder.newLayout("PatternLayout");
        layout.addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss} [%-5p] %c{1}:%L - %m%n");

        final AppenderComponentBuilder file = builder.newAppender("file", "File");
        file.addAttribute("fileName", "out/log/TestWithHumanPlayer.log");
        file.add(layout);
        builder.add(file);

        final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.WARN);
        rootLogger.addAttribute("level", Level.DEBUG);
        rootLogger.add(builder.newAppenderRef("file"));
        builder.add(rootLogger);

        Configurator.initialize(builder.build());

        LOGGER = LogManager.getLogger(TestWithHumanPlayer.class);
    }

    public static void main(String[] args) {
        try {
            testWithHumanPlayer();
        } catch(Exception e) {
            LOGGER.error("Error. Welp...", e);
        }
    }

    private static void testWithHumanPlayer() {
        final Scanner in = new Scanner(System.in);
        final Random rand = new Random();
        long seed = rand.nextLong();
        rand.setSeed(seed);
        LOGGER.info("Set Random seed to '" + seed + "'.");

        final GameState rootState = new GameState(rand);
        final List<wallacewatler.javamcts.sheepshead.InfoSet> infoSets = IntStream.range(0, 3).mapToObj(i -> new InfoSet(rootState, i)).toList();
        final SearchParameters params = new SearchParameters(1000, 5000, 100000, new UCT(60 * Math.sqrt(2), true), 4);

        LOGGER.info("Search parameters: '" + params + "'.");
        LOGGER.debug(rootState.toString());

        while(!rootState.isTerminal()) {
            final int player = rootState.playerAboutToMove;

            System.out.println("Table: " + Arrays.toString(rootState.trickOnTable.cards) + ", led " + rootState.trickOnTable.ledCard());

            final PlayCard action;
            if(player == 3) {
                System.out.println("Hand: " + rootState.players[3].hand);
                final List<PlayCard> availableActions = rootState.availableActions();
                for(int i = 0; i < availableActions.size(); i++)
                    System.out.printf("- %d) %s%n", i + 1, availableActions.get(i).card());

                action = availableActions.get(in.nextInt() - 1);
            } else {
                action = MOISMCTS.search(4, infoSets.get(player), params, rand);
            }
            System.out.println("Player " + player + " plays " + action.card() + ".");
            LOGGER.debug("Player " + player + " plays " + action.card() + ".");

            action.applyToState(rootState, rand);
            for(int i = 0; i < infoSets.size(); i++)
                action.observe(i).applyToInfoSet(infoSets.get(i));

            LOGGER.debug(rootState.toString());
        }

        final double[] rewards = rootState.scores();
        System.out.println("Players 0 and 2 score: " + (int) rewards[0]);
        System.out.println("Players 1 and 3 score: " + (int) rewards[1]);

        LOGGER.info("Game complete.");
    }
}
