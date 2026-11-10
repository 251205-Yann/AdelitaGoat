# Opciones de despliegue para la demostración escolar

Consulta inicial realizada el 7 de noviembre de 2026. **Actualización del 10 de noviembre:** la opción elegida y desplegada es Render Free para la API y Supabase Free para PostgreSQL. La API está en https://adelitagoat-api.onrender.com y el proyecto de base se llama `adelitagoat-escolar`. La comparación siguiente queda como antecedente de la decisión. Los límites y precios pueden cambiar.

## Recomendación inicial

Para una demostración con presupuesto cero, probar **Render Free para la API Ktor en Docker** y **Neon Free para PostgreSQL**. La app Android apuntaría a la URL HTTPS pública de Render. Esta combinación conserva los pedidos en una base externa aunque el proceso de la API se reinicie. Antes de ofrecer el servicio a usuarios reales, probar tiempos de arranque, conectividad TLS y límites de recursos con el APK y datos genuinos.

- Render admite Kotlin/JVM mediante Docker; su web service Free se suspende después de 15 minutos sin tráfico y puede tardar aproximadamente un minuto en reactivarse. Su disco local es efímero. La base PostgreSQL Free propia de Render expira a los 30 días, razón para evaluar Neon por separado. [Documentación de Render Free](https://render.com/docs/free), [Docker en Render](https://render.com/docs/docker).
- Neon Free anunció 1 GB de almacenamiento y 100 CU-horas por proyecto al mes en octubre de 2026. Verificar el plan vigente antes de crear la base. [Plan gratuito de Neon](https://neon.com/blog/neon-free-plan-1-gb-per-project).
- Railway ofrece una prueba de 30 días con 5 USD de crédito y después un plan Free con 1 USD de crédito mensual. Puede ser más simple para alojar API y base en un solo proveedor, pero el crédito y los recursos pueden no bastar para mantener un servicio Ktor activo continuamente. [Precios de Railway](https://railway.com/pricing), [documentación de la prueba](https://docs.railway.com/pricing/free-trial).

## Preparación técnica antes de desplegar

1. Construir y probar una imagen de producción del servidor con JDK/JRE 21. El servidor ya lee `PORT` y `DB_URL`, `DB_USER`, `DB_PASSWORD`; configurar `DB_URL` como URL JDBC PostgreSQL con TLS según la cadena que entregue el proveedor.
2. Desactivar explícitamente semillas demo y fallback a base local para el proceso alojado. Si PostgreSQL falla, la API debe fallar de forma visible; no guardar pedidos en el disco efímero del contenedor.
3. Configurar en el APK la URL HTTPS pública del backend. El alias `10.0.2.2` sólo funciona para el emulador local. En Android, la configuración de red de producción debe exigir HTTPS; [Android Network Security Configuration](https://developer.android.com/privacy-and-security/security-config).
4. Ejecutar migraciones en una base nueva, crear una cuenta de vendedor, registrar el puesto y menú real, luego probar desde un cliente la reserva y cambio de estado. Guardar credenciales como secretos del proveedor, nunca en Git.
5. Revisar recursos consumidos y el tiempo de despertar de Render antes de una presentación con horario fijo. Si el servicio Free resulta lento o insuficiente, cotizar un plan temporal de pago para los días de demostración.

La elección final depende de los requisitos de evaluación, disponibilidad deseada y datos reales que proporcione el compañero. No se requiere PostgreSQL instalado en esta laptop para desarrollar y probar localmente mientras se prepara el despliegue.
