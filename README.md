# Rotom

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![enkan](https://img.shields.io/badge/enkan-0.15.0-blue)](https://github.com/kawasima/enkan)
[![Lucene](https://img.shields.io/badge/Lucene-10.4.0-yellow?logo=apache)](https://lucene.apache.org/)
[![License](https://img.shields.io/github/license/kawasima/rotom)](LICENSE)

Git-based Wiki system — a [Gollum](https://github.com/gollum/gollum) clone built on [enkan](https://github.com/kawasima/enkan) and JGit.

## Features

- **Git-backed storage** — every edit is a commit; full history and diff support
- **Multiple markup formats** — Markdown, Textile
- **Full-text search** — powered by Apache Lucene (Japanese-aware analyzer)
- **Authentication & authorization** — via [bouncr](https://github.com/kawasima/bouncr) or anonymous access

## Getting started

```shell
mvn package
java -jar target/rotom-0.1.0.jar
```

Open [http://localhost:3000](http://localhost:3000).

## Configuration

All settings are provided via environment variables.

| Variable     | Default    | Description                           |
| ------------ | ---------- | ------------------------------------- |
| `PORT`       | `3000`     | HTTP listen port                      |
| `REPO_PATH`  | `wiki`     | Path to the Git repository directory  |
| `INDEX_PATH` | `index`    | Path to the Lucene index directory    |
| `BASE_PATH`  | `""` (root)| URL path prefix (e.g. `/wiki`)        |

Example:

```shell
REPO_PATH=/var/lib/rotom/repo \
INDEX_PATH=/var/lib/rotom/index \
PORT=8080 \
java -jar target/rotom-0.1.0.jar
```

## Authentication

Rotom supports [bouncr](https://github.com/kawasima/bouncr) authentication and authorization.

| Permission | Operation |
|:-----------|:----------|
| `page:read` | View pages, history, and search |
| `page:create` | Create a new page |
| `page:edit` | Edit an existing page |
| `page:delete` | Delete a page |

Enable bouncr by configuring `BouncrBackend`:

```java
RotomConfiguration configuration = new RotomConfiguration();
configuration.setAuthBackend(new BouncrBackend());
```

For open access without authentication, use `AnonymousBackend` (default).

## Development

```shell
# Run with hot-reload
mvn exec:exec -Pdev

# Run tests
mvn test
```

Requires Java 25 and Maven 3.9+.
