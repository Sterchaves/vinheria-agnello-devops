
package com.vinheria.produtos.service;

import com.vinheria.produtos.model.Vinho;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProdutoService {

    private final List<Vinho> vinhos = List.of(
        new Vinho(
            1, "Barolo Riserva", "Piemonte", "Itália",
            2015, "Nebbiolo", "Tinto Seco",
            new BigDecimal("1250.00")
        ),
        new Vinho(
            2, "Chianti Classico", "Toscana", "Itália",
            2020, "Sangiovese", "Tinto",
            new BigDecimal("320.00")
        ),
        new Vinho(
            3, "Chablis", "Borgonha", "França",
            2022, "Chardonnay", "Branco",
            new BigDecimal("450.00")
        )
    );

    public List<Vinho> listarTodos() {
        return vinhos;
    }

    public Optional<Vinho> buscarPorId(int id) {
        return vinhos.stream()
                .filter(vinho -> vinho.getId() == id)
                .findFirst();
    }
}
