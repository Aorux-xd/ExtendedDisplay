# ED Network

ed-network/ - это сборник из трех Gradle-модулей экосистемы ED:

- ed-common  - общая библиотека (codec, модели, канал `ed:proxy`) для proxy и expansion.
- ed-proxy - Velocity-плагин, который собирает snapshot серверов и рассылает их на backend.
- ed-expansion - Paper-плагин + PlaceholderAPI expansion, читающий данные канала и отдающий плейсхолдеры.
