package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.DependenteParadaDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.DTOs.ParadaRotaDTO;
import TioDaPerua.BackEndJava.DTOs.RotaViagemDTO;
import TioDaPerua.BackEndJava.entities.GeocodificacaoEndereco;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import TioDaPerua.BackEndJava.repositories.GeocodificacaoEnderecoRepository;
import TioDaPerua.BackEndJava.repositories.LocalizacaoViagemAtualRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Rota e previsão de chegada da viagem em andamento ("Uber" para o responsável, "Waze" para o motorista).
 *
 * A rota sai da posição atual do veículo e passa pelas paradas que ainda têm aluno pendente, na ordem da viagem.
 * O motorista recebe a rota inteira; o responsável só o trecho até a parada do(s) próprio(s) filho(s):
 * - aluno esperando → a parada de embarque (casa na ida, escola na volta);
 * - aluno embarcado → a parada de desembarque (escola na ida, casa na volta).
 * O responsável nunca recebe coordenadas das paradas de outras famílias.
 */
@Service
public class RotaViagemService {

    /** A rota de uma viagem é recalculada no máximo uma vez por minuto (custo da Routes API). */
    private static final Duration VALIDADE_ROTA = Duration.ofSeconds(60);

    private record ParadaGeo(ParadaDTO parada, GoogleMapsClient.Coordenada coordenada) {
    }

    private record RotaCalculada(Instant em, GoogleMapsClient.Coordenada origem,
                                 List<ParadaGeo> paradas, List<GoogleMapsClient.Trecho> trechos) {
    }

    private final Map<Long, RotaCalculada> cache = new ConcurrentHashMap<>();

    private final GoogleMapsClient google;
    private final ViagemService viagemService;
    private final LocalizacaoViagemService localizacaoViagemService;
    private final ViagemDiaRepository viagemDiaRepository;
    private final LocalizacaoViagemAtualRepository localizacaoAtualRepository;
    private final ResponsavelRepository responsavelRepository;
    private final GeocodificacaoEnderecoRepository geocodificacaoRepository;

    public RotaViagemService(GoogleMapsClient google, ViagemService viagemService,
                             LocalizacaoViagemService localizacaoViagemService,
                             ViagemDiaRepository viagemDiaRepository,
                             LocalizacaoViagemAtualRepository localizacaoAtualRepository,
                             ResponsavelRepository responsavelRepository,
                             GeocodificacaoEnderecoRepository geocodificacaoRepository) {
        this.google = google;
        this.viagemService = viagemService;
        this.localizacaoViagemService = localizacaoViagemService;
        this.viagemDiaRepository = viagemDiaRepository;
        this.localizacaoAtualRepository = localizacaoAtualRepository;
        this.responsavelRepository = responsavelRepository;
        this.geocodificacaoRepository = geocodificacaoRepository;
    }

    /**
     * @param latitude/longitude posição informada pelo app do motorista (opcional; o responsável não envia)
     */
    @Transactional
    public RotaViagemDTO calcular(Long viagemDiaId, Long usuarioId, Double latitude, Double longitude) {
        ViagemDia execucao = viagemDiaRepository.findById(viagemDiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Execução de viagem não encontrada com id: " + viagemDiaId));
        boolean motorista = viagemDiaRepository.existsByIdAndViagem_Motorista_Usuario_Id(viagemDiaId, usuarioId);
        if (!motorista) {
            // A rota começa na posição do veículo: mesmas regras (e consentimento) da localização.
            localizacaoViagemService.validarLeituraLocalizacao(viagemDiaId, usuarioId);
        }
        if (execucao.getStatus() != StatusViagemDia.EM_ANDAMENTO) {
            return RotaViagemDTO.indisponivel("A rota aparece quando a viagem está em andamento.");
        }
        if (!google.configurado()) {
            return RotaViagemDTO.indisponivel("Previsão de chegada indisponível: o servidor ainda não tem a chave do Google Maps.");
        }

        boolean ida = execucao.getViagem() != null && execucao.getViagem().getPeriodo() != null
                && execucao.getViagem().getPeriodo().name().endsWith("_IDA");
        List<ParadaDTO> todas = viagemService.gerarParadasViagemDia(viagemDiaId);
        List<ParadaDTO> pendentes = todas.stream().filter(p -> pendente(p, ida)).toList();
        if (pendentes.isEmpty()) {
            return RotaViagemDTO.indisponivel("Todas as paradas foram concluídas.");
        }

        GoogleMapsClient.Coordenada origem = origem(viagemDiaId, motorista, latitude, longitude, todas);
        if (origem == null) {
            return RotaViagemDTO.indisponivel("Aguardando a primeira posição do veículo.");
        }

        RotaCalculada rota;
        try {
            rota = rotaEmCache(viagemDiaId, origem, pendentes);
        } catch (IllegalStateException e) {
            return RotaViagemDTO.indisponivel("Não foi possível calcular a rota agora. Tentaremos de novo em instantes.");
        }
        if (rota.paradas().isEmpty()) {
            return RotaViagemDTO.indisponivel("Os endereços das paradas não foram encontrados no mapa.");
        }
        return motorista ? paraMotorista(rota) : paraResponsavel(rota, usuarioId, ida);
    }

    // ---------------------------------------------------------------- regras

    /** Parada ainda a visitar: embarque com aluno esperando; desembarque com aluno esperando ou a bordo. */
    static boolean pendente(ParadaDTO parada, boolean ida) {
        if (parada.tipoParada() == TipoParada.MOTORISTA || parada.listaDependentes() == null) {
            return false;
        }
        boolean embarque = embarque(parada, ida);
        return parada.listaDependentes().stream().anyMatch(d -> "ESPERANDO".equals(d.statusEmbarque())
                || (!embarque && "EMBARCADO".equals(d.statusEmbarque())));
    }

    /** Na ida o embarque é na casa e o desembarque na escola; na volta é o contrário. */
    static boolean embarque(ParadaDTO parada, boolean ida) {
        return (parada.tipoParada() == TipoParada.RESPONSAVEL) == ida;
    }

    /**
     * Índice (em {@code paradas}) da parada que importa para o aluno agora,
     * ou -1 se ele já desembarcou, faltou ou não está na rota.
     */
    static int paradaDoAluno(List<ParadaDTO> paradas, long dependenteId, String status, boolean ida) {
        boolean procuraEmbarque;
        if ("ESPERANDO".equals(status)) procuraEmbarque = true;
        else if ("EMBARCADO".equals(status)) procuraEmbarque = false;
        else return -1;
        for (int i = 0; i < paradas.size(); i++) {
            ParadaDTO p = paradas.get(i);
            if (embarque(p, ida) == procuraEmbarque && p.listaDependentes() != null
                    && p.listaDependentes().stream().anyMatch(d -> d.id() != null && d.id() == dependenteId)) {
                return i;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- montagem da resposta

    private RotaViagemDTO paraMotorista(RotaCalculada rota) {
        List<ParadaRotaDTO> paradas = new ArrayList<>();
        long segundos = 0, metros = 0;
        for (int i = 0; i < rota.paradas().size() && i < rota.trechos().size(); i++) {
            GoogleMapsClient.Trecho t = rota.trechos().get(i);
            segundos += t.segundos();
            metros += t.metros();
            ParadaGeo pg = rota.paradas().get(i);
            paradas.add(new ParadaRotaDTO(pg.parada().id(), pg.parada().ordem(), pg.parada().tipoParada().name(),
                    pg.parada().nomeLocal(), pg.coordenada().latitude(), pg.coordenada().longitude(),
                    segundos, metros, rota.em().plusSeconds(segundos), null, null));
        }
        GoogleMapsClient.Trecho atual = rota.trechos().isEmpty() ? null : rota.trechos().get(0);
        return new RotaViagemDTO(true, null, rota.em(), rota.origem().latitude(), rota.origem().longitude(),
                rota.trechos().stream().map(GoogleMapsClient.Trecho::polyline).filter(p -> p != null).toList(),
                paradas,
                atual == null ? null : atual.primeiraInstrucao(),
                atual == null ? null : atual.metrosPrimeiraInstrucao());
    }

    private RotaViagemDTO paraResponsavel(RotaCalculada rota, Long usuarioId, boolean ida) {
        List<ParadaDTO> paradas = rota.paradas().stream().map(ParadaGeo::parada).toList();
        Map<Long, DependenteParadaDTO> alunos = paradas.stream()
                .flatMap(p -> p.listaDependentes().stream())
                .filter(d -> d.id() != null)
                .collect(Collectors.toMap(DependenteParadaDTO::id, d -> d, (a, b) -> a));
        Set<Long> meus = alunos.isEmpty() ? Set.of()
                : responsavelRepository.findUsuariosAtivosPorDependentes(List.copyOf(alunos.keySet())).stream()
                        .filter(v -> usuarioId.equals(v.getUsuarioId()))
                        .map(DependenteResponsavelUsuarioProjection::getDependenteId)
                        .collect(Collectors.toSet());

        List<ParadaRotaDTO> alvos = new ArrayList<>();
        int ultimo = -1;
        for (Long id : meus) {
            DependenteParadaDTO aluno = alunos.get(id);
            int i = paradaDoAluno(paradas, id, aluno.statusEmbarque(), ida);
            if (i < 0 || i >= rota.trechos().size()) continue;
            long segundos = 0, metros = 0;
            for (int k = 0; k <= i; k++) {
                segundos += rota.trechos().get(k).segundos();
                metros += rota.trechos().get(k).metros();
            }
            ParadaGeo pg = rota.paradas().get(i);
            boolean embarque = embarque(pg.parada(), ida);
            String nome = primeiroNome(aluno.nomeDependente());
            String rotulo = embarque
                    ? (pg.parada().tipoParada() == TipoParada.ESCOLA ? "Busca de " + nome + " na escola" : "Busca de " + nome + " em casa")
                    : (pg.parada().tipoParada() == TipoParada.ESCOLA ? nome + " chega à escola" : nome + " chega em casa");
            alvos.add(new ParadaRotaDTO(null, null, pg.parada().tipoParada().name(), rotulo,
                    pg.coordenada().latitude(), pg.coordenada().longitude(), segundos, metros,
                    rota.em().plusSeconds(segundos), id, embarque ? "EMBARQUE" : "DESEMBARQUE"));
            ultimo = Math.max(ultimo, i);
        }
        if (alvos.isEmpty()) {
            return RotaViagemDTO.indisponivel("Seu aluno não tem mais paradas nesta viagem.");
        }
        alvos.sort((a, b) -> Long.compare(a.segundos(), b.segundos()));
        List<String> polylines = rota.trechos().subList(0, ultimo + 1).stream()
                .map(GoogleMapsClient.Trecho::polyline).filter(p -> p != null).toList();
        return new RotaViagemDTO(true, null, rota.em(), rota.origem().latitude(), rota.origem().longitude(),
                polylines, alvos, null, null);
    }

    // ---------------------------------------------------------------- Google + cache

    private RotaCalculada rotaEmCache(Long viagemDiaId, GoogleMapsClient.Coordenada origem, List<ParadaDTO> pendentes) {
        RotaCalculada atual = cache.get(viagemDiaId);
        List<Long> ids = pendentes.stream().map(ParadaDTO::ordem).map(Integer::longValue).toList();
        if (atual != null && Duration.between(atual.em(), Instant.now()).compareTo(VALIDADE_ROTA) < 0
                && atual.paradas().stream().map(p -> (long) p.parada().ordem()).toList().equals(ids)) {
            return atual;
        }
        List<ParadaGeo> geo = new ArrayList<>();
        for (ParadaDTO p : pendentes) {
            coordenada(p.endereco()).ifPresent(c -> geo.add(new ParadaGeo(p, c)));
        }
        List<GoogleMapsClient.Trecho> trechos = geo.isEmpty() ? List.of()
                : google.calcularRota(origem, geo.stream().map(ParadaGeo::coordenada).toList());
        RotaCalculada nova = new RotaCalculada(Instant.now(), origem, geo, trechos);
        cache.put(viagemDiaId, nova);
        return nova;
    }

    private GoogleMapsClient.Coordenada origem(Long viagemDiaId, boolean motorista, Double latitude, Double longitude,
                                               List<ParadaDTO> todas) {
        if (motorista && latitude != null && longitude != null
                && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180) {
            return new GoogleMapsClient.Coordenada(latitude, longitude);
        }
        var salva = localizacaoAtualRepository.findByViagemDia_Id(viagemDiaId);
        if (salva.isPresent()) {
            return new GoogleMapsClient.Coordenada(salva.get().getLatitude().doubleValue(), salva.get().getLongitude().doubleValue());
        }
        if (motorista) {
            // Sem GPS ainda: parte da casa do motorista (primeira parada da rota).
            return todas.stream().filter(p -> p.tipoParada() == TipoParada.MOTORISTA).findFirst()
                    .flatMap(p -> coordenada(p.endereco())).orElse(null);
        }
        return null;
    }

    /** Coordenada do endereço, consultando o Google só na primeira vez. */
    private Optional<GoogleMapsClient.Coordenada> coordenada(String endereco) {
        if (endereco == null || endereco.isBlank()) return Optional.empty();
        String chave = normalizar(endereco);
        Optional<GeocodificacaoEndereco> salvo = geocodificacaoRepository.findByChave(chave);
        if (salvo.isPresent()) {
            GeocodificacaoEndereco g = salvo.get();
            return g.isEncontrado() ? Optional.of(new GoogleMapsClient.Coordenada(g.getLatitude(), g.getLongitude())) : Optional.empty();
        }
        Optional<GoogleMapsClient.Coordenada> achada = google.geocodificar(endereco + ", Brasil");
        GeocodificacaoEndereco novo = new GeocodificacaoEndereco();
        novo.setChave(chave);
        novo.setEncontrado(achada.isPresent());
        achada.ifPresent(c -> {
            novo.setLatitude(c.latitude());
            novo.setLongitude(c.longitude());
        });
        novo.setAtualizadoEm(Instant.now());
        geocodificacaoRepository.save(novo);
        return achada;
    }

    static String normalizar(String endereco) {
        String s = Normalizer.normalize(endereco.trim().toLowerCase(), Normalizer.Form.NFC).replaceAll("\\s+", " ");
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private static String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) return "o aluno";
        return nome.trim().split("\\s+")[0];
    }
}
