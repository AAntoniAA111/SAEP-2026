package main.java.dao;
import main.java.util.*;
import main.java.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Registra e consulta as movimentações (entradas e saídas) do estoque. */
public class MovimentacaoDAO {

    /**
     * Registra uma ENTRADA ou SAIDA: atualiza o saldo do produto E grava no histórico,
     * tudo na mesma transação (se uma parte falhar, nada é gravado).
     * Retorna false se não foi possível (produto inexistente ou saldo insuficiente na saída).
     */
    public boolean registrar(int codigoProduto, String tipo, int quantidade,
                             int usuarioId, String observacao) throws SQLException {
        boolean saida = "SAIDA".equals(tipo);
        String sqlSaldo = saida
                ? "UPDATE produtos SET quantidade = quantidade - ? WHERE codigo = ? AND ativo = 1 AND quantidade >= ?"
                : "UPDATE produtos SET quantidade = quantidade + ? WHERE codigo = ? AND ativo = 1";
        String sqlHistorico = "INSERT INTO movimentacoes (produto_id, usuario_id, tipo, quantidade, observacao) "
                            + "SELECT id, ?, ?, ?, ? FROM produtos WHERE codigo = ?";

        try (Connection con = Conexao.obter()) {
            con.setAutoCommit(false);
            try {
                int linhas;
                try (PreparedStatement ps = con.prepareStatement(sqlSaldo)) {
                    ps.setInt(1, quantidade);
                    ps.setInt(2, codigoProduto);
                    if (saida) {
                        ps.setInt(3, quantidade); // garante que o estoque nunca fica negativo
                    }
                    linhas = ps.executeUpdate();
                }
                if (linhas == 0) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(sqlHistorico)) {
                    ps.setInt(1, usuarioId);
                    ps.setString(2, tipo);
                    ps.setInt(3, quantidade);
                    ps.setString(4, observacao);
                    ps.setInt(5, codigoProduto);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Histórico das últimas 300 movimentações, da mais recente para a mais antiga. */
    public List<Movimentacao> listarHistorico(String filtro) throws SQLException {
        String sql = "SELECT m.data_hora, p.codigo, p.nome AS produto, m.tipo, m.quantidade, "
                   + "u.nome AS responsavel, m.observacao "
                   + "FROM movimentacoes m "
                   + "JOIN produtos p ON p.id = m.produto_id "
                   + "JOIN usuarios u ON u.id = m.usuario_id "
                   + "WHERE p.nome LIKE ? OR CAST(p.codigo AS CHAR) LIKE ? OR u.nome LIKE ? "
                   + "ORDER BY m.data_hora DESC, m.id DESC LIMIT 300";
        String padrao = "%" + (filtro == null ? "" : filtro.trim()) + "%";
        List<Movimentacao> lista = new ArrayList<>();
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, padrao);
            ps.setString(2, padrao);
            ps.setString(3, padrao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Movimentacao(
                            rs.getTimestamp("data_hora").toLocalDateTime(),
                            rs.getInt("codigo"), rs.getString("produto"), rs.getString("tipo"),
                            rs.getInt("quantidade"), rs.getString("responsavel"),
                            rs.getString("observacao")));
                }
            }
        }
        return lista;
    }
}
