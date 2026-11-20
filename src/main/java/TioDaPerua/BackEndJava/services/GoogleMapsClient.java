package TioDaPerua.BackEndJava.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chamadas às APIs do Google Maps usadas pelo backend: Geocoding (endereço → coordenada) e Routes (rota e tempo).
 * A chave vem de GOOGLE_MAPS_API_KEY e nunca é enviada ao app. Sem a chave, {@link #configurado()} é falso
 * e as telas mostram o mapa sem rota nem previsão.
 */
@Service
public class GoogleMapsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleMapsClient.class);
    private static final String CAMPOS_ROTA = String.join(",",
            "routes.legs.duration",
            "routes.legs.distanceMeters",
            "routes.legs.polyline.encodedPolyline",
            "routes.legs.steps.distanceMeters",
            "routes.legs.steps.navigationInstruction.instructions");

    public record Coordenada(double latitude, double longitude) {
    }

    /** Um trecho da rota (de uma parada até a seguinte). */
    public record Trecho(long segundos, long metros, String polyline, String primeiraInstrucao, Long metrosPrimeiraInstrucao) {
    }

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.google.maps.api-key:}")
    private String apiKey;

    public GoogleMapsClient(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(6_000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
    }

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Coordenada do endereço, ou vazio se o Google não encontrar. */
    public Optional<Coordenada> geocodificar(String endereco) {
        try {
            String corpo = restClient.get()
                    .uri("https://maps.googleapis.com/maps/api/geocode/json?address={a}&region=br&language=pt-BR&key={k}",
                            endereco, apiKey.trim())
                    .retrieve()
                    .body(String.class);
            JsonNode raiz = objectMapper.readTree(corpo);
            String status = raiz.path("status").asString("");
            if (!"OK".equals(status)) {
                if (!"ZERO_RESULTS".equals(status)) {
                    LOGGER.warn("Geocoding recusou o pedido: {}", status);
                }
                return Optional.empty();
            }
            JsonNode local = raiz.path("results").path(0).path("geometry").path("location");
            return Optional.of(new Coordenada(local.path("lat").asDouble(), local.path("lng").asDouble()));
        } catch (RestClientException e) {
            LOGGER.warn("Geocoding indisponível ({})", e.getClass().getSimpleName());
            throw new IllegalStateException("Geocoding indisponível", e);
        }
    }

    /**
     * Rota de carro, com trânsito, saindo da origem e passando pelas paradas na ordem dada (a última é o destino).
     * Devolve um trecho por parada.
     */
    public List<Trecho> calcularRota(Coordenada origem, List<Coordenada> paradas) {
        if (paradas.isEmpty()) {
            return List.of();
        }
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("origin", ponto(origem));
        corpo.put("destination", ponto(paradas.get(paradas.size() - 1)));
        if (paradas.size() > 1) {
            corpo.put("intermediates", paradas.subList(0, paradas.size() - 1).stream().map(this::ponto).toList());
        }
        corpo.put("travelMode", "DRIVE");
        corpo.put("routingPreference", "TRAFFIC_AWARE");
        corpo.put("languageCode", "pt-BR");
        corpo.put("units", "METRIC");
        try {
            String resposta = restClient.post()
                    .uri("https://routes.googleapis.com/directions/v2:computeRoutes")
                    .header("X-Goog-Api-Key", apiKey.trim())
                    .header("X-Goog-FieldMask", CAMPOS_ROTA)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(corpo))
                    .retrieve()
                    .body(String.class);
            JsonNode legs = objectMapper.readTree(resposta).path("routes").path(0).path("legs");
            List<Trecho> trechos = new ArrayList<>();
            for (JsonNode leg : legs) {
                JsonNode passo = leg.path("steps").path(0);
                String instrucao = passo.path("navigationInstruction").path("instructions").asString(null);
                trechos.add(new Trecho(
                        segundos(leg.path("duration").asString("0s")),
                        leg.path("distanceMeters").asLong(0),
                        leg.path("polyline").path("encodedPolyline").asString(null),
                        instrucao,
                        passo.has("distanceMeters") ? passo.path("distanceMeters").asLong() : null));
            }
            return trechos;
        } catch (RestClientException e) {
            LOGGER.warn("Routes API indisponível ({})", e.getClass().getSimpleName());
            throw new IllegalStateException("Routes API indisponível", e);
        }
    }

    private Map<String, Object> ponto(Coordenada c) {
        return Map.of("location", Map.of("latLng", Map.of("latitude", c.latitude(), "longitude", c.longitude())));
    }

    /** "754s" → 754. */
    static long segundos(String duracao) {
        if (duracao == null || duracao.isBlank()) return 0;
        try {
            return Math.round(Double.parseDouble(duracao.replace("s", "")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
