package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.validation.SenhaPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class MotoristaService {

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TermosService termosService;

    @Autowired
    private CadastroUsuarioValidator cadastroUsuarioValidator;

    @Autowired
    private DocumentoMotoristaService documentoMotoristaService;

    @Autowired
    private EnderecoOperacionalService enderecoOperacionalService;

    /** Lazy para não criar ciclo de dependência na inicialização (usado só para avisar troca de veículo). */
    @Autowired
    @org.springframework.context.annotation.Lazy
    private ViagemService viagemService;

    public Motorista cadastraMotorista(Motorista motorista) {
        if (motorista == null) throw new IllegalArgumentException("Motorista não pode ser nulo");
        if (motorista.getNome() == null || motorista.getNome().isBlank()) {
            throw new BusinessException("Nome do motorista é obrigatório");
        }
        if (motorista.getId() != null || motorista.getUsuario() == null
                || motorista.getUsuario().getId() != null
                || motorista.getUsuario().getSenha() == null
                || motorista.getUsuario().getSenha().isBlank()) {
            throw new IllegalArgumentException("Cadastro deve conter credenciais novas e válidas");
        }
        validarIdadeMinima(motorista.getDataNascimento());
        motorista.setCpf(validarCpf(motorista.getCpf()));
        cadastroUsuarioValidator.validarNovoCadastro(motorista.getUsuario());
        SenhaPolicy.exigirValida(motorista.getUsuario().getSenha());
        termosService.registrarAceite(motorista.getUsuario(),
                motorista.getUsuario().getAceitouTermos());
        motorista.getUsuario().setSenha(passwordEncoder.encode(motorista.getUsuario().getSenha()));
        if (motorista.getUsuario().getEnderecoOperacional() == null) {
            throw new BusinessException("Endereço operacional estruturado é obrigatório no cadastro");
        }
        motorista.getUsuario().setEnderecoOperacional(
                enderecoOperacionalService.validar(motorista.getUsuario().getEnderecoOperacional()));
        return motoristaRepository.save(motorista);
    }

    @Transactional
    public Motorista cadastrarVeiculo(long id_motorista, Veiculo veiculo) {
        if (veiculo == null) throw new IllegalArgumentException("Veículo não pode ser nulo");
        if (veiculo.getId() != null) {
            throw new IllegalArgumentException("O cadastro de veículo não pode reutilizar o ID de um veículo existente");
        }
        Motorista motoristaExistente = motoristaRepository.findById(id_motorista)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + id_motorista));
        validarVeiculo(veiculo, motoristaExistente.getId(), false);
        veiculo.setPlaca(DocumentoFiscalValidator.normalizarPlaca(veiculo.getPlaca()));

        motoristaExistente.setVeiculo(veiculo);
        return motoristaRepository.save(motoristaExistente);
    }

    @Transactional
    public Motorista alterarVeiculo(Long idMotorista, Veiculo veiculo) {
        if (idMotorista == null || veiculo == null) {
            throw new IllegalArgumentException("Motorista e veículo são obrigatórios");
        }
        Motorista motorista = motoristaRepository.findById(idMotorista)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + idMotorista));
        if (motorista.getVeiculo() == null) {
            throw new ResourceNotFoundException("Motorista ainda não possui veículo cadastrado");
        }
        if (veiculo.getId() != null && !veiculo.getId().equals(motorista.getVeiculo().getId())) {
            throw new IllegalArgumentException("Não é permitido substituir o veículo por ID enviado pelo cliente");
        }
        validarVeiculo(veiculo, motorista.getId(), true);
        Veiculo existente = motorista.getVeiculo();
        boolean dadosAlterados = !Objects.equals(existente.getModelo(), veiculo.getModelo())
                || !Objects.equals(existente.getPlaca(),
                DocumentoFiscalValidator.normalizarPlaca(veiculo.getPlaca()))
                || existente.getAno() != veiculo.getAno()
                || existente.getCapacidade() != veiculo.getCapacidade()
                || !Objects.equals(existente.getCor(), veiculo.getCor());
        if (dadosAlterados) {
            documentoMotoristaService.invalidarDocumentosDoVeiculo(existente.getId());
        }
        // Mudou o que o responsável usa para reconhecer a van (modelo, placa ou cor)?
        boolean identificacaoAlterada = !Objects.equals(existente.getModelo(), veiculo.getModelo())
                || !Objects.equals(existente.getPlaca(), DocumentoFiscalValidator.normalizarPlaca(veiculo.getPlaca()))
                || !Objects.equals(existente.getCor(), veiculo.getCor());
        existente.setModelo(veiculo.getModelo());
        existente.setPlaca(DocumentoFiscalValidator.normalizarPlaca(veiculo.getPlaca()));
        existente.setAno(veiculo.getAno());
        existente.setCapacidade(veiculo.getCapacidade());
        existente.setCor(veiculo.getCor());
        Motorista salvo = motoristaRepository.save(motorista);
        if (identificacaoAlterada && viagemService != null) {
            // Avisa os responsáveis das viagens de hoje (planejadas ou em andamento).
            viagemService.notificarTrocaDeVeiculo(salvo);
        }
        return salvo;
    }

    public Motorista alterarInfos(Motorista motorista) {
        if (motorista == null || motorista.getId() == null) throw new IllegalArgumentException("ID do motorista é obrigatório");
        EnderecoOperacional enderecoAtualizado = motorista.getUsuario() == null
                ? null : motorista.getUsuario().getEnderecoOperacional();
        if (enderecoAtualizado != null) {
            enderecoAtualizado = enderecoOperacionalService.validar(enderecoAtualizado);
        }
        if (motorista.getDataNascimento() != null) {
            validarIdadeMinima(motorista.getDataNascimento());
        }
        
        Motorista existente = motoristaRepository.findByIdComEnderecoOperacional(motorista.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + motorista.getId()));
        
        // Atualizar campos editáveis do motorista
        existente.setNome(motorista.getNome());
        if (motorista.getDataNascimento() != null) {
            existente.setDataNascimento(motorista.getDataNascimento());
        }
        if (motorista.getCpf() != null) {
            String cpf = DocumentoFiscalValidator.normalizarCpf(motorista.getCpf());
            if (!DocumentoFiscalValidator.cpfValido(cpf)) {
                throw new BusinessException("CPF inválido");
            }
            if (motoristaRepository.existsByCpfAndIdNot(cpf, existente.getId())) {
                throw new BusinessException("CPF já cadastrado");
            }
            existente.setCpf(cpf);
        }
        if (motorista.getCnh() != null && !motorista.getCnh().isBlank()
                && !Objects.equals(motorista.getCnh(), existente.getCnh())) {
            throw new BusinessException("Atualize a CNH pelo envio de documento para revisão do administrador");
        }
        
        if (motorista.getUsuario() != null && existente.getUsuario() != null) {
            Usuario uExistente = existente.getUsuario();
            Usuario uNovo = motorista.getUsuario();
            cadastroUsuarioValidator.validarAtualizacao(uNovo, uExistente.getId());
            uExistente.setEmail(uNovo.getEmail());
            uExistente.setEndereco(uNovo.getEndereco());
            uExistente.setTelefone(uNovo.getTelefone());
            if (enderecoAtualizado != null) {
                enderecoOperacionalService.atualizarUsuario(uExistente, enderecoAtualizado);
            }
        }

        return motoristaRepository.save(existente);
    }

    private void validarIdadeMinima(LocalDate dataNascimento) {
        if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now().minusYears(18))) {
            throw new BusinessException("O motorista deve ter pelo menos 18 anos completos");
        }
    }

    private String validarCpf(String cpfInformado) {
        String cpf = DocumentoFiscalValidator.normalizarCpf(cpfInformado);
        if (!DocumentoFiscalValidator.cpfValido(cpf)) {
            throw new BusinessException("CPF inválido");
        }
        if (motoristaRepository.existsByCpf(cpf)) {
            throw new BusinessException("CPF já cadastrado");
        }
        return cpf;
    }

    private void validarVeiculo(Veiculo veiculo, Long motoristaId, boolean permitirPlacaAtual) {
        String placa = DocumentoFiscalValidator.normalizarPlaca(veiculo.getPlaca());
        if (!DocumentoFiscalValidator.placaValida(placa)) {
            throw new BusinessException("Placa inválida");
        }
        boolean placaDuplicada = permitirPlacaAtual
                ? motoristaRepository.existsByVeiculo_PlacaAndIdNot(placa, motoristaId)
                : motoristaRepository.existsByVeiculo_Placa(placa);
        if (placaDuplicada) {
            throw new BusinessException("Placa já cadastrada");
        }
        if (veiculo.getModelo() == null || veiculo.getModelo().isBlank()
                || veiculo.getCapacidade() <= 0 || veiculo.getAno() < 1900
                || veiculo.getAno() > LocalDate.now().getYear() + 1) {
            throw new BusinessException("Modelo, capacidade ou ano do veículo são inválidos");
        }
    }
}
