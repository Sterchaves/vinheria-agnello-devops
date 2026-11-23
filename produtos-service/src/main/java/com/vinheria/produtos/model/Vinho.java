package com.vinheria.produtos.model;

import java.math.BigDecimal;

public class Vinho {

    private int id;
    private String nome;
    private String regiao;
    private String pais;
    private int safra;
    private String uva;
    private String tipo;
    private BigDecimal preco;

    public Vinho(int id, String nome, String regiao,
                 String pais, int safra, String uva,
                 String tipo, BigDecimal preco) {
        this.id = id;
        this.nome = nome;
        this.regiao = regiao;
        this.pais = pais;
        this.safra = safra;
        this.uva = uva;
        this.tipo = tipo;
        this.preco = preco;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }

    public String getPais() {
        return pais;
    }

    public int getSafra() {
        return safra;
    }

    public String getUva() {
        return uva;
    }

    public String getTipo() {
        return tipo;
    }

    public BigDecimal getPreco() {
        return preco;
    }
}
