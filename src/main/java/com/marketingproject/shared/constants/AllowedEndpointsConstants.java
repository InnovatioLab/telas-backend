package br.gov.ce.ematerce.raizescearenses.shared.constants;

import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RotasPermitidasConstants {
  protected static final Map<HttpMethod, List<String>> ROTAS_PERMITIDAS = new HashMap<>();

  static {
    ROTAS_PERMITIDAS.put(HttpMethod.GET, List.of(
            "/health-check",
            "/qa",
            "/qa/**",
            "/usuario/{login}",
            "/usuario/situacao-cadastro/{login}",
            "/usuario/perfil/tipo",
            "/usuario/validar-usuario-cadastrado/{email}",
            "/usuario/busca-usuario/{identificador}",
            "/usuario/verificar-whatsapp/{numeroWhatsapp}",
            "/usuario/produtores",
            "/usuario/dados-usuario/{id}",
            "/termo-condicao/consulta",
            "/grupo-categoria/**",
            "/oferta/dados-oferta/{id}",
            "/oferta/usuario/dados-oferta/{id}",
            "/swagger-ui/**",
            "/v*/api-docs/**",
            "/actuator/**",
            "/swagger-resources/**",
            "/docs"
    ));
    ROTAS_PERMITIDAS.put(HttpMethod.POST, List.of(
            "/usuario",
            "/usuario/upload/{login}",
            "/usuario/reenviar-codigo/{login}",
            "/newsletter/inscricao-newsletter",
            "/oferta/dados-oferta",
            "/oferta/filtro",
            "/auth/login",
            "/auth/login-social",
            "/auth/atualizar-token",
            "/auth/recuperar-senha/{login}"
    ));
    ROTAS_PERMITIDAS.put(HttpMethod.PATCH, List.of(
            "/usuario/criar-senha/{login}",
            "/usuario/validacao-codigo/{login}",
            "/usuario/alterar-contato/{login}",
            "/usuario/atualiza-dados-perfil/{id}",
            "/auth/redefinir-senha/{login}"
    ));
    ROTAS_PERMITIDAS.put(HttpMethod.DELETE, List.of(
            "/qa",
            "/qa/**",
            "/usuario/inativar-firebase/{uid}",
            "/auth/invalidar-codigo-senha/{login}"
    ));
    ROTAS_PERMITIDAS.put(HttpMethod.PUT, List.of(
            "/usuario/{id}"
    ));
  }

  private RotasPermitidasConstants() {
  }

  public static Map<HttpMethod, List<String>> getRotasPermitidas() {
    return ROTAS_PERMITIDAS;
  }

  public static boolean isRotaPermitida(HttpMethod method, String uri) {
    String normalizedUri = uri.replaceAll("/\\d+", "/*").replaceAll("/[a-f0-9\\-]{36}", "/*").replaceAll("\\{\\w+\\}", "*");
    return ROTAS_PERMITIDAS.getOrDefault(method, List.of()).stream()
            .anyMatch(allowedUri -> allowedUri.replaceAll("\\{\\w+\\}", "*").equals(normalizedUri));
  }
}
