package ctn.informatica.sia.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void specialtyNameAccessorReturnsSafeDefaultValue() {
        User user = new User(1, "teacher", "Teacher Name", 1);

        assertNotNull(user.getSpecialtyName());
        assertEquals("", user.getSpecialtyName());
    }
}
