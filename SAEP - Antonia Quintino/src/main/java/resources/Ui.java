package main.java.resources;
import main.java.model.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/** Cores, botões e mensagens usados por todas as telas (evita repetir código). */
public final class Ui {

    public static final Color AZUL = new Color(25, 85, 160);
    public static final Color VERDE_CLARO = new Color(220, 245, 225);
    public static final Color VERDE_ESCURO = new Color(0, 100, 30);
    public static final Color VERMELHO_CLARO = new Color(255, 220, 220);
    public static final Color VERMELHO_ESCURO = new Color(160, 0, 0);
    public static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private Ui() { }

    public static JButton botao(String texto) {
        JButton b = new JButton(texto);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setBackground(AZUL);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(210, 38));
        return b;
    }

    public static JLabel titulo(String texto) {
        JLabel t = new JLabel(texto, SwingConstants.CENTER);
        t.setOpaque(true);
        t.setBackground(AZUL);
        t.setForeground(Color.WHITE);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));
        t.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        return t;
    }

    public static void erro(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Atenção", JOptionPane.ERROR_MESSAGE);
    }

    public static void erroBanco(Component pai, SQLException ex) {
        JOptionPane.showMessageDialog(pai,
                "Erro ao acessar o banco de dados:\n" + ex.getMessage()
                        + "\n\nConfira usuário/senha em Conexao.java e se o MySQL está ligado.",
                "Erro de banco de dados", JOptionPane.ERROR_MESSAGE);
    }

    /** Pop-up de alerta para um produto que chegou ao estoque mínimo. */
    public static void alertaEstoque(Component pai, Produto p) {
        JOptionPane.showMessageDialog(pai,
                "ATENÇÃO! ESTOQUE NO MÍNIMO!\n\n"
                        + "Produto: " + p.getNome() + " (código " + p.getCodigo() + ")\n"
                        + "Estoque atual: " + p.getQuantidade() + "\n"
                        + "Estoque mínimo: " + p.getEstoqueMinimo() + "\n\n"
                        + "Providencie a reposição.",
                "Alerta de estoque", JOptionPane.WARNING_MESSAGE);
    }

    /** Executa uma ação toda vez que o texto do campo muda (usado nas buscas). */
    public static void aoDigitar(JTextField campo, Runnable acao) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { acao.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { acao.run(); }
            @Override public void changedUpdate(DocumentEvent e) { acao.run(); }
        });
    }

    /** Pinta de vermelho as linhas cuja coluna "Situação" vale ESTOQUE BAIXO. */
    public static void pintarLinhasBaixas(JTable tabela, DefaultTableModel modelo, int colunaSituacao) {
        tabela.setRowHeight(26);
        tabela.setFont(tabela.getFont().deriveFont(13f));
        tabela.getTableHeader().setFont(tabela.getFont().deriveFont(Font.BOLD, 13f));
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor,
                    boolean selecionada, boolean foco, int linha, int coluna) {
                Component c = super.getTableCellRendererComponent(t, valor, selecionada, foco, linha, coluna);
                boolean baixo = "ESTOQUE BAIXO".equals(modelo.getValueAt(linha, colunaSituacao));
                if (!selecionada) {
                    c.setBackground(baixo ? VERMELHO_CLARO : Color.WHITE);
                    c.setForeground(baixo ? VERMELHO_ESCURO : Color.BLACK);
                }
                return c;
            }
        });
    }

    /** Modelo de tabela que não deixa editar as células. */
    public static DefaultTableModel modeloSomenteLeitura(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
        };
    }
}
