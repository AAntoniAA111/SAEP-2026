package main.java.model;

/**
 * Representa um produto do estoque.
 * Substitui os vetores paralelos do Portugol (produto[], codigo[], estoque[]...):
 * agora cada produto é UM objeto com todos os seus dados juntos.
 */
public class Produto {

    private final int codigo;
    private final String nome;
    private final int quantidade;
    private final double valor;
    private final int estoqueMinimo;
    private final int entradas;
    private final int saidas;

    // Usado ao cadastrar um produto novo (entradas e saídas começam em 0)
    public Produto(int codigo, String nome, int quantidade, double valor, int estoqueMinimo) {
        this(codigo, nome, quantidade, valor, estoqueMinimo, 0, 0);
    }

    // Usado ao ler um produto que já está no banco
    public Produto(int codigo, String nome, int quantidade, double valor,
                   int estoqueMinimo, int entradas, int saidas) {
        this.codigo = codigo;
        this.nome = nome;
        this.quantidade = quantidade;
        this.valor = valor;
        this.estoqueMinimo = estoqueMinimo;
        this.entradas = entradas;
        this.saidas = saidas;
    }

    public int getCodigo()        { return codigo; }
    public String getNome()       { return nome; }
    public int getQuantidade()    { return quantidade; }
    public double getValor()      { return valor; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public int getEntradas()      { return entradas; }
    public int getSaidas()        { return saidas; }

    /** true quando o estoque chegou ao mínimo (ou ficou abaixo dele). */
    public boolean estoqueBaixo() {
        return quantidade <= estoqueMinimo;
    }
}
