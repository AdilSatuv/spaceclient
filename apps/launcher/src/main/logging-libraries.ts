// Controlled Space overrides; original Mojang 1.8.9 Log4j entries are never installed.
// SHA-1 checked against Maven Central sidecars; SHA-256 independently computed and pinned.
export const LOGGING_LIBRARIES = Object.freeze([
  Object.freeze({
    url: 'https://repo.maven.apache.org/maven2/org/apache/logging/log4j/log4j-api/2.26.1/log4j-api-2.26.1.jar',
    sha1: 'b9864ed8d0c01f65f3b0bf70f1447e5cc1b49c24',
    sha256: 'f1810a4704ccce019d02bba029dc02a4f2ac0c997f647b465e7d409c1927f822',
    size: 351428,
    relativePath: 'libraries/org/apache/logging/log4j/log4j-api/2.26.1/log4j-api-2.26.1.jar',
  }),
  Object.freeze({
    url: 'https://repo.maven.apache.org/maven2/org/apache/logging/log4j/log4j-core/2.26.1/log4j-core-2.26.1.jar',
    sha1: '1941359537713285d224714f63ca4e5cf004d575',
    sha256: '2ec8c3bf5a6d7c89be8620116a2fc973f7dda1ad0261ad393c4d4726edfd3dcb',
    size: 2015459,
    relativePath: 'libraries/org/apache/logging/log4j/log4j-core/2.26.1/log4j-core-2.26.1.jar',
  }),
]);
