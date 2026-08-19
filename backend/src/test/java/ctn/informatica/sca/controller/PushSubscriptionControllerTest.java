package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.PushSubscriptionDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.PushSubscriptionSaveRequest;
import ctn.informatica.sca.dto.PushSubscriptionResponse;
import ctn.informatica.sca.model.User;
import java.sql.SQLException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionControllerTest {

    @Mock
    private PushSubscriptionDao pushSubscriptionDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private PushSubscriptionController controller;

    @Test
    void shouldReturnSubscriptionStatus() throws Exception {
        User user = new User(4, "profesor", "Profesor Uno", 1);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getId(), null);

        when(userDao.findById(4)).thenReturn(user);
        when(pushSubscriptionDao.findByUser(4, "profesor")).thenReturn(Collections.emptyList());

        PushSubscriptionResponse response = controller.getPushSubscription(authentication);

        assertNotNull(response);
        assertFalse(response.subscribed());
        assertNotNull(response.publicKey());
    }

    @Test
    void shouldSavePushSubscription() throws Exception {
        User user = new User(5, "profesor", "Profesor Dos", 1);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getId(), null);

        when(userDao.findById(5)).thenReturn(user);
        when(pushSubscriptionDao.save(eq(5), eq("profesor"), anyString(), anyString(), anyString())).thenReturn(true);

        controller.savePushSubscription(new PushSubscriptionSaveRequest("https://example.com/sub", "p256dh", "auth"), authentication);

        verify(pushSubscriptionDao, times(1)).save(eq(5), eq("profesor"), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectIncompletePushSubscription() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(5, null);

        assertThrows(ResponseStatusException.class, () -> controller.savePushSubscription(
                new PushSubscriptionSaveRequest("https://example.com/sub", "", "auth"), authentication));
        verifyNoInteractions(pushSubscriptionDao);
    }

    @Test
    void shouldUnsubscribePushSubscription() throws Exception {
        User user = new User(6, "profesor", "Profesor Tres", 1);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getId(), null);

        when(userDao.findById(6)).thenReturn(user);

        controller.unsubscribePushSubscription(authentication);

        verify(pushSubscriptionDao, times(1)).deleteByUser(6, "profesor");
    }
}
