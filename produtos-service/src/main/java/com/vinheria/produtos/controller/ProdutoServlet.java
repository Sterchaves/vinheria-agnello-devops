
package com.vinheria.produtos.controller;

import com.google.gson.Gson;
import com.vinheria.produtos.service.ProdutoService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
