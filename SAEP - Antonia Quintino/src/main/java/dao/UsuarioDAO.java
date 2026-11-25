package main.java.dao;
import main.java.model.*;
import main.java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Cuida do login. A senha NUNCA fica salva em texto puro: só o hash SHA-256 dela. */
public class UsuarioDAO {

    /** Devolve o usuário se login e senha estiverem certos; senão devolve null. */
    public Usuario autenticar(String login, String senha) throws SQLException {
        String sql = "SELECT id, nome, login, perfil FROM usuarios WHERE login = ? AND senha_hash = ?";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, hash(senha));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(rs.getInt("id"), rs.getString("nome"),
                            rs.getString("login"), rs.getString("perfil"));
                }
                return null;
            }
        }
    }

    /** Transforma a senha em um código SHA-256 de 64 letras/números (hexadecimal). */
    public static String hash(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
