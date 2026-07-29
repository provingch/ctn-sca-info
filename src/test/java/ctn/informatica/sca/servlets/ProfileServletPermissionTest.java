package ctn.informatica.sca.servlets;

import ctn.informatica.sca.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServletPermissionTest {

    @Test
    void nonAdminUsersCannotEditAdminOnlyFields() {
        ProfileServlet servlet = new ProfileServlet();
        User teacher = new User(7, "teacher", "Teacher", 1);

        assertFalse(servlet.canModifyField("nombre", teacher));
        assertFalse(servlet.canModifyField("apellido", teacher));
        assertFalse(servlet.canModifyField("ci", teacher));
        assertFalse(servlet.canModifyField("nivel", teacher));
    }

    @Test
    void nonAdminUsersCanEditTheirOwnEditableFields() {
        ProfileServlet servlet = new ProfileServlet();
        User teacher = new User(7, "teacher", "Teacher", 1);

        assertTrue(servlet.canModifyField("correo", teacher));
        assertTrue(servlet.canModifyField("telefono", teacher));
        assertTrue(servlet.canModifyField("celular", teacher));
        assertTrue(servlet.canModifyField("usuario", teacher));
        assertTrue(servlet.canModifyField("especialidadId", teacher));
    }

    @Test
    void adminsCanEditAdminOnlyFields() {
        ProfileServlet servlet = new ProfileServlet();
        User admin = new User(3, "admin", "Admin", 3);

        assertTrue(servlet.canModifyField("nombre", admin));
        assertTrue(servlet.canModifyField("apellido", admin));
        assertTrue(servlet.canModifyField("ci", admin));
        assertTrue(servlet.canModifyField("nivel", admin));
    }
}
