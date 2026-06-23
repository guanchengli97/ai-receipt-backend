# Quick Start

This project has two common workflows:

- Local development: run MySQL and the app with Docker Compose.
- Production deployment: provision MySQL with Ansible, then deploy the Spring Boot app as a Docker container through GitHub Actions.

## Local Development

1. Copy the environment template:

```bash
cp .env.example .env
```

2. Fill in the required values in `.env`.

At minimum, local startup needs database values. Receipt parsing, S3 uploads, email activation, Google login, and Stripe billing require their corresponding API keys.

3. Start the stack:

```bash
docker compose up --build
```

4. Open the API:

```text
http://localhost:8080/api
```

## Maven Development

Start MySQL first, then run:

```bash
mvn spring-boot:run
```

Build the application:

```bash
mvn clean package
```

Run the packaged JAR:

```bash
java -jar target/ai-receipt-backend-0.0.1-SNAPSHOT.jar
```

## Production Deployment Summary

1. Use Ansible to provision MySQL on the target server:

```bash
cd ansible
bash deploy.sh mysql
```

2. Configure GitHub Actions secrets for Docker Hub, Oracle SSH, database, JWT, S3, Gemini, mail, Google, and Stripe.

3. Push to `master`:

```bash
git push origin master
```

GitHub Actions builds the Java app, pushes the Docker image, connects to the Oracle host, and restarts the `ai-receipt-app` container.

## Useful Checks

```bash
# Local app
curl http://localhost:8080/api/

# Production app
curl http://<ORACLE_HOST>:7008/api/

# Production container
ssh ubuntu@<ORACLE_HOST>
docker ps
docker logs -f ai-receipt-app
```

## Documentation Map

- `README.md` - project overview and API map
- `ORACLE_DEPLOYMENT.md` - full production deployment guide
- `ORACLE_QUICK_REFERENCE.md` - short production command reference
- `DEPLOYMENT_CHECKLIST.md` - preflight and verification checklist
- `ANSIBLE_DEPLOYMENT.md` - Ansible MySQL provisioning guide
- `ansible/DEPLOY_GUIDE.md` - Ansible command details
