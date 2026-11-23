
package com.vinheria.carrinho.model;

import java.math.BigDecimal;

public class ItemCarrinho {

    private int vinhoId;
    private String nome;
    private BigDecimal preco;
    private int quantidade;

    public ItemCarrinho(int vinhoId, String nome,
                        BigDecimal preco, int quantidade) {
        this.vinhoId = vinhoId;
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
    }

    public int getVinhoId() {
        return vinhoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getSubtotal() {
        return preco.multiply(
            BigDecimal.valueOf(quantidade)
        );
    }
}
