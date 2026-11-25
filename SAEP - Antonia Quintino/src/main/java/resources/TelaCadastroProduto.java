package main.java.resources;

import main.java.model.*;
import main.java.dao.*;
import main.java.util.*;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * ENTREGA 6 - Interface de cadastro de produto.
 * Cadastrar, buscar, editar e excluir produtos.
 */
public class TelaCadastroProduto extends JDialog {

    private final Usuario usuario;
    private final ProdutoDAO dao = new ProdutoDAO();

    private final JTextField txtNome = new JTextField(18);
    private final JTextField txtCodigo = new JTextField(10);
    private final JTextField txtQtd = new JTextField(10);
    private final JTextField txtValor = new JTextField(10);
    private final JTextField txtMinimo = new JTextField("5", 10);

    private final javax.swing.table.DefaultTableModel modelo = Ui.modeloSomenteLeitura(
            "Código", "Produto", "Estoque", "Mínimo", "Valor unit.", "Entradas", "Saídas", "Situação");
    private final JTable tabela = new JTable(modelo);
    private final JTextField txtBusca = new JTextField(25);

    public TelaCadastroProduto(Frame dono, Usuario usuario) {
        super(dono, "Cadastro de produtos", true);
        this.usuario = usuario;
        setSize(1000, 680);
        setLocationRelativeTo(dono);
        setLayout(new BorderLayout());

        add(Ui.titulo("CADASTRO DE PRODUTOS"), BorderLayout.NORTH);

        JPanel corpo = new JPanel(new BorderLayout(10, 10));
        corpo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        corpo.add(criarFormulario(), BorderLayout.NORTH);
        corpo.add(criarListagem(), BorderLayout.CENTER);
        add(corpo, BorderLayout.CENTER);

        carregarTabela();
    }

    // ------------------------------------------------------------ formulário

    private JPanel criarFormulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Novo produto"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        campo(p, g, 0, 0, "Nome do produto:", txtNome);
        campo(p, g, 0, 1, "Código (inteiro):", txtCodigo);
        campo(p, g, 1, 0, "Quantidade inicial:", txtQtd);
        campo(p, g, 1, 1, "Valor unitário (R$):", txtValor);
        campo(p, g, 2, 0, "Estoque mínimo (alerta):", txtMinimo);

        JButton cadastrar = Ui.botao("Cadastrar produto");
        cadastrar.addActionListener(e -> cadastrar());
        g.gridx = 3; g.gridy = 2;
        p.add(cadastrar, g);
        return p;
    }

    private void campo(JPanel p, GridBagConstraints g, int linha, int coluna, String rotulo, JTextField f) {
        g.gridy = linha;
        g.gridx = coluna * 2;
        p.add(new JLabel(rotulo), g);
        g.gridx = coluna * 2 + 1;
        p.add(f, g);
    }

    private void cadastrar() {
        try {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) { Ui.erro(this, "Digite o nome do produto."); return; }
            int codigo = Integer.parseInt(txtCodigo.getText().trim());
            int qtd = Integer.parseInt(txtQtd.getText().trim());
            double valor = Double.parseDouble(txtValor.getText().trim().replace(',', '.'));
            int minimo = Integer.parseInt(txtMinimo.getText().trim());

            if (qtd < 0) { Ui.erro(this, "Impossível adicionar quantidade negativa."); return; }
            if (valor < 0) { Ui.erro(this, "Impossível adicionar valor negativo."); return; }
            if (minimo < 0) { Ui.erro(this, "O estoque mínimo não pode ser negativo."); return; }
            if (dao.buscarPorCodigo(codigo) != null) {
                Ui.erro(this, "Já existe um produto cadastrado com o código " + codigo + ".");
                return;
            }

            Produto novo = new Produto(codigo, nome, qtd, valor, minimo);
            dao.inserir(novo);

            JOptionPane.showMessageDialog(this, "Produto cadastrado com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            limparFormulario();
            carregarTabela();
            if (novo.estoqueBaixo()) {
                Ui.alertaEstoque(this, novo);
            }
        } catch (NumberFormatException ex) {
            Ui.erro(this, "Preencha código, quantidade, valor e mínimo apenas com números.");
        } catch (SQLIntegrityConstraintViolationException ex) {
            Ui.erro(this, "Esse código já foi utilizado (talvez por um produto excluído). Use outro código.");
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }

    private void limparFormulario() {
        txtNome.setText("");
        txtCodigo.setText("");
        txtQtd.setText("");
        txtValor.setText("");
        txtMinimo.setText("5");
        txtNome.requestFocus();
    }

    // ------------------------------------------------------------ listagem

    private JPanel criarListagem() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Produtos cadastrados"));

        Ui.pintarLinhasBaixas(tabela, modelo, 7);

        JPanel barraBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraBusca.add(new JLabel("Buscar por nome ou código:"));
        barraBusca.add(txtBusca);
        JButton limpar = new JButton("Limpar");
        limpar.addActionListener(e -> txtBusca.setText(""));
        barraBusca.add(limpar);
        Ui.aoDigitar(txtBusca, this::carregarTabela);

        JButton editar = Ui.botao("Editar selecionado");
        editar.addActionListener(e -> editarSelecionado());
        JButton excluir = Ui.botao("Excluir selecionado");
        excluir.setBackground(Ui.VERMELHO_ESCURO);
        excluir.addActionListener(e -> excluirSelecionado());
        if (!usuario.isAdministrador()) {
            excluir.setEnabled(false); // só o administrador exclui
            excluir.setToolTipText("Somente o administrador pode excluir produtos.");
        }
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        botoes.add(editar);
        botoes.add(excluir);

        p.add(barraBusca, BorderLayout.NORTH);
        p.add(new JScrollPane(tabela), BorderLayout.CENTER);
        p.add(botoes, BorderLayout.SOUTH);
        return p;
    }

    private void carregarTabela() {
        try {
            modelo.setRowCount(0);
            for (Produto p : dao.listar(txtBusca.getText())) {
                modelo.addRow(new Object[] {
                        p.getCodigo(), p.getNome(), p.getQuantidade(), p.getEstoqueMinimo(),
                        Ui.MOEDA.format(p.getValor()), p.getEntradas(), p.getSaidas(),
                        p.estoqueBaixo() ? "ESTOQUE BAIXO" : "OK"
                });
            }
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }

    /** Código do produto selecionado na tabela, ou -1 se não houver seleção. */
    private int codigoSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.erro(this, "Clique em um produto da tabela primeiro.");
            return -1;
        }
        return (Integer) modelo.getValueAt(linha, 0);
    }

    private void editarSelecionado() {
        int codigo = codigoSelecionado();
        if (codigo < 0) return;
        try {
            Produto atual = dao.buscarPorCodigo(codigo);
            if (atual == null) {
                Ui.erro(this, "Esse produto não existe mais.");
                carregarTabela();
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
            if (novoNome.isEmpty()) { Ui.erro(this, "O nome não pode ficar vazio."); return; }
            if (novoValor < 0) { Ui.erro(this, "Impossível adicionar valor negativo."); return; }
            if (novoMinimo < 0) { Ui.erro(this, "O estoque mínimo não pode ser negativo."); return; }

            dao.atualizar(new Produto(codigo, novoNome, atual.getQuantidade(), novoValor, novoMinimo));
            carregarTabela();

            Produto depois = dao.buscarPorCodigo(codigo);
            if (depois != null && depois.estoqueBaixo()) {
                Ui.alertaEstoque(this, depois);
            }
        } catch (NumberFormatException ex) {
            Ui.erro(this, "Digite números válidos em valor e estoque mínimo.");
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }

    private void excluirSelecionado() {
        int codigo = codigoSelecionado();
        if (codigo < 0) return;
        String nome = String.valueOf(modelo.getValueAt(tabela.getSelectedRow(), 1));

        int resposta = JOptionPane.showConfirmDialog(this,
                "Excluir o produto \"" + nome + "\" (código " + codigo + ")?\n"
                        + "Ele sai da lista, mas o histórico de movimentações é mantido.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) return;
        try {
            dao.inativar(codigo);
            carregarTabela();
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }
}
