package main.java.model;

import java.time.LocalDateTime;

/** Uma linha do histórico: quem mexeu, em qual produto, quanto e quando. */
public class Movimentacao {

    private final LocalDateTime dataHora;
    private final int codigoProduto;
    private final String nomeProduto;
    private final String tipo; // ENTRADA ou SAIDA
    private final int quantidade;
    private final String responsavel;
    private final String observacao;

    public Movimentacao(LocalDateTime dataHora, int codigoProduto, String nomeProduto,
                        String tipo, int quantidade, String responsavel, String observacao) {
        this.dataHora = dataHora;
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.responsavel = responsavel;
        this.observacao = observacao;
    }

    public LocalDateTime getDataHora() { return dataHora; }
    public int getCodigoProduto()      { return codigoProduto; }
    public String getNomeProduto()     { return nomeProduto; }
    public String getTipo()            { return tipo; }
    public int getQuantidade()         { return quantidade; }
    public String getResponsavel()     { return responsavel; }
    public String getObservacao()      { return observacao == null ? "" : observacao; }
}
