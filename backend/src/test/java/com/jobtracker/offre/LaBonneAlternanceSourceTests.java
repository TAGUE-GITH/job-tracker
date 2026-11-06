package com.jobtracker.offre;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LaBonneAlternanceSourceTests {

    @Test
    void traduitLesMotsClesEnCodesRome() {
        assertEquals(List.of("M1805", "M1806", "M1801", "M1810"), LaBonneAlternanceSource.romes("développeur java"));
        assertEquals(List.of("M1403", "M1805", "M1806"), LaBonneAlternanceSource.romes("Data analyst"));
        assertEquals(List.of("M1503", "M1501"), LaBonneAlternanceSource.romes("assistant RH"));
        assertEquals(List.of("M1805", "E1205"), LaBonneAlternanceSource.romes("m1805 E1205"));
    }
}
