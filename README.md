# Obsidian Core

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

> Core package for the Obsidian Framework - Published on GitHub Packages

## About

This repository contains the source code for `obsidian-core`, the heart of the Obsidian framework. It includes all essential framework features: routing, middlewares, authentication, CSRF, templates, etc.

**Full documentation**: [obsidian.kainovaii.dev](https://obsidian.kainovaii.dev)

## Installation

```xml
<dependency>
    <groupId>io.github.kainovaii</groupId>
    <artifactId>obsidian-core</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Features

- Annotation-based routing
- Middleware system (`@Before`, `@After`)
- Authentication (UserDetailsService)
- CSRF protection
- Flash messages
- Pebble Templates integration
- Styled error handler

## Build & Deploy

```bash
# Build
mvn clean package

# Deploy to GitHub Packages
mvn deploy
```

## License

MIT © [KainoVaii](https://github.com/KainoVaii)