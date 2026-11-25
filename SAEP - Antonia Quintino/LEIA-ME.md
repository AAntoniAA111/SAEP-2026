# BiT Power – Sistema de Estoque (Java + MySQL)

## Entregas da atividade
| Nº | Entrega | Arquivo |
|----|---------|---------|
| 1 | Requisitos funcionais | `docs/BitPower_Documentacao.docx` |
| 2 | DER | `docs/BitPower_Documentacao.docx` e `docs/der.png` |
| 3 | Script de criação e população do banco | `sql/schema.sql` |
| 4 | Interface de login | `src/bitpower/TelaLogin.java` |
| 5 | Interface principal | `src/bitpower/TelaPrincipal.java` |
| 6 | Cadastro de produto | `src/bitpower/TelaCadastroProduto.java` |
| 7 | Gestão de estoque | `src/bitpower/TelaEstoque.java` |
| 8 | Casos de teste | `docs/BitPower_Documentacao.docx` |
| 9 | Requisitos de infraestrutura | `docs/BitPower_Documentacao.docx` |

## O que você precisa
- JDK 17 ou superior
- MySQL 8 rodando na sua máquina
- Driver JDBC: `mysql-connector-j-8.x.jar` (https://dev.mysql.com/downloads/connector/j/ → "Platform Independent")

## Passo a passo
1. **Banco:** execute `sql/schema.sql` no MySQL (Workbench ou terminal). Ele apaga e recria as tabelas e insere dados de exemplo.
2. **Senha do banco:** abra `src/bitpower/Conexao.java` e troque `USUARIO` e `SENHA` pelos do seu MySQL.
3. **Rodar:** abra a pasta na sua IDE, adicione o `.jar` do conector às bibliotecas e rode `bitpower.Main`.

Pelo terminal:
```
javac -encoding UTF-8 -cp mysql-connector-j-8.x.jar -d out src/bitpower/*.java
java -cp "out:mysql-connector-j-8.x.jar" bitpower.Main      # Linux/Mac
java -cp "out;mysql-connector-j-8.x.jar" bitpower.Main      # Windows
```

## Usuários de teste
| Login | Senha | Perfil |
|-------|-------|--------|
| admin | admin123 | Administrador (pode excluir produtos) |
| operador | operador123 | Operador |

## Como funciona
- **Login** obrigatório. A senha fica no banco como hash SHA-256.
- **Histórico:** toda entrada ou saída grava produto, quantidade, responsável (usuário logado) e data/hora.
- **Alerta de estoque mínimo** (quantidade ≤ mínimo): pop-up ao cadastrar/editar/movimentar, pop-up ao abrir o sistema, faixa vermelha no topo (reconferida a cada 30 s) e linha vermelha nas tabelas.
- **Excluir produto** só o inativa, para não perder o histórico.
