-- =====================================================================
-- BiT Power - Sistema de Estoque
-- ENTREGA 3: Script de criação e população do banco de dados (MySQL 8)
--
-- ATENÇÃO: este script APAGA e recria as tabelas. Rode-o do zero sempre
-- que quiser voltar o banco ao estado inicial de testes.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS saep_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE saep_db;

DROP TABLE IF EXISTS movimentacoes;
DROP TABLE IF EXISTS produtos;
DROP TABLE IF EXISTS usuarios;

-- ---------------------------------------------------------------------
-- CRIAÇÃO DAS TABELAS
-- ---------------------------------------------------------------------

CREATE TABLE usuarios (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    login       VARCHAR(30)  NOT NULL UNIQUE,
    senha_hash  CHAR(64)     NOT NULL,                       -- SHA-256 da senha (nunca a senha pura)
    perfil      ENUM('ADMINISTRADOR','OPERADOR') NOT NULL DEFAULT 'OPERADOR'
);

CREATE TABLE produtos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    codigo          INT           NOT NULL UNIQUE,           -- código de negócio digitado pelo usuário
    nome            VARCHAR(100)  NOT NULL,
    quantidade      INT           NOT NULL DEFAULT 0,        -- saldo atual em estoque
    valor           DECIMAL(10,2) NOT NULL,                  -- valor unitário (R$)
    estoque_minimo  INT           NOT NULL DEFAULT 5,        -- quantidade <= mínimo dispara o alerta
    ativo           TINYINT(1)    NOT NULL DEFAULT 1,        -- 0 = "excluído" (mantém o histórico)
    CONSTRAINT ck_produto_quantidade CHECK (quantidade >= 0),
    CONSTRAINT ck_produto_valor      CHECK (valor >= 0),
    CONSTRAINT ck_produto_minimo     CHECK (estoque_minimo >= 0)
);

CREATE TABLE movimentacoes (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    produto_id  INT          NOT NULL,
    usuario_id  INT          NOT NULL,                       -- RESPONSÁVEL pela operação
    tipo        ENUM('ENTRADA','SAIDA') NOT NULL,
    quantidade  INT          NOT NULL,
    data_hora   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- DATA da operação
    observacao  VARCHAR(200) NULL,
    CONSTRAINT fk_mov_produto FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_mov_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_mov_data ON movimentacoes (data_hora);

-- ---------------------------------------------------------------------
-- POPULAÇÃO (dados de exemplo para testes)
-- ---------------------------------------------------------------------

-- Usuários:  admin / admin123   (administrador)
--            operador / operador123
INSERT INTO usuarios (nome, login, senha_hash, perfil) VALUES
    ('Administrador do Sistema', 'admin',    SHA2('admin123', 256),    'ADMINISTRADOR'),
    ('Carlos Operador',          'operador', SHA2('operador123', 256), 'OPERADOR');

-- Produtos (a quantidade já é o saldo FINAL após as movimentações abaixo)
INSERT INTO produtos (codigo, nome, quantidade, valor, estoque_minimo) VALUES
    (1001, 'Notebook 15"',        3, 3499.90,  3),   -- no mínimo  -> alerta
    (1002, 'Mouse sem fio',      30,   59.90, 10),
    (1003, 'Teclado mecânico',    4,  249.90,  5),   -- abaixo     -> alerta
    (1004, 'Monitor 24"',        10,  899.00,  4),
    (1005, 'Cabo HDMI 2m',       10,   29.90, 15),   -- abaixo     -> alerta
    (1006, 'Webcam Full HD',      9,  189.90,  5),
    (1007, 'Headset gamer',      13,  199.90,  6),
    (1008, 'Pendrive 64GB',      35,   39.90, 20);

-- Histórico de movimentações (usuario_id 1 = admin, 2 = operador)
-- Conferência: saldo = soma das ENTRADAS - soma das SAÍDAS de cada produto.
INSERT INTO movimentacoes (produto_id, usuario_id, tipo, quantidade, data_hora, observacao) VALUES
    ((SELECT id FROM produtos WHERE codigo = 1001), 1, 'ENTRADA',  10, '2026-11-02 09:00:00', 'Estoque inicial - NF 1020'),
    ((SELECT id FROM produtos WHERE codigo = 1001), 2, 'SAIDA',     4, '2026-11-05 14:30:00', 'Venda balcão'),
    ((SELECT id FROM produtos WHERE codigo = 1001), 2, 'SAIDA',     3, '2026-11-12 10:15:00', 'Venda online'),

    ((SELECT id FROM produtos WHERE codigo = 1002), 1, 'ENTRADA',  50, '2026-11-02 09:10:00', 'Estoque inicial - NF 1020'),
    ((SELECT id FROM produtos WHERE codigo = 1002), 2, 'SAIDA',    12, '2026-11-06 11:00:00', 'Venda balcão'),
    ((SELECT id FROM produtos WHERE codigo = 1002), 2, 'SAIDA',     8, '2026-11-13 16:20:00', 'Venda online'),

    ((SELECT id FROM produtos WHERE codigo = 1003), 1, 'ENTRADA',  20, '2026-11-02 09:20:00', 'Estoque inicial - NF 1020'),
    ((SELECT id FROM produtos WHERE codigo = 1003), 2, 'SAIDA',    10, '2026-11-07 15:45:00', 'Venda balcão'),
    ((SELECT id FROM produtos WHERE codigo = 1003), 2, 'SAIDA',     6, '2026-11-14 13:10:00', 'Venda online'),

    ((SELECT id FROM produtos WHERE codigo = 1004), 1, 'ENTRADA',  15, '2026-11-02 09:30:00', 'Estoque inicial - NF 1021'),
    ((SELECT id FROM produtos WHERE codigo = 1004), 2, 'SAIDA',     5, '2026-11-09 10:00:00', 'Venda balcão'),

    ((SELECT id FROM produtos WHERE codigo = 1005), 1, 'ENTRADA',  60, '2026-11-03 08:50:00', 'Estoque inicial - NF 1022'),
    ((SELECT id FROM produtos WHERE codigo = 1005), 2, 'SAIDA',    30, '2026-11-10 17:05:00', 'Venda atacado'),
    ((SELECT id FROM produtos WHERE codigo = 1005), 2, 'SAIDA',    20, '2026-11-17 12:40:00', 'Venda atacado'),

    ((SELECT id FROM produtos WHERE codigo = 1006), 1, 'ENTRADA',  12, '2026-11-03 09:00:00', 'Estoque inicial - NF 1022'),
    ((SELECT id FROM produtos WHERE codigo = 1006), 2, 'SAIDA',     3, '2026-11-11 14:00:00', 'Venda online'),

    ((SELECT id FROM produtos WHERE codigo = 1007), 1, 'ENTRADA',  18, '2026-11-03 09:10:00', 'Estoque inicial - NF 1022'),
    ((SELECT id FROM produtos WHERE codigo = 1007), 2, 'SAIDA',     5, '2026-11-16 15:30:00', 'Venda balcão'),

    ((SELECT id FROM produtos WHERE codigo = 1008), 1, 'ENTRADA', 100, '2026-11-04 08:30:00', 'Estoque inicial - NF 1023'),
    ((SELECT id FROM produtos WHERE codigo = 1008), 2, 'SAIDA',    40, '2026-11-12 09:45:00', 'Venda atacado'),
    ((SELECT id FROM produtos WHERE codigo = 1008), 2, 'SAIDA',    25, '2026-11-19 18:10:00', 'Venda balcão');

-- ---------------------------------------------------------------------
-- CONSULTAS DE CONFERÊNCIA (opcional)
-- ---------------------------------------------------------------------
-- Produtos no estoque mínimo:
--   SELECT codigo, nome, quantidade, estoque_minimo FROM produtos WHERE quantidade <= estoque_minimo;
-- Saldo calculado pelo histórico (deve ser igual a produtos.quantidade):
--   SELECT p.codigo, p.quantidade,
--          SUM(CASE m.tipo WHEN 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END) AS saldo_historico
--   FROM produtos p JOIN movimentacoes m ON m.produto_id = p.id GROUP BY p.id;
