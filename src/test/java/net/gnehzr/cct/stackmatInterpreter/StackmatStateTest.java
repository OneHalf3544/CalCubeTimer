package net.gnehzr.cct.stackmatInterpreter;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class StackmatStateTest {

    @Test
    public void testTimeFormat() throws Exception {
        assertEquals(getStackmatState().toString(), "5.39");
    }

    private StackmatState getStackmatState() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getBoolean(VariableKey.CLOCK_FORMAT)).thenReturn(Boolean.TRUE);
        StackmatState.setInverted(true, true, true);

        List<Integer> periodData = "10011010101111100110111110011010101001101001100110101100011010111010101101011110101001111"
                .chars()
                .asLongStream()
                .map(operand -> operand - '0')
                .collect(ArrayList::new, (integers, value) -> integers.add((int) value), ArrayList::addAll);

        return new StackmatState(null, periodData);
    }
}