/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sia.servlets;

import ctn.informatica.sia.clases.conexion;
import ctn.informatica.sia.model.User;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Admin dashboard skeleton for real administrator users.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/AdminServlet"})
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try (Connection c = new conexion().getCon()) {
            request.setAttribute("profesorCount", countRows(c, "SELECT COUNT(*) FROM profesor"));
            request.setAttribute("cursoCount", countRows(c, "SELECT COUNT(*) FROM curso"));
            request.setAttribute("especialidadCount", countRows(c, "SELECT COUNT(*) FROM especialidad"));
        } catch (SQLException ex) {
            throw new ServletException("Error loading admin dashboard statistics", ex);
        }

        request.getRequestDispatcher("/Admin.jsp").forward(request, response);
    }

    private int countRows(Connection connection, String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

}
