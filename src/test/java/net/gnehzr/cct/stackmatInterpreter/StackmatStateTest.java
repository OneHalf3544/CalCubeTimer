package net.gnehzr.cct.stackmatInterpreter;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class StackmatStateTest {

    @Test(enabled = false)
    public void testTimeFormat() throws Exception {
        List<Integer> periodData = "10011010101111100110111110011010101001101001100110101100011010111010101101011110101001111"
                .chars()
                .asLongStream()
                .map(operand -> operand - '0')
                .collect(ArrayList::new, (integers, value) -> integers.add((int) value), ArrayList::addAll);

        StackmatState stackmatState = new StackmatState(null, periodData);

        System.out.println(periodData.toString());
        System.out.println(stackmatState);
        assertEquals(stackmatState.toString(), "15.16");
    }
}