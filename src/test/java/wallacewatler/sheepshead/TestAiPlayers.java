package wallacewatler.sheepshead;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import wallacewatler.moismcts.MOISMCTS;
import wallacewatler.moismcts.SearchParameters;
import wallacewatler.moismcts.UCT;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class TestAiPlayers {
    private static final Logger LOGGER;

    static {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        final LayoutComponentBuilder layout = builder.newLayout("PatternLayout");
        layout.addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss} [%-5p] %c{1}:%L - %m%n");

        final AppenderComponentBuilder file = builder.newAppender("file", "File");
        file.addAttribute("fileName", "out/log/TestAiPlayers.log");
        file.add(layout);
        builder.add(file);

        final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.WARN);
        rootLogger.addAttribute("level", Level.INFO);
        rootLogger.add(builder.newAppenderRef("file"));
        builder.add(rootLogger);

        Configurator.initialize(builder.build());

        LOGGER = LogManager.getLogger(TestWithHumanPlayer.class);
    }

    public static void main(String[] args) {
        try {
            for(int i = 0; i < 1000; i++) {
                System.out.println(i);
                testAiPlayers();
            }
        } catch(Exception e) {
            LOGGER.error("Error. Welp...", e);
        }
    }

    private static void testAiPlayers() {
        final Random rand = new Random();
        long seed = rand.nextLong();
        rand.setSeed(seed);
        LOGGER.info("Set Random seed to '" + seed + "'.");

        final GameState rootState = new GameState(rand);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();
        final SearchParameters params = new SearchParameters()
                .withMinTime(0)
                .withMaxTime(Long.MAX_VALUE)
                .withMaxIters(1000)
                .withUct(new UCT(60 * Math.sqrt(2), true));

        LOGGER.info("Search parameters: '" + params + "'.");
        LOGGER.debug(rootState.toString());

        while(!rootState.isTerminal()) {
            final int player = rootState.playerAboutToMove;
            final PlayCard recommendedAction = MOISMCTS.search(4, infoSets.get(player), params, rand);

            LOGGER.debug("Player " + player + " plays " + recommendedAction.card() + ".");

            recommendedAction.applyToState(rootState, rand);
            for(int i = 0; i < infoSets.size(); i++)
                recommendedAction.observe(i).applyToInfoSet(infoSets.get(i));

            LOGGER.debug(rootState.toString());
        }

        LOGGER.info("Game complete.");
    }
}
