# Mídia acessível e equivalência entre formatos

O Práxis aceita `IMAGE`, `AUDIO` e `VIDEO`. Formatos diferentes não são considerados equivalentes apenas por conterem a mesma situação. A autoria deve fornecer texto equivalente, transcrição para áudio/vídeo e legenda WebVTT para vídeo, e a publicação é bloqueada quando esses recursos faltam.

Cada mídia recebe uma versão imutável. O tipo e a versão apresentados são copiados para `attempt_node_serves`, permitindo separar amostras incompatíveis. A mídia nunca altera a pontuação e não substitui automaticamente o texto.

Vídeos usam controles nativos acessíveis por teclado, tela cheia, volume, pausa e controle adicional de velocidade. Reprodução automática com som não é utilizada. Uploads são verificados pelo conteúdo real com Apache Tika; imagens e áudios aceitam até 10 MB e vídeos até 100 MB nos formatos MP4, WebM, Ogg ou QuickTime.
