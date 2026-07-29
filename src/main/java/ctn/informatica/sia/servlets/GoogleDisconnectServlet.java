package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.ProfesorDao;
import ctn.informatica.sia.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "GoogleDisconnectServlet", urlPatterns = {"/GoogleDisconnectServlet"})
public class GoogleDisconnectServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        ProfesorDao profesorDao = new ProfesorDao();
        boolean success = profesorDao.updateGoogleTokens(
                user.getId(),
                null,
                null,
                0L,
                null
        );

        if (success) {
            session.setAttribute("flashMessage", "Se ha desconectado Google Classroom correctamente.");
        } else {
            session.setAttribute("flashErrors", java.util.Collections.singletonList("No se pudo desconectar Google Classroom. Intente nuevamente."));
        }
        response.sendRedirect(request.getContextPath() + "/ProfileServlet");
    }
}
