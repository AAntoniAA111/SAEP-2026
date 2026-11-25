package main.java.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cuida da conexão com o MySQL.
 * >>> ALTERE o usuário e a senha abaixo para os do SEU MySQL. <<<
 */
public class Conexao {

    private static final String URL =
            "jdbc:mysql://localhost:3306/saep_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo";
    private static final String USUARIO = "EBB2N";
    private static final String SENHA = "";

    private Conexao() { }

    public static Connection obter() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }
}
