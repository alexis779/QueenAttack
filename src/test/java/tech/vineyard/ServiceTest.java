package tech.vineyard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.vineyard.model.Input;
import tech.vineyard.model.Output;

import java.io.IOException;
import java.io.InputStream;

public class ServiceTest {
    @Test
    public void sample() throws IOException {
        Output output = runTest("/test.in");
        Assertions.assertTrue(output.moves().length == 1);
    }

    private static Output runTest(String fileName) throws IOException {
        InputStream inputStream = ServiceTest.class.getResourceAsStream(fileName);
        Input input = QueenAttack.parseInput2(inputStream);
        Service service = new Service(new QueenAttackModel(input));
        return service.output();
    }
}
