package com.telas;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Desabilitado: exige configuração externa (DB/Email) para subir contexto completo.")
class TelasApplicationTests {

    @Test
    void contextLoads() {
    }

}
