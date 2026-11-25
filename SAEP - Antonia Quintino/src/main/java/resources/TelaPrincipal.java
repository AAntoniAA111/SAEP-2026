package main.java.resources;

import main.java.dao.ProdutoDAO;
import main.java.model.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Tela única do sistema, com 4 abas:
 *  Cadastrar | Produtos | Movimentar estoque | Resumo
 * No topo há uma faixa que fica VERMELHA quando algum produto está no estoque mínimo.
 */
public class TelaPrincipal extends JFrame {

    private static final Color AZUL = new Color(25, 85, 160);
    private static final Color VERDE_CLARO = new Color(220, 245, 225);
    private static final Color VERMELHO_CLARO = new Color(255, 220, 220);
    private static final Color VERMELHO_ESCURO = new Color(160, 0, 0);

    private final ProdutoDAO dao = new ProdutoDAO();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    // Faixa de alerta no topo
    private final JLabel faixaAlerta = new JLabel(" ", SwingConstants.CENTER);

    // Aba "Cadastrar"
    private final JTextField txtNome = new JTextField(20);
    private final JTextField txtCodigo = new JTextField(20);
    private final JTextField txtQtd = new JTextField(20);
    private final JTextField txtValor = new JTextField(20);
    private final JTextField txtMinimo = new JTextField("5", 20);

    // Aba "Produtos"
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new String[] {"Código", "Produto", "Estoque", "Mínimo", "Valor unit.", "Entradas", "Saídas", "Situação"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false; // tabela só para leitura
        }
    };
    private final JTable tabela = new JTable(modeloTabela);
    private final JTextField txtBusca = new JTextField(25);

    // Aba "Movimentar estoque"
    private final JTextField txtCodigoMov = new JTextField(20);
    private final JComboBox<String> comboTipo = new JComboBox<>(new String[] {
            "Entrada - recebimento de nota fiscal", "Saída - venda ao consumidor final"});
    private final JTextField txtQtdMov = new JTextField(20);

    // Aba "Resumo"
    private final JTextArea txtResumo = new JTextArea();

    private final JTabbedPane abas = new JTabbedPane();

    public TelaPrincipal() {
        super("BiT Power - Sistema de Estoque");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(criarCabecalho(), BorderLayout.NORTH);

        abas.setFont(abas.getFont().deriveFont(Font.BOLD, 14f));
        abas.addTab("Cadastrar", criarAbaCadastro());
        abas.addTab("Produtos", criarAbaProdutos());
        abas.addTab("Movimentar estoque", criarAbaMovimentacao());
        abas.addTab("Resumo", criarAbaResumo());
        // Sempre que o usuário trocar de aba, os dados são recarregados do banco
        abas.addChangeListener(e -> atualizarTudo());
        add(abas, BorderLayout.CENTER);

        // ALERTA AUTOMÁTICO: a cada 30 segundos confere o banco e atualiza a faixa do topo
        new javax.swing.Timer(30_000, e -> atualizarFaixaAlerta()).start();

        atualizarTudo();

        // Ao abrir o sistema, avisa se já existe produto no mínimo
        SwingUtilities.invokeLater(this::alertaInicial);
    }

    // ---------------------------------------------------------------- CABEÇALHO

    private JPanel criarCabecalho() {
        JPanel painel = new JPanel(new BorderLayout());

        JLabel titulo = new JLabel("SISTEMA DE ESTOQUE - BiT Power", SwingConstants.CENTER);
        titulo.setOpaque(true);
        titulo.setBackground(AZUL);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

        faixaAlerta.setOpaque(true);
        faixaAlerta.setFont(faixaAlerta.getFont().deriveFont(Font.BOLD, 13f));
        faixaAlerta.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        painel.add(titulo, BorderLayout.NORTH);
        painel.add(faixaAlerta, BorderLayout.SOUTH);
        return painel;
    }

    // ---------------------------------------------------------------- ABA CADASTRAR

    private JPanel criarAbaCadastro() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        adicionarCampo(form, g, 0, "Nome do produto:", txtNome);
        adicionarCampo(form, g, 1, "Código (somente números inteiros):", txtCodigo);
        adicionarCampo(form, g, 2, "Quantidade em estoque:", txtQtd);
        adicionarCampo(form, g, 3, "Valor unitário (R$):", txtValor);
        adicionarCampo(form, g, 4, "Estoque mínimo (para o alerta):", txtMinimo);

        JButton botao = criarBotao("Cadastrar produto");
        botao.addActionListener(e -> cadastrar());
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2;
        g.insets = new Insets(20, 8, 8, 8);
        form.add(botao, g);

        JPanel externo = new JPanel(new FlowLayout(FlowLayout.CENTER));
        externo.add(form);
        return externo;
    }

    private void cadastrar() {
        try {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) {
                erro("Digite o nome do produto.");
                return;
            }
            int codigo = Integer.parseInt(txtCodigo.getText().trim());
            int qtd = Integer.parseInt(txtQtd.getText().trim());
            double valor = Double.parseDouble(txtValor.getText().trim().replace(',', '.'));
            int minimo = Integer.parseInt(txtMinimo.getText().trim());

            if (qtd < 0) { erro("Impossível adicionar quantidade negativa."); return; }
            if (valor < 0) { erro("Impossível adicionar valor negativo."); return; }
            if (minimo < 0) { erro("O estoque mínimo não pode ser negativo."); return; }
            if (dao.buscarPorCodigo(codigo) != null) {
                erro("Já existe um produto cadastrado com o código " + codigo + ".");
                return;
            }

            Produto novo = new Produto(codigo, nome, qtd, valor, minimo);
            dao.inserir(novo);

            JOptionPane.showMessageDialog(this, "Produto cadastrado com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            limparCadastro();
            atualizarTudo();

            if (novo.estoqueBaixo()) {
                alertaEstoque(novo);
            }
        } catch (NumberFormatException ex) {
            erro("Preencha código, quantidade, valor e mínimo apenas com números.");
        } catch (SQLException ex) {
            erroBanco(ex);
        }
    }

    private void limparCadastro() {
        txtNome.setText("");
        txtCodigo.setText("");
        txtQtd.setText("");
        txtValor.setText("");
        txtMinimo.setText("5");
        txtNome.requestFocus();
    }

    // ---------------------------------------------------------------- ABA PRODUTOS

    private JPanel criarAbaProdutos() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        tabela.setRowHeight(26);
        tabela.setFont(tabela.getFont().deriveFont(13f));
        tabela.getTableHeader().setFont(tabela.getFont().deriveFont(Font.BOLD, 13f));

        // Pinta de vermelho a linha dos produtos com estoque no mínimo
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor,
                    boolean selecionada, boolean foco, int linha, int coluna) {
                Component c = super.getTableCellRendererComponent(t, valor, selecionada, foco, linha, coluna);
                boolean baixo = "ESTOQUE BAIXO".equals(modeloTabela.getValueAt(linha, 7));
                if (!selecionada) {
                    c.setBackground(baixo ? VERMELHO_CLARO : Color.WHITE);
                    c.setForeground(baixo ? VERMELHO_ESCURO : Color.BLACK);
                }
                return c;
            }
        });

        // Barra de busca (filtra enquanto a pessoa digita)
        JPanel barraBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraBusca.add(new JLabel("Buscar por nome ou código:"));
        barraBusca.add(txtBusca);
        JButton limpar = new JButton("Limpar");
        limpar.addActionListener(e -> txtBusca.setText(""));
        barraBusca.add(limpar);
        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrarTabela(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrarTabela(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarTabela(); }
        });

        // Botões de ação
        JButton atualizar = criarBotao("Atualizar lista");
        atualizar.addActionListener(e -> atualizarTudo());
        JButton editar = criarBotao("Editar selecionado");
        editar.addActionListener(e -> editarSelecionado());
        JButton excluir = criarBotao("Excluir selecionado");
        excluir.setBackground(VERMELHO_ESCURO);
        excluir.addActionListener(e -> excluirSelecionado());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        botoes.add(atualizar);
        botoes.add(editar);
        botoes.add(excluir);

        painel.add(barraBusca, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        painel.add(botoes, BorderLayout.SOUTH);
        return painel;
    }

    /** Recarrega só a tabela com o filtro atual (usado pela busca). */
    private void filtrarTabela() {
        try {
            carregarTabela();
        } catch (SQLException ex) {
            erroBanco(ex);
        }
    }

    /** Devolve o código do produto selecionado na tabela, ou -1 se nada estiver selecionado. */
    private int codigoSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            erro("Clique em um produto da tabela primeiro.");
            return -1;
        }
        return (Integer) modeloTabela.getValueAt(linha, 0);
    }

    private void editarSelecionado() {
        int codigo = codigoSelecionado();
        if (codigo < 0) return;
        try {
            Produto atual = dao.buscarPorCodigo(codigo);
            if (atual == null) {
                erro("Esse produto não existe mais.");
                atualizarTudo();
                return;
            }

            JTextField nome = new JTextField(atual.getNome(), 20);
            JTextField valor = new JTextField(String.valueOf(atual.getValor()), 20);
            JTextField minimo = new JTextField(String.valueOf(atual.getEstoqueMinimo()), 20);

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Código (não editável):"));
            form.add(new JLabel(String.valueOf(codigo)));
            form.add(new JLabel("Nome:"));
            form.add(nome);
            form.add(new JLabel("Valor unitário (R$):"));
            form.add(valor);
            form.add(new JLabel("Estoque mínimo:"));
            form.add(minimo);

            int resposta = JOptionPane.showConfirmDialog(this, form, "Editar produto",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resposta != JOptionPane.OK_OPTION) return;

            String novoNome = nome.getText().trim();
            double novoValor = Double.parseDouble(valor.getText().trim().replace(',', '.'));
            int novoMinimo = Integer.parseInt(minimo.getText().trim());

            if (novoNome.isEmpty()) { erro("O nome não pode ficar vazio."); return; }
            if (novoValor < 0) { erro("Impossível adicionar valor negativo."); return; }
            if (novoMinimo < 0) { erro("O estoque mínimo não pode ser negativo."); return; }

            dao.atualizar(new Produto(codigo, novoNome, atual.getQuantidade(), novoValor, novoMinimo));
            atualizarTudo();

            // Se o novo mínimo faz o produto entrar na zona de alerta, avisa
            Produto depois = dao.buscarPorCodigo(codigo);
            if (depois != null && depois.estoqueBaixo()) {
                alertaEstoque(depois);
            }
        } catch (NumberFormatException ex) {
            erro("Digite números válidos em valor e estoque mínimo.");
        } catch (SQLException ex) {
            erroBanco(ex);
        }
    }

    private void excluirSelecionado() {
        int codigo = codigoSelecionado();
        if (codigo < 0) return;
        String nome = String.valueOf(modeloTabela.getValueAt(tabela.getSelectedRow(), 1));

        int resposta = JOptionPane.showConfirmDialog(this,
                "Excluir o produto \"" + nome + "\" (código " + codigo + ")?\nEssa ação não pode ser desfeita.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) return;

        try {
            dao.excluir(codigo);
            atualizarTudo();
        } catch (SQLException ex) {
            erroBanco(ex);
        }
    }

    private void carregarTabela() throws SQLException {
        modeloTabela.setRowCount(0);
        for (Produto p : dao.listar(txtBusca.getText())) {
            modeloTabela.addRow(new Object[] {
                    p.getCodigo(), p.getNome(), p.getQuantidade(), p.getEstoqueMinimo(),
                    moeda.format(p.getValor()), p.getEntradas(), p.getSaidas(),
                    p.estoqueBaixo() ? "ESTOQUE BAIXO" : "OK"
            });
        }
    }

    // ---------------------------------------------------------------- ABA MOVIMENTAR

    private JPanel criarAbaMovimentacao() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        adicionarCampo(form, g, 0, "Código do produto:", txtCodigoMov);
        adicionarCampo(form, g, 1, "Tipo de movimentação:", comboTipo);
        adicionarCampo(form, g, 2, "Quantidade:", txtQtdMov);

        JButton botao = criarBotao("Confirmar movimentação");
        botao.addActionListener(e -> movimentar());
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        g.insets = new Insets(20, 8, 8, 8);
        form.add(botao, g);

        JPanel externo = new JPanel(new FlowLayout(FlowLayout.CENTER));
        externo.add(form);
        return externo;
    }

    private void movimentar() {
        try {
            int codigo = Integer.parseInt(txtCodigoMov.getText().trim());
            int qtd = Integer.parseInt(txtQtdMov.getText().trim());

            if (qtd <= 0) {
                erro("Digite uma quantidade maior que zero.");
                return;
            }
            Produto antes = dao.buscarPorCodigo(codigo);
            if (antes == null) {
                erro("Código não encontrado.");
                return;
            }

            boolean entrada = comboTipo.getSelectedIndex() == 0;
            if (entrada) {
                dao.registrarEntrada(codigo, qtd);
            } else if (!dao.registrarSaida(codigo, qtd)) {
                erro("Impossível retirar essa quantidade.\nEstoque atual de \""
                        + antes.getNome() + "\": " + antes.getQuantidade());
                return;
            }

            Produto depois = dao.buscarPorCodigo(codigo);
            JOptionPane.showMessageDialog(this,
                    (entrada ? "Quantidade acrescentada!" : "Quantidade retirada!")
                            + "\nNovo estoque de \"" + depois.getNome() + "\": " + depois.getQuantidade(),
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            txtCodigoMov.setText("");
            txtQtdMov.setText("");
            atualizarTudo();

            // ALERTA AUTOMÁTICO: depois de toda movimentação, confere o mínimo
            if (depois.estoqueBaixo()) {
                alertaEstoque(depois);
            }
        } catch (NumberFormatException ex) {
            erro("Digite apenas números inteiros no código e na quantidade.");
        } catch (SQLException ex) {
            erroBanco(ex);
        }
    }

    // ---------------------------------------------------------------- ABA RESUMO

    private JPanel criarAbaResumo() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        txtResumo.setEditable(false);
        txtResumo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        txtResumo.setMargin(new Insets(15, 15, 15, 15));

        painel.add(new JScrollPane(txtResumo), BorderLayout.CENTER);
        return painel;
    }

    private void carregarResumo() throws SQLException {
        double[] totais = dao.totais();
        Produto maior = dao.maiorEstoque();
        Produto menor = dao.menorEstoque();

        StringBuilder sb = new StringBuilder();
        sb.append("VALOR TOTAL DO ESTOQUE CADASTRADO\n");
        sb.append("=================================\n");
        sb.append("Quantidade total de itens: ").append((long) totais[0]).append("\n");
        sb.append("Valor total do estoque:    ").append(moeda.format(totais[1])).append("\n\n");

        sb.append("PRODUTO COM MAIOR ESTOQUE\n");
        sb.append("=================================\n");
        sb.append(maior == null ? "Nenhum produto cadastrado.\n" : descrever(maior)).append("\n");

        sb.append("PRODUTO COM MENOR ESTOQUE\n");
        sb.append("=================================\n");
        sb.append(menor == null ? "Nenhum produto cadastrado.\n" : descrever(menor));

        txtResumo.setText(sb.toString());
        txtResumo.setCaretPosition(0);
    }

    private String descrever(Produto p) {
        return "Produto:    " + p.getNome() + "\n"
             + "Código:     " + p.getCodigo() + "\n"
             + "Quantidade: " + p.getQuantidade() + "\n";
    }

    // ---------------------------------------------------------------- ALERTAS

    /** Pop-up de aviso para UM produto que chegou ao estoque mínimo. */
    private void alertaEstoque(Produto p) {
        JOptionPane.showMessageDialog(this,
                "ATENÇÃO! ESTOQUE NO MÍNIMO!\n\n"
                        + "Produto: " + p.getNome() + " (código " + p.getCodigo() + ")\n"
                        + "Estoque atual: " + p.getQuantidade() + "\n"
                        + "Estoque mínimo: " + p.getEstoqueMinimo() + "\n\n"
                        + "Providencie a reposição.",
                "Alerta de estoque", JOptionPane.WARNING_MESSAGE);
    }

    /** Pop-up ao abrir o sistema listando todos os produtos no mínimo. */
    private void alertaInicial() {
        try {
            List<Produto> baixos = dao.listarEstoqueBaixo();
            if (!baixos.isEmpty()) {
                String lista = baixos.stream()
                        .map(p -> "• " + p.getNome() + ": " + p.getQuantidade()
                                + " (mínimo " + p.getEstoqueMinimo() + ")")
                        .collect(Collectors.joining("\n"));
                JOptionPane.showMessageDialog(this,
                        "Estes produtos estão com estoque no mínimo:\n\n" + lista,
                        "Alerta de estoque", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            // Sem aviso aqui: o erro de conexão já foi mostrado em atualizarTudo()
        }
    }

    /** Atualiza a faixa do topo: vermelha se houver produto no mínimo, verde se estiver tudo certo. */
    private void atualizarFaixaAlerta() {
        try {
            List<Produto> baixos = dao.listarEstoqueBaixo();
            if (baixos.isEmpty()) {
                faixaAlerta.setBackground(VERDE_CLARO);
                faixaAlerta.setForeground(new Color(0, 100, 30));
                faixaAlerta.setText("Estoque em dia: nenhum produto no mínimo.");
            } else {
                String nomes = baixos.stream().map(Produto::getNome).collect(Collectors.joining(", "));
                faixaAlerta.setBackground(VERMELHO_CLARO);
                faixaAlerta.setForeground(VERMELHO_ESCURO);
                faixaAlerta.setText("ATENÇÃO: " + baixos.size() + " produto(s) com estoque no mínimo: " + nomes);
            }
        } catch (SQLException ex) {
            faixaAlerta.setBackground(Color.LIGHT_GRAY);
            faixaAlerta.setForeground(Color.DARK_GRAY);
            faixaAlerta.setText("Sem conexão com o banco de dados.");
        }
    }

    // ---------------------------------------------------------------- UTILITÁRIOS

    /** Recarrega tudo que depende do banco. */
    private void atualizarTudo() {
        try {
            carregarTabela();
            carregarResumo();
        } catch (SQLException ex) {
            erroBanco(ex);
        }
        atualizarFaixaAlerta();
    }

    private void adicionarCampo(JPanel painel, GridBagConstraints g, int linha, String rotulo, JComponent campo) {
        g.gridwidth = 1;
        g.insets = new Insets(8, 8, 8, 8);
        g.gridx = 0; g.gridy = linha; g.weightx = 0;
        painel.add(new JLabel(rotulo), g);
        g.gridx = 1; g.weightx = 1;
        painel.add(campo, g);
    }

    private JButton criarBotao(String texto) {
        JButton b = new JButton(texto);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setBackground(AZUL);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(220, 38));
        return b;
    }

    private void erro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.ERROR_MESSAGE);
    }

    private void erroBanco(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Erro ao acessar o banco de dados:\n" + ex.getMessage()
                        + "\n\nConfira usuário/senha em Conexao.java e se o MySQL está ligado.",
                "Erro de banco de dados", JOptionPane.ERROR_MESSAGE);
    }
}
