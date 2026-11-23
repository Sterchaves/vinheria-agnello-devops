
package com.vinheria.carrinho.service;

import com.google.gson.Gson;
import com.vinheria.carrinho.model.ItemCarrinho;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CarrinhoService {

    private final List<ItemCarrinho> itens = new ArrayList<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private final String produtosUrl = System.getenv()
            .getOrDefault(
                "PRODUTOS_URL",
                "http://localhost:8080"
            );

    public synchronized List<ItemCarrinho> listar() {
        return new ArrayList<>(itens);
    }

    public synchronized void adicionar(int vinhoId, int quantidade)
            throws IOException, InterruptedException {

        if (quantidade < 1 || quantidade > 99) {
            throw new IllegalArgumentException(
                "A quantidade deve estar entre 1 e 99."
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    produtosUrl + "/produtos/" + vinhoId
                ))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            throw new IllegalArgumentException(
                "Vinho não encontrado."
            );
        }

        if (response.statusCode() != 200) {
            throw new IOException(
                "Erro ao consultar o serviço de Produtos."
            );
        }

        ProdutoDTO produto = gson.fromJson(
            response.body(),
            ProdutoDTO.class
        );

        ItemCarrinho existente = itens.stream()
                .filter(item -> item.getVinhoId() == vinhoId)
                .findFirst()
                .orElse(null);

        if (existente != null) {
            int novaQuantidade =
                existente.getQuantidade() + quantidade;

            if (novaQuantidade > 99) {
                throw new IllegalArgumentException(
                    "Quantidade máxima de 99 unidades."
                );
            }

            existente.setQuantidade(novaQuantidade);
        } else {
            itens.add(new ItemCarrinho(
                produto.id,
                produto.nome,
                produto.preco,
                quantidade
            ));
        }
    }

    public synchronized boolean remover(int vinhoId) {
        return itens.removeIf(
            item -> item.getVinhoId() == vinhoId
        );
    }

    public synchronized BigDecimal calcularSubtotal() {
        return itens.stream()
                .map(ItemCarrinho::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized BigDecimal calcularFrete() {
        return calcularSubtotal().compareTo(
            new BigDecimal("500.00")
        ) >= 0
            ? BigDecimal.ZERO
            : new BigDecimal("29.90");
    }

    private static class ProdutoDTO {
        int id;
        String nome;
        BigDecimal preco;
    }
}
