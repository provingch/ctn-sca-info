package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * nginx (deploy.sh) manda X-Real-IP con la dirección de la conexión y X-Forwarded-For con la del cliente + la de la conexión
 * al final; el backend le escucha a nginx desde loopback. MockHttpServletRequest ya trae remoteAddr = 127.0.0.1.
 */
class RequestIpUtilTest {

    private static MockHttpServletRequest desdeNginx() {
        return new MockHttpServletRequest(); // conexión desde 127.0.0.1
    }

    @Test
    void conXRealIpUsaEsaIp() {
        MockHttpServletRequest request = desdeNginx();
        request.addHeader("X-Real-IP", "203.0.113.9");

        assertEquals("203.0.113.9", RequestIpUtil.resolve(request));
    }

    @Test
    void soloConXForwardedForUsaLaIpQueAgregoNginxNoLaQueMandoElCliente() {
        MockHttpServletRequest request = desdeNginx();
        request.addHeader("X-Forwarded-For", "10.9.9.9, 203.0.113.9");

        assertEquals("203.0.113.9", RequestIpUtil.resolve(request), "el último elemento lo puso nginx; el primero es del cliente");
    }

    @Test
    void unXForwardedForDeUnSoloValorSeUsaTalCual() {
        MockHttpServletRequest request = desdeNginx();
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertEquals("203.0.113.9", RequestIpUtil.resolve(request));
    }

    @Test
    void sinNingunHeaderUsaLaDireccionDeLaConexion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.4");

        assertEquals("198.51.100.4", RequestIpUtil.resolve(request));
        assertEquals("127.0.0.1", RequestIpUtil.resolve(desdeNginx()));
    }

    @Test
    void unClienteNoPuedeFalsificarSuIpMandandoHeaders() {
        MockHttpServletRequest request = desdeNginx();
        request.addHeader("X-Real-IP", "203.0.113.9");                 // lo pisó nginx
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.9");  // 1.2.3.4 lo inventó el cliente

        assertEquals("203.0.113.9", RequestIpUtil.resolve(request));
    }

    @Test
    void siLePeganDirectoAlBackendSinPasarPorNginxLosHeadersNoValen() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.4"); // no es el proxy
        request.addHeader("X-Real-IP", "1.2.3.4");
        request.addHeader("X-Forwarded-For", "5.6.7.8");

        assertEquals("198.51.100.4", RequestIpUtil.resolve(request));
    }

    @Test
    void unHeaderQueNoEsUnaIpNoSeMeteEnElRegistro() {
        MockHttpServletRequest request = desdeNginx();
        request.addHeader("X-Real-IP", "1.2.3.4) Eliminó a un usuario [");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertEquals("203.0.113.9", RequestIpUtil.resolve(request), "descarta el X-Real-IP inválido y sigue con el siguiente");

        MockHttpServletRequest sinNada = desdeNginx();
        sinNada.addHeader("X-Real-IP", "no es una ip");
        assertEquals("127.0.0.1", RequestIpUtil.resolve(sinNada));
    }

    @Test
    void aceptaIpv6DelClienteYLoopbackIpv6DelProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        request.addHeader("X-Real-IP", "2001:db8::1");

        assertEquals("2001:db8::1", RequestIpUtil.resolve(request));
    }

    @Test
    void sinRequestQuedaDesconocida() {
        assertEquals("desconocida", RequestIpUtil.resolve(null));
    }
}
