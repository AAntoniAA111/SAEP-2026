package main.java.dao;
import main.java.model.Produto;

import main.java.util.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO = "Data Access Object". É a única classe que conversa com o banco.
 * A tela só chama os métodos daqui, sem escrever SQL.
 */
public class ProdutoDAO {

    private static final String COLUNAS =
            "codigo, nome, quantidade, valor, estoque_minimo, entradas, saidas";

    // CADASTRAR
    public void inserir(Produto p) throws SQLException {
        String sql = "INSERT INTO produtos (codigo, nome, quantidade, valor, estoque_minimo) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getCodigo());
            ps.setString(2, p.getNome());
            ps.setInt(3, p.getQuantidade());
            ps.setDouble(4, p.getValor());
            ps.setInt(5, p.getEstoqueMinimo());
            ps.executeUpdate();
        }
    }

    // LISTAR
    public List<Produto> listar() throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM produtos WHERE ativo = 1 ORDER BY nome";
        List<Produto> lista = new ArrayList<>();
        try (Connection con = Conexao.obter();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // LISTAR COM FILTRO: procura o texto no nome OU no código do produto
    public List<Produto> listar(String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            return listar();
        }
        String sql = "SELECT " + COLUNAS + " FROM produtos "
                + "WHERE ativo = 1 AND (nome LIKE ? OR CAST(codigo AS CHAR) LIKE ?) ORDER BY nome";
        String padrao = "%" + filtro.trim() + "%"; // % = "qualquer coisa antes/depois"
        List<Produto> lista = new ArrayList<>();
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, padrao);
            ps.setString(2, padrao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    // EDITAR: muda nome, valor e estoque mínimo (a quantidade só muda por entrada/saída)
    public void atualizar(Produto p) throws SQLException {
        String sql = "UPDATE produtos SET nome = ?, valor = ?, estoque_minimo = ? "
                + "WHERE codigo = ? AND ativo = 1";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setDouble(2, p.getValor());
            ps.setInt(3, p.getEstoqueMinimo());
            ps.setInt(4, p.getCodigo());
            ps.executeUpdate();
        }
    }

    // EXCLUIR
    public void excluir(int codigo) throws SQLException {
        inativar(codigo);
    }

    // INATIVAR: mantém o produto no banco para preservar o histórico de movimentações
    public void inativar(int codigo) throws SQLException {
        String sql = "UPDATE produtos SET ativo = 0 WHERE codigo = ? AND ativo = 1";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            ps.executeUpdate();
        }
    }

    // BUSCAR POR CÓDIGO (devolve null se não existir)
    public Produto buscarPorCodigo(int codigo) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM produtos WHERE codigo = ? AND ativo = 1";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // ENTRADA (recebimento de nota fiscal)
    public void registrarEntrada(int codigo, int quantidade) throws SQLException {
        String sql = "UPDATE produtos SET quantidade = quantidade + ?, entradas = entradas + 1 "
                + "WHERE codigo = ? AND ativo = 1";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setInt(2, codigo);
            ps.executeUpdate();
        }
    }

    // SAÍDA (venda). Retorna false se não há estoque suficiente.
    public boolean registrarSaida(int codigo, int quantidade) throws SQLException {
        // O "AND quantidade >= ?" garante que o estoque nunca fica negativo
        String sql = "UPDATE produtos SET quantidade = quantidade - ?, saidas = saidas + 1 "
                + "WHERE codigo = ? AND ativo = 1 AND quantidade >= ?";
        try (Connection con = Conexao.obter();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setInt(2, codigo);
            ps.setInt(3, quantidade);
            return ps.executeUpdate() > 0;
        }
    }

    // PRODUTOS COM ESTOQUE NO MÍNIMO (base do alerta automático)
    public List<Produto> listarEstoqueBaixo() throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM produtos "
                + "WHERE ativo = 1 AND quantidade <= estoque_minimo ORDER BY quantidade";
        List<Produto> lista = new ArrayList<>();
        try (Connection con = Conexao.obter();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // VALOR TOTAL DO ESTOQUE: retorna {quantidade total de itens, valor total em R$}
    public double[] totais() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantidade), 0), COALESCE(SUM(quantidade * valor), 0) "
                + "FROM produtos WHERE ativo = 1";
        try (Connection con = Conexao.obter();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return new double[] { rs.getDouble(1), rs.getDouble(2) };
        }
    }

    // MAIOR QUANTIDADE
    public Produto maiorEstoque() throws SQLException {
        return primeiroDaConsulta("SELECT " + COLUNAS + " FROM produtos "
                + "WHERE ativo = 1 ORDER BY quantidade DESC LIMIT 1");
    }

    // MENOR QUANTIDADE
    public Produto menorEstoque() throws SQLException {
        return primeiroDaConsulta("SELECT " + COLUNAS + " FROM produtos "
                + "WHERE ativo = 1 ORDER BY quantidade ASC LIMIT 1");
    }

    private Produto primeiroDaConsulta(String sql) throws SQLException {
        try (Connection con = Conexao.obter();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? mapear(rs) : null;
        }
    }

    // Converte uma linha do banco em um objeto Produto
    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getInt("codigo"),
                rs.getString("nome"),
                rs.getInt("quantidade"),
                rs.getDouble("valor"),
                rs.getInt("estoque_minimo"),
                rs.getInt("entradas"),
                rs.getInt("saidas"));
    }
}