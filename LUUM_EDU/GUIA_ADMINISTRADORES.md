# LUUM EDU - Administracion

## 1. Publicar las reglas
Firebase Console -> Firestore Database -> Reglas -> pegar el contenido de `firestore.rules` -> Publicar.
(Las reglas cambiaron: ahora incluyen la coleccion `reports`. Hay que volver a publicarlas.)

## 2. Crear el primer administrador (solo se hace una vez, desde la consola)
1. Authentication -> Usuarios -> Agregar usuario (correo y contrasena). Copiar su UID.
2. Firestore Database -> coleccion `users` -> documento con ese UID, con campos de tipo string:
   - `uid`: el mismo UID
   - `displayName`: nombre
   - `email`: correo
   - `role`: `admin`
   - `status`: `approved`

## 3. Aprobar cuentas e instituciones desde la app
Inicia sesion con la cuenta de administrador: aparece la pestana **Admin** (estrella).
- **Cuentas pendientes:** Aprobar asigna el rol solicitado y vincula la escuela elegida; Rechazar bloquea la solicitud.
- **Instituciones pendientes:** Activar la publica en el directorio; Rechazar la descarta.

Orden recomendado para arrancar: administrador -> director se registra -> admin lo aprueba ->
el director registra su escuela -> admin la activa -> ya se pueden registrar estudiantes, docentes y padres.

## 4. Retirar administradores anteriores
- Authentication: eliminar o deshabilitar las cuentas viejas.
- Firestore `users`: cambiar `role`/`status` de las cuentas viejas (o borrar sus documentos).
- Configuracion del proyecto -> Usuarios y permisos: quitar a quien ya no deba tener acceso a la consola.

## 5. Antes de publicar la app
- Activar App Check (Play Integrity) en Firebase.
- Google Cloud Console -> Credenciales: restringir la clave de API al paquete `mx.luumedu.app` y a la huella SHA-1.
- Agregar las huellas SHA-1 y SHA-256 de la llave de firma de release en Firebase (Configuracion del proyecto).
- Probar una compilacion `release` en un telefono real.
