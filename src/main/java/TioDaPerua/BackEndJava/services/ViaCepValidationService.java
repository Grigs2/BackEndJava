package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ViaCepValidationService implements CepValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViaCepValidationService.class);

    private final RestClient restClient;

    public ViaCepValidationService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public StatusValidacaoEndereco validarExistencia(String cep) {
        try {
            ViaCepResponse response = restClient.get()
                    .uri("/ws/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);
            if (response == null || response.erro() == null) {
                LOGGER.warn("ViaCEP retornou resposta incompleta; endereço ficará pendente de validação.");
                return StatusValidacaoEndereco.PENDENTE;
            }
            if (response.erro()) {
                throw new BusinessException("CEP não encontrado no ViaCEP");
            }
            return StatusValidacaoEndereco.VALIDADO;
        } catch (RestClientException exception) {
            LOGGER.warn("ViaCEP indisponível; endereço ficará pendente de validação ({})",
                    exception.getClass().getSimpleName());
            return StatusValidacaoEndereco.PENDENTE;
        }
    }

    private record ViaCepResponse(Boolean erro) {
    }
}
