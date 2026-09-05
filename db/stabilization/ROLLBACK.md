# Rollback da estabilização

Pare backend e frontend antes da troca. A reversão segura é restaurar o backup obtido imediatamente antes de `V002__normalize_and_constrain.sql`, pois máscaras e aliases originais não podem ser reconstruídos sem perda de informação.

Se somente o binário precisar voltar, mantenha os dados canônicos e as colunas `numeric(19,8)`: o esquema é compatível com leitura numérica pelo código anterior. Não remova as restrições enquanto o binário novo puder voltar a receber tráfego.

Ordem coordenada: interromper tráfego, fazer backup, executar `V001`, executar `V002`, implantar backend, validar `/acoes`, `/corretoras` e `/carteira`, então publicar o frontend. Em falha, interromper tráfego, restaurar o backup e recolocar os dois artefatos anteriores.
