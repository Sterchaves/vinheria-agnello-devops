
package com.vinheria.produtos.controller;

import com.google.gson.Gson;
import com.vinheria.produtos.service.ProdutoService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@WebServlet("/produtos/*")
public class ProdutoServlet extends HttpServlet {

    private final ProdutoService service = new ProdutoService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");


String chaveEsperada = System.getenv("PRODUTOS_API_KEY");
String chaveRecebida = request.getHeader("X-API-Key");

if (chaveEsperada == null || chaveEsperada.isBlank()) {
    response.setStatus(503);
    response.getWriter().write(
        "{\"erro\":\"Autenticação não configurada\"}"
    );
    return;
}

boolean chaveValida = chaveRecebida != null
    && MessageDigest.isEqual(
        chaveEsperada.getBytes(StandardCharsets.UTF_8),
        chaveRecebida.getBytes(StandardCharsets.UTF_8)
    );

if (!chaveValida) {
    response.setStatus(401);
    response.getWriter().write(
        "{\"erro\":\"Não autorizado\"}"
    );
    return;
}

        String path = request.getPathInfo();

        // GET /produtos
        if (path == null || path.equals("/")) {
            response.getWriter().write(
                gson.toJson(service.listarTodos())
            );
            return;
        }

        // GET /produtos/{id}
        try {
            int id = Integer.parseInt(path.substring(1));

            var vinho = service.buscarPorId(id);

            if (vinho.isPresent()) {
                response.getWriter().write(
                    gson.toJson(vinho.get())
                );
            } else {
                response.setStatus(404);
                response.getWriter().write(
                    "{\"erro\":\"Vinho não encontrado\"}"
                );
            }

        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().write(
                "{\"erro\":\"ID inválido\"}"
            );
        }
    }
}
