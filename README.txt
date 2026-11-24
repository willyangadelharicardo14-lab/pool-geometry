POOL GEOMETRY — ANDROID OVERLAY STARTER
=======================================

Este é o primeiro projeto Android nativo para exibir uma camada transparente sobre outro aplicativo.

REQUISITOS
- Android Studio recente
- Android SDK 36 instalado
- JDK 17
- Tablet Android 8.0 ou superior (testar em Android 16)

COMO ABRIR
1. Extraia o ZIP.
2. No Android Studio, escolha Open e selecione a pasta PoolGeometry_Android_Overlay.
3. Aguarde o Gradle Sync.
4. Execute no tablet.
5. Na tela inicial, toque em "Autorizar sobreposição" e habilite "Permitir sobre outros apps".
6. Volte ao app e toque em "Iniciar menu flutuante".
7. Abra o 8 Ball Pool: o botão PG e as linhas devem permanecer por cima do jogo.
8. Arraste os pontos 1 e 2. Toque em PG para ajustar largura, altura e quantidade de segmentos.
9. Para parar, abra novamente o app e encerre o serviço (ou force a parada nas configurações).

LIMITAÇÕES DESTA PRIMEIRA INTEGRAÇÃO
- O projeto desenha uma trajetória geométrica sobre a tela, mas ainda usa uma área de mesa proporcional e centralizada.
- Não detecta automaticamente a mesa nem as bolas do jogo.
- Antes de usar como mira, será necessário alinhar a área de projeção com a mesa real. A calibração de quatro bordas será a próxima etapa.
- A física ainda não considera spin, força, atrito ou colisões com outras bolas.
- O projeto é código-fonte para Android Studio; não inclui APK pré-compilado.
- O serviço usa foreground service do tipo specialUse para manter a janela ativa. O Android poderá exibir uma notificação persistente enquanto estiver ligado.
