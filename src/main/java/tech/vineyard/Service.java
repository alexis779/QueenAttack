package tech.vineyard;

import tech.vineyard.ip.IntegerProgrammingModel;
import tech.vineyard.model.Move;
import tech.vineyard.model.Output;

public class Service {
    private final IntegerProgrammingModel integerProgrammingModel;

    public Service(IntegerProgrammingModel integerProgrammingModel) {
        this.integerProgrammingModel = integerProgrammingModel;
    }

    public Output output() {
        integerProgrammingModel.buildVariables();
        integerProgrammingModel.buildConstraints();
        integerProgrammingModel.buildCost();

        return new Output(new Move[]{});
    }
}