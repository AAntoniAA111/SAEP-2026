package main.java.resources;

import main.java.dao.*;
import main.java.util.*;
import main.java.model.*;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** ENTREGA 4 - Interface de autenticação de usuários (login). */
public class TelaLogin extends JFrame {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final JTextField txtLogin = new JTextField(18);
    private final JPasswordField txtSenha = new JPasswordField(18);

    public TelaLogin() {
        super("BiT Power - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(430, 340);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Ui.titulo("BiT Power - Estoque"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Usuário:"), g);
        g.gridx = 1;              form.add(txtLogin, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Senha:"), g);
        g.gridx = 1;              form.add(txtSenha, g);

        JButton entrar = Ui.botao("Entrar");
        entrar.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(entrar); // a tecla ENTER aciona o botão
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        g.insets = new Insets(20, 8, 8, 8);
        form.add(entrar, g);

        JPanel centro = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centro.add(form);
        add(centro, BorderLayout.CENTER);
    }

    private void entrar() {
        String login = txtLogin.getText().trim();
        String senha = new String(txtSenha.getPassword());
        if (login.isEmpty() || senha.isEmpty()) {
            Ui.erro(this, "Informe o usuário e a senha.");
            return;
        }
        try {
            Usuario usuario = dao.autenticar(login, senha);
            if (usuario == null) {
                Ui.erro(this, "Usuário ou senha inválidos.");
                txtSenha.setText("");
                txtSenha.requestFocus();
                return;
            }
            new TelaPrincipal().setVisible(true);
            dispose();
        } catch (SQLException ex) {
            Ui.erroBanco(this, ex);
        }
    }
}