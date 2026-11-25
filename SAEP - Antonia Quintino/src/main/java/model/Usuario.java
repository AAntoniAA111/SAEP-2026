package main.java.model;

/** Usuário que fez login no sistema (o "responsável" pelas movimentações). */
public class Usuario {

    private final int id;
    private final String nome;
    private final String login;
    private final String perfil; // ADMINISTRADOR ou OPERADOR

    public Usuario(int id, String nome, String login, String perfil) {
        this.id = id;
        this.nome = nome;
        this.login = login;
        this.perfil = perfil;
    }

    public int getId()        { return id; }
    public String getNome()   { return nome; }
    public String getLogin()  { return login; }
    public String getPerfil() { return perfil; }

    public boolean isAdministrador() {
        return "ADMINISTRADOR".equals(perfil);
    }
}
