package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ResponsavelService {

    @Autowired
    private ResponsavelRepository responsavelRepository;

    public Responsavel cadastrarResponsavel(Responsavel Responsavel){
        if(Responsavel == null)return null;

        // Se já existir este responsavel no sistema, ele checará a lista de dependentes, caso aja um dependente
        // Que não está na lista ja cadastrada, ele será adicionado e salvo no banco de dados com as alterações
        if(Responsavel.getId()!=null) {
            Optional<Responsavel> responsavel = responsavelRepository.findById(Responsavel.getId());
            if(responsavel.isPresent()){
                Responsavel responsavelExistente = responsavel.get();
                if(responsavelExistente.getDependentes() != null){
                    for (Dependente d : Responsavel.getDependentes()){
                        if(!responsavelExistente.getDependentes().contains(d)){
                            responsavelExistente.getDependentes().add(d);
                        } // dependente ja existente
                        else { return null;}
                    }
                }
                return responsavelRepository.save(responsavelExistente);
            }
        }
        return responsavelRepository.save(Responsavel);
    }

    public Responsavel cadastrarDependente(Long id, Dependente Dependente){
        if(Dependente == null)return null;
        if(id == null)return null;
        Optional<Responsavel> responsavel = responsavelRepository.findById(id);
            if(responsavel.isPresent()){
                Responsavel responsavelExistente = responsavel.get();
                if(responsavelExistente.getDependentes() != null){
                    if (!responsavelExistente.getDependentes().contains(Dependente)) {
                        responsavelExistente.getDependentes().add(Dependente);
                    } else {return null;}//Dependente já cadastrado
                } else {// se a lista de dependentes for nulla
                    responsavelExistente.getDependentes().add(Dependente);
                }

                return responsavelRepository.save(responsavelExistente);
            } //Responsavel não presente no banco de dados
        return null;
    }
}
