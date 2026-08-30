package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HorarioSlotDaoNameOrderTest {

    @Test
    void buildFullNameOrdersNombreApellido() throws Exception {
        HorarioSlotDao dao = new HorarioSlotDao();
        var method = HorarioSlotDao.class.getDeclaredMethod("buildFullName", String.class, String.class);
        method.setAccessible(true);

        String both = (String) method.invoke(dao, "Rojas", "Andres");
        assertEquals("Andres Rojas", both);

        String onlyNombre = (String) method.invoke(dao, null, "Andres");
        assertEquals("Andres", onlyNombre);

        String onlyApellido = (String) method.invoke(dao, "Rojas", null);
        assertEquals("Rojas", onlyApellido);

        String neither = (String) method.invoke(dao, null, null);
        assertEquals("", neither);
    }
}
