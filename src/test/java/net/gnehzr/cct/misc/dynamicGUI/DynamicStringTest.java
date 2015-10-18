package net.gnehzr.cct.misc.dynamicGUI;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.SortedProperties;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.statistics.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static net.gnehzr.cct.misc.dynamicGUI.DStringPart.Type.I18N_TEXT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * <p>
 * <p>
 * Created: 18.10.2015 12:03
 * <p>
 *
 * @author OneHalf
 */
public class DynamicStringTest {

    private Configuration configuration;
    private SessionsList sessionsList;
    private XMLGuiMessages xmlGuiMessages;

    @BeforeClass
    public void setUpClass() {
        configuration = new Configuration(new SortedProperties(ImmutableMap.of(),
                ImmutableMap.of(
                        "RA0.size", "12"
                )));

        xmlGuiMessages = mock(XMLGuiMessages.class);
        when(xmlGuiMessages.getString("Best_rolling_average_of")).thenReturn("Best rolling average 10 of");

        RollingAverage rollingAverage = mock(RollingAverage.class);
        when(rollingAverage.getAverage()).thenReturn(new SolveTime("23.12"));

        SessionPuzzleStatistics statistics = mock(SessionPuzzleStatistics.class);
        when(statistics.getBestAverage(any(RollingAverageOf.class))).thenReturn(rollingAverage);

        Session currentSession = mock(Session.class);
        when(currentSession.getStatistics()).thenReturn(statistics);

        sessionsList = mock(SessionsList.class);
        when(sessionsList.getCurrentSession()).thenReturn(currentSession);

    }


    @Test
    public void testParsing() throws Exception {
        String rawString = "%%Best_rolling_average_of%% @@RA0.size@@: $$ra(0,best)$$";

        DynamicString dynamicString = new DynamicString(
                rawString,
                xmlGuiMessages,
                configuration);

        assertEquals(dynamicString.getRawText(), rawString);

        List<DStringPart> splitString = dynamicString.getParts();
        assertEquals(splitString, ImmutableList.of(
                new DStringPart("Best_rolling_average_of", DStringPart.Type.I18N_TEXT),
                new DStringPart(" ", DStringPart.Type.RAW_TEXT),
                new DStringPart("RA0.size", DStringPart.Type.CONFIGURATION_TEXT),
                new DStringPart(": ", DStringPart.Type.RAW_TEXT),
                new DStringPart("ra(0,best)", DStringPart.Type.STATISTICS_TEXT)
                ));

        String processedString = dynamicString.toString(RollingAverageOf.OF_5, sessionsList);
        assertEquals(processedString, "Best rolling average 10 of 12: 23.12");
    }

    @Test
    public void testParsePlaceholders() throws Exception {
        String dynamicString = "raw-string1 %%stat-value%% raw-string2";

        List<DStringPart> parts = DynamicString.parsePlaceholders(
                dynamicString,
                I18N_TEXT, "%%",
                s -> Collections.singletonList(new DStringPart(s, DStringPart.Type.RAW_TEXT)));

        assertEquals(parts, ImmutableList.of(
                new DStringPart("raw-string1 ", DStringPart.Type.RAW_TEXT),
                new DStringPart("stat-value", DStringPart.Type.I18N_TEXT),
                new DStringPart(" raw-string2", DStringPart.Type.RAW_TEXT)
        ));
    }
}