# Cuenta Clara para Android — prototipo inicial

Aplicación nativa Android con Kotlin + Jetpack Compose. Este paquete es un **andamiaje de desarrollo**, no un APK listo para instalar ni una aplicación de producción.

## Funciones incluidas en el prototipo
- Panel con totales de ejemplo de cuentas por cobrar, deudas y ahorros.
- Alta de ingresos, gastos, deudas, cuentas por cobrar y ahorros.
- USD y VES como monedas de registro.
- Registro de pagos/abonos parciales y saldo restante.
- Agenda básica por fechas de vencimiento.
- Conversión de ejemplo mediante una tasa manual.
- Solicitud de autenticación biométrica/credencial del dispositivo.

## Importante: todavía por implementar
- **Persistencia real:** los registros de la interfaz están en memoria y se pierden al cerrar la app. Añadir Room/DAO y migraciones antes de usarla con datos reales.
- **Sincronización en nube:** las dependencias Firebase están preparadas, pero falta configurar Firebase, autenticación de usuarios, reglas de seguridad y sincronización de conflictos.
- **Tasa BCV automática:** no se presenta como conexión real. Integrar una fuente confiable, validar la fecha de publicación, almacenar la tasa usada por operación y manejar fallos/sin conexión.
- **Notificaciones reales:** falta crear canales, pedir permiso de notificaciones, programar trabajos con WorkManager y guardar preferencias/mensajes.
- **Bloqueo biométrico obligatorio:** el botón de huella inicia el prompt, pero esta versión no bloquea la navegación de toda la app después de abrirla. Implementar una pantalla de bloqueo y políticas de reautenticación.
- **Calendario mensual interactivo y metas con aportes:** se muestran fechas de vencimiento en lista; falta el calendario mensual y el módulo completo de metas.
- **Pruebas y seguridad:** faltan pruebas, cifrado de datos sensibles y revisión de seguridad.

## Abrir en Android Studio
1. Instala Android Studio estable y abre esta carpeta como proyecto Gradle.
2. Usa JDK 17 y Android SDK 35.
3. Sincroniza Gradle y ejecuta en un emulador o teléfono Android.
4. Para Firebase, crea un proyecto propio en Firebase Console, registra `com.cuentaclara.app`, descarga `google-services.json` y colócalo en `app/`. Habilita Authentication y Cloud Firestore, y configura reglas que impidan que un usuario lea los datos de otro.
5. No guardes claves privadas ni credenciales administrativas dentro de la app.

## Reglas de cálculo recomendadas
- `saldo pendiente = importe original - suma de abonos`.
- Cada movimiento conserva su moneda original.
- La conversión se calcula con una tasa fechada y guardada en el registro, no con la tasa actual aplicada retroactivamente.
- Patrimonio neto = activos (efectivo + ahorros + cuentas por cobrar) − pasivos (deudas pendientes), con conversiones explícitas.
