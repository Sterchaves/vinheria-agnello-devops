
package com.vinheria.carrinho.controller;

import com.google.gson.Gson;
import com.vinheria.carrinho.service.CarrinhoService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet("/carrinho/*")
public class CarrinhoServlet extends HttpServlet {

    private final CarrinhoService service = new CarrinhoService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        configurarJson(response);

        if (request.getPathInfo() != null
                && !request.getPathInfo().equals("/")) {
            enviarErro(response, 404, "Rota não encontrada.");
            return;
        }

        BigDecimal subtotal = service.calcularSubtotal();

        response.getWriter().write(gson.toJson(Map.of(
            "itens", service.listar(),
            "subtotal", subtotal,
            "frete", service.calcularFrete(),
            "total", subtotal.add(service.calcularFrete())
        )));
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        configurarJson(response);

        if (!"/itens".equals(request.getPathInfo())) {
            enviarErro(response, 404, "Rota não encontrada.");
            return;
        }

        try {
            AdicionarItemDTO dados = gson.fromJson(
                request.getReader(),
                AdicionarItemDTO.class
            );

            if (dados == null) {
                enviarErro(response, 400, "Dados inválidos.");
                return;
            }

            service.adicionar(dados.vinhoId, dados.quantidade);

            response.setStatus(201);
            response.getWriter().write(gson.toJson(Map.of(
                "mensagem", "Vinho adicionado ao carrinho.",
                "itens", service.listar()
            )));

        } catch (IllegalArgumentException e) {
            enviarErro(response, 400, e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            enviarErro(response, 503,
                "Comunicação com Produtos interrompida.");

        } catch (IOException e) {
            enviarErro(response, 503,
                "Serviço de Produtos indisponível.");

        } catch (com.google.gson.JsonParseException e) {
            enviarErro(response, 400, "JSON inválido.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException {

        configurarJson(response);

        String path = request.getPathInfo();

        if (path == null || !path.matches("/itens/[0-9]+")) {
            enviarErro(response, 400, "ID inválido.");
            return;
        }

        int vinhoId = Integer.parseInt(
            path.substring("/itens/".length())
        );

        if (service.remover(vinhoId)) {
            response.getWriter().write(gson.toJson(Map.of(
                "mensagem", "Vinho removido do carrinho."
            )));
        } else {
            enviarErro(response, 404,
                "Vinho não encontrado no carrinho.");
        }
    }

    private void configurarJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void enviarErro(HttpServletResponse response,
                            int status, String mensagem)
            throws IOException {

        response.setStatus(status);
        response.getWriter().write(gson.toJson(
            Map.of("erro", mensagem)
        ));
    }

    private static class AdicionarItemDTO {
        int vinhoId;
        int quantidade;
    }
}
