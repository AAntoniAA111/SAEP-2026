package main.java;

import main.java.resources.*;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Ponto de partida do programa. */
public class Main {

    public static void main(String[] args) {
        try {
            // Usa a aparência nativa do sistema (Windows, Mac, Linux)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se falhar, usa a aparência padrão do Java
        }
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
