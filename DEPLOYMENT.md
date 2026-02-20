# Deployment Guide

This document outlines the steps to build, push, and run the Administrative Area API using Docker.

## Prerequisites
- Docker installed
- Docker Compose installed
- A Docker Hub account (or another container registry like GitHub Container Registry)
- Java 21 and Maven (if building the jar locally before dockerizing)

## 1. Build the Application Jar

Since the `Dockerfile` expects the pre-built application jar, build it first using Maven:

```bash
mvn clean package -DskipTests
```
*(Or use your IDE's build functionality to generate `target/AdministrativeareaApi-0.0.1-SNAPSHOT.jar`)*

## 2. Build the Docker Image

To build the Docker image, run the following command from the root of the project. Replace `<dockerhub-username>` with your actual Docker Hub username (e.g., `wanfadger`).

```bash
docker build -t <dockerhub-username>/administrativearea-api:latest .
```

## 3. Push to Docker Hub

Log in to Docker Hub with your credentials if you haven't already:

```bash
docker login
```

Then push the built image to your repository:

```bash
docker push <dockerhub-username>/administrativearea-api:latest
```

## 4. Running the Application with Docker Compose

If you are deploying this on a server or another machine, you'll need the `docker-compose.yml` file. 

Edit the `docker-compose.yml` file to ensure the `image` field under `api` matches the image you pushed to Docker Hub:
```yaml
  api:
    image: <dockerhub-username>/administrativearea-api:latest
    # ... other configurations
```
*(Note: You can remove the `build: .` line from `docker-compose.yml` in your production environment since you are pulling the pre-built image from Docker Hub).*

Start the application along with its PostgreSQL database and Redis dependencies:

```bash
docker compose up -d
```

## Useful Commands

- **View logs:** `docker compose logs -f api`
- **Stop the application:** `docker compose down`
- **Restart the application:** `docker compose restart api`
- **Rebuild and restart locally:** `docker compose up -d --build`
