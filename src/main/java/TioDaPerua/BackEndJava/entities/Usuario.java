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
    private static final long serialVersionUID = 1123123123123L;

    @Id
    @SequenceGenerator(name="SEQ_usuario", sequenceName = "public.seq_usuario", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Usuario")
    private long id;

    @Column(name ="email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha", nullable = false)
    private String senha;

    public Usuario(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }

    public Usuario(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
    }

    public Usuario() {}

}
