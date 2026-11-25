package main.java.resources;

import main.java.dao.*;
import main.java.model.*;
import main.java.util.*;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * ENTREGA 7 - Interface de gestão de estoque.
 * Registra entradas e saídas (com o responsável e a data gravados automaticamente),
 * mostra a posição atual do estoque e o histórico completo de movimentações.
 */
public class TelaEstoque extends JDialog {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Usuario usuario;
    private final ProdutoDAO produtoDao = new ProdutoDAO();
    private final MovimentacaoDAO movDao = new MovimentacaoDAO();

    // Formulário de movimentação
    private final JTextField txtCodigo = new JTextField(10);
    private final JComboBox<String> comboTipo = new JComboBox<>(new String[] {
            "Entrada - recebimento de nota fiscal", "Saída - venda ao consumidor final"});
    private final JTextField txtQtd = new JTextField(10);
    private final JTextField txtObs = new JTextField(25);
    private final JLabel lblInfo = new JLabel(" ");

    // Posição do estoque
    private final javax.swing.table.DefaultTableModel modeloPosicao = Ui.modeloSomenteLeitura(
            "Código", "Produto", "Estoque", "Mínimo", "Situação");
    private final JTable tabelaPosicao = new JTable(modeloPosicao);

    // Histórico
    private final javax.swing.table.DefaultTableModel modeloHistorico = Ui.modeloSomenteLeitura(
            "Data/hora", "Código", "Produto", "Tipo", "Qtd.", "Responsável", "Observação");
    private final JTable tabelaHistorico = new JTable(modeloHistorico);
    private final JTextField txtBuscaHistorico = new JTextField(25);

    public TelaEstoque(Frame dono, Usuario usuario) {
        super(dono, "Gestão de estoque", true);
        this.usuario = usuario;
        setSize(1000, 700);
        setLocationRelativeTo(dono);
        setLayout(new BorderLayout());

        add(Ui.titulo("GESTÃO DE ESTOQUE"), BorderLayout.NORTH);

        JPanel corpo = new JPanel(new BorderLayout(10, 10));
        corpo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        corpo.add(criarFormulario(), BorderLayout.NORTH);

        JTabbedPane abas = new JTabbedPane();
        abas.setFont(abas.getFont().deriveFont(Font.BOLD, 13f));
        abas.addTab("Posição do estoque", criarAbaPosicao());
        abas.addTab("Histórico de movimentações", criarAbaHistorico());
        corpo.add(abas, BorderLayout.CENTER);
        add(corpo, BorderLayout.CENTER);

        carregarTudo();
    }

    // ------------------------------------------------------------ formulário

    private JPanel criarFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                "Registrar movimentação (responsável: " + usuario.getNome() + ")"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridy = 0; g.gridx = 0; p.add(new JLabel("Código do produto:"), g);
        g.gridx = 1;              p.add(txtCodigo, g);
        g.gridx = 2;              p.add(new JLabel("Tipo:"), g);
        g.gridx = 3;              p.add(comboTipo, g);

        g.gridy = 1; g.gridx = 0; p.add(new JLabel("Quantidade:"), g);
        g.gridx = 1;              p.add(txtQtd, g);
        g.gridx = 2;              p.add(new JLabel("Observação:"), g);
        g.gridx = 3;              p.add(txtObs, g);

        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD));
        g.gridy = 2; g.gridx = 0; g.gridwidth = 3;
        p.add(lblInfo, g);

        JButton registrar = Ui.botao("Registrar movimentação");
        registrar.addActionListener(e -> registrar());
        g.gridx = 3; g.gridwidth = 1;
        p.add(registrar, g);

        // Mostra o nome e o estoque do produto enquanto o código é digitado
        Ui.aoDigitar(txtCodigo, this::atualizarInfoProduto);
        return p;
    }

    private void atualizarInfoProduto() {
        try {
            int codigo = Integer.parseInt(txtCodigo.getText().trim());
            Produto p = produtoDao.buscarPorCodigo(codigo);
            lblInfo.setText(p == null ? "Código não encontrado."
                    : p.getNome() + "  |  Estoque atual: " + p.getQuantidade()
                    + "  |  Mínimo: " + p.getEstoqueMinimo());
        } catch (NumberFormatException ex) {
            lblInfo.setText(" ");
        } catch (SQLException ex) {
            lblInfo.setText("Erro ao consultar o banco de dados.");
        }
    }

    private void registrar() {
        try {
            int codigo = Integer.parseInt(txtCodigo.getText().trim());
            int qtd = Integer.parseInt(txtQtd.getText().trim());
            if (qtd <= 0) {
                Ui.erro(this, "Digite uma quantidade maior que zero.");
                return;
            }
            Produto antes = produtoDao.buscarPorCodigo(codigo);
            if (antes == null) {
                Ui.erro(this, "Código não encontrado.");
                return;
            }

            boolean entrada = comboTipo.getSelectedIndex() == 0;
            String obs = txtObs.getText().trim();
            boolean ok = movDao.registrar(codigo, entrada ? "ENTRADA" : "SAIDA", qtd,
                    usuario.getId(), obs.isEmpty() ? null : obs);

            if (!ok) {
                Ui.erro(this, "Impossível retirar essa quantidade.\nEstoque atual de \""
                        + antes.getNome() + "\": " + antes.getQuantidade());
                return;
            }

            Produto depois = produtoDao.buscarPorCodigo(codigo);
            JOptionPane.showMessageDialog(this,
                    (entrada ? "Quantidade acrescentada!" : "Quantidade retirada!")
                            + "\nNovo estoque de \"" + depois.getNome() + "\": " + depois.getQuantidade(),
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            txtCodigo.setText("");
            txtQtd.setText("");
            txtObs.setText("");
            carregarTudo();

            // ALERTA AUTOMÁTICO após cada movimentação
            if (depois.estoqueBaixo()) {
                Ui.alertaEstoque(this, depois);
            }
        } catch (NumberFormatException ex) {
            Ui.erro(this, "Digite apenas números inteiros no código e na quantidade.");
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }

    // ------------------------------------------------------------ abas

    private JPanel criarAbaPosicao() {
        Ui.pintarLinhasBaixas(tabelaPosicao, modeloPosicao, 4);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JScrollPane(tabelaPosicao), BorderLayout.CENTER);
        return p;
    }

    private JPanel criarAbaHistorico() {
        tabelaHistorico.setRowHeight(26);
        tabelaHistorico.setFont(tabelaHistorico.getFont().deriveFont(13f));
        tabelaHistorico.getTableHeader().setFont(tabelaHistorico.getFont().deriveFont(Font.BOLD, 13f));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel("Filtrar por produto, código ou responsável:"));
        barra.add(txtBuscaHistorico);
        JButton limpar = new JButton("Limpar");
        limpar.addActionListener(e -> txtBuscaHistorico.setText(""));
        barra.add(limpar);
        Ui.aoDigitar(txtBuscaHistorico, this::carregarHistorico);

        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(barra, BorderLayout.NORTH);
        p.add(new JScrollPane(tabelaHistorico), BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------ carga de dados

    private void carregarTudo() {
        carregarPosicao();
        carregarHistorico();
    }

    private void carregarPosicao() {
        try {
            modeloPosicao.setRowCount(0);
            for (Produto p : produtoDao.listar()) {
                modeloPosicao.addRow(new Object[] {
                        p.getCodigo(), p.getNome(), p.getQuantidade(), p.getEstoqueMinimo(),
                        p.estoqueBaixo() ? "ESTOQUE BAIXO" : "OK"
                });
            }
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }

    private void carregarHistorico() {
        try {
            modeloHistorico.setRowCount(0);
            for (Movimentacao m : movDao.listarHistorico(txtBuscaHistorico.getText())) {
                modeloHistorico.addRow(new Object[] {
                        m.getDataHora().format(FORMATO_DATA), m.getCodigoProduto(), m.getNomeProduto(),
                        "ENTRADA".equals(m.getTipo()) ? "Entrada" : "Saída",
                        m.getQuantidade(), m.getResponsavel(), m.getObservacao()
                });
            }
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }
}
