package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository ur;
    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario findByID (Long Id){
        return usuarioRepository.findById(Id).orElse(null);
    }

    public Usuario findByEmail (String email){
        if (email == null && email.isEmpty()) return null;
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public Usuario cadastrarUsuario (Usuario usuario){
        if (usuario != null)return usuarioRepository.save(usuario);
        else return null;
    }

    public Boolean autenticar(String email, String senha){
        if (email == null && email.isEmpty() && senha == null && senha.isEmpty()) return false;
        Usuario usuario = new Usuario(findByEmail(email));

        if (usuario == null) return false;
        if (senha.equals(usuario.getSenha())) return true;
        else return false;
    }
}
