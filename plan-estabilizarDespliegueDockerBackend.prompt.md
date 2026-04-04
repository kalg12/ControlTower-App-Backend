## Plan: Estabilizar despliegue Docker backend

Resolveremos primero el bloqueo de arranque (`SettingsService`/`ObjectMapper`) y luego haremos una revisión dirigida de configuración, dependencias y contenedores para evitar regresiones en Docker. El enfoque prioriza compatibilidad con Spring Boot 4 (Jackson 3), consistencia de configuración entre `dev/prod`, y hardening de build/runtime sin tocar la lógica de negocio de módulos funcionales.

### Steps
1. Confirmar causa raíz en `SettingsService` y alinear `ObjectMapper`/`JsonMapper` en [src/main/java/com/controltower/app/settings/application/SettingsService.java](src/main/java/com/controltower/app/settings/application/SettingsService.java) y `SettingsController`.
2. Unificar migración Jackson 3 en código principal, reemplazando imports `com.fasterxml` por equivalentes `tools.jackson` donde aplique, incluyendo [src/main/java/com/controltower/app/shared/response/ApiResponse.java](src/main/java/com/controltower/app/shared/response/ApiResponse.java).
3. Corregir configuración Spring para reducir ruido y riesgos de arranque en [src/main/resources/application.yml](src/main/resources/application.yml) (`spring.data.redis.repositories.enabled`, `hibernate.dialect`).
4. Endurecer build Docker para artefacto determinista (`bootJar` único) en [Dockerfile](Dockerfile) y [build.gradle](build.gradle), evitando selección ambigua de JAR.
5. Revisar coherencia `docker-compose` y variables (`dev/prod`, healthchecks, secretos, profile) en [docker-compose.yml](docker-compose.yml), [docker-compose.prod.yml](docker-compose.prod.yml) y [src/main/resources/application-prod.yml](src/main/resources/application-prod.yml).
6. Actualizar guía operativa con pasos de rebuild limpio y diagnóstico rápido en [LOCAL_DOCKER.md](LOCAL_DOCKER.md) para prevenir despliegues con imagen/caché obsoleta.

### Further Considerations
1. ¿Alcance de este borrador? Opción A: solo fix de arranque; Opción B: fix + hardening Docker; Opción C: revisión completa multi-módulo.
2. ¿Mantenemos Spring Boot 4 ahora o preferimos plan de compatibilidad temporal con Jackson 2 en transición?
3. ¿Quieres que el siguiente ciclo priorice cero warnings de startup o solo errores bloqueantes de producción?
