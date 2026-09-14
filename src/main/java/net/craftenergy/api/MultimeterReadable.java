package net.craftenergy.api;

import java.util.List;

/**
 * Bloco que o multímetro do Craft Energy sabe ler. Implemente no block entity: cada leitura é um
 * valor com a sua unidade (por exemplo {@code 220.0} e {@code "MV"}), mostrados na ordem em que
 * foram adicionados.
 */
public interface MultimeterReadable {
    String VOLTAGE = "MV";
    String CURRENT = "RA";
    String POWER = "CW";

    /** Acrescenta as leituras deste bloco; não adicionar nada esconde o visor. */
    void multimeterReading(List<Double> values, List<String> units);

    /** Tensão, corrente e potência, como num multímetro de verdade. */
    static void electric(List<Double> values, List<String> units, double voltage, double power) {
        values.add(voltage);
        units.add(VOLTAGE);
        values.add(voltage <= 0 ? 0.0 : power / voltage);
        units.add(CURRENT);
        values.add(power);
        units.add(POWER);
    }
}
