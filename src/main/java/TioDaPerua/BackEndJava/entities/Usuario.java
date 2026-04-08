package TioDaPerua.BackEndJava.entities;


import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
@Entity
@Table(name = "usuario", schema = "public")
public class Usuario implements Serializable {

    @Serial
    private static final Long serialVersionUID = 1123123123123L;

    @Id
    @SequenceGenerator(name="SEQ_usuario", sequenceName = "public.seq_usuario", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Usuario")
    private Long id;

    @Column(name ="email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha", nullable = false)
    private String senha;

    @Column(name = "endereco", nullable = false)
    private String endereco;

    @Column(name = "telefone", nullable = false)
    private String telefone;

    @Column(name = "tipoPerfil", nullable = false)
    private String tipoPerfil;



    public Usuario(String email, String senha, String endereco, String telefone, String tipoPerfil) {
        this.email = email;
        this.senha = senha;
        this.endereco = endereco;
        this.telefone = telefone;
        this.tipoPerfil = tipoPerfil;

    }

    public Usuario(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
        this.endereco = usuario.getEndereco();
        this.telefone = usuario.getTelefone();
        this.tipoPerfil = usuario.getTipoPerfil();

    }

    public Usuario() {}

}
