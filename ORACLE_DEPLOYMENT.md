# Oracle Deployment Guide

This guide describes the production deployment model for AI Receipt Backend on an Oracle/Linux host.

## Deployment Model

```text
Oracle host
|-- MySQL 8
|   |-- managed by Ansible
|   |-- database: ai_receipt_db
|   `-- user: receipt_user
`-- Docker
    `-- ai-receipt-app
        |-- managed by GitHub Actions
        |-- container port: 8080
        `-- host port: 7008
```

Ansible provisions MySQL. GitHub Actions deploys the Spring Boot application as a Docker container.

## Prerequisites

Local/control machine:

- Git
- Ansible
- SSH private key for the Oracle host

Oracle host:

- Linux user with sudo access, usually `ubuntu`
- SSH reachable from the control machine and GitHub Actions
- Docker installed
- Port `7008` allowed by the OS firewall and Oracle cloud ingress rules

GitHub:

- Repository secrets configured under Settings -> Secrets and variables -> Actions
- Docker Hub account or compatible Docker registry account

## Step 1: Configure Ansible Inventory

Edit `ansible/hosts.ini`:

```ini
[webservers]
oracle-prod ansible_host=<ORACLE_HOST> ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/oracle_key
```

Test connectivity:

```bash
cd ansible
ansible -i hosts.ini all -m ping
```

## Step 2: Create Ansible Vault Values

```bash
cd ansible
ansible-vault create vars/vault.yml
```

Use strong values:

```yaml
vault_mysql_root_password: "strong-root-password"
vault_db_password: "strong-receipt-user-password"
vault_jwt_secret: "long-random-jwt-secret"
```

`vault_db_password` must match the `DB_PASSWORD` GitHub secret used by the Docker deployment.

## Step 3: Provision MySQL

```bash
cd ansible
bash deploy.sh mysql
```

Or run the playbook directly:

```bash
cd ansible
ansible-playbook deploy.yml \
  -i hosts.ini \
  --ask-vault-pass \
  --ask-become-pass
```

Verify on the Oracle host:

```bash
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

## Step 4: Configure GitHub Actions Secrets

Required deployment secrets:

| Secret | Purpose |
| --- | --- |
| `DOCKER_USERNAME` | Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub password or access token |
| `ORACLE_HOST` | Oracle host IP or DNS name |
| `ORACLE_USERNAME` | SSH username, usually `ubuntu` |
| `SSH_PRIVATE_KEY` | Private key used to connect to the Oracle host |
| `SSH_PORT` | SSH port, usually `22` |
| `DB_PASSWORD` | MySQL password for `receipt_user` |
| `JWT_SECRET` | JWT signing secret |
| `GOOGLE_CLIENT_ID` | Google login client ID |
| `GEMINI_API_KEY` | Gemini API key |
| `AWS_S3_BUCKET` | S3 bucket name |
| `AWS_S3_REGION` | S3 bucket region |
| `AWS_ACCESS_KEY_ID` | AWS access key |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key |
| `MAIL_PASSWORD` | SMTP password |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `STRIPE_PRICE_PRO_MONTHLY` | Stripe monthly price ID |
| `STRIPE_PRICE_PRO_YEARLY` | Stripe yearly price ID |

## Step 5: Deploy the Application

Push to `master`:

```bash
git push origin master
```

The workflow in `.github/workflows/deploy.yml` will:

1. Check out the code.
2. Set up JDK 17.
3. Build with Maven.
4. Build and push a Docker image for `linux/amd64` and `linux/arm64`.
5. SSH into the Oracle host.
6. Pull the latest image.
7. Replace the `ai-receipt-app` container.

## Runtime Configuration

The production container uses:

```text
host port 7008 -> container port 8080
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/ai_receipt_db?...
SPRING_DATASOURCE_USERNAME=receipt_user
SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
```

The workflow maps `host.docker.internal` to Docker's `host-gateway`, allowing the container to reach MySQL on the host.

## Verification

On the Oracle host:

```bash
docker ps
docker logs -f ai-receipt-app
curl http://localhost:7008/api/
```

From your machine:

```bash
curl http://<ORACLE_HOST>:7008/api/
```

## Operations

Restart the app:

```bash
ssh ubuntu@<ORACLE_HOST>
docker restart ai-receipt-app
```

View logs:

```bash
docker logs -f ai-receipt-app
```

Check database:

```bash
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT NOW();"
```

## Troubleshooting

### Container does not start

```bash
docker logs ai-receipt-app
docker inspect ai-receipt-app
```

Check missing secrets, invalid database credentials, and malformed environment variables.

### Database connection fails

```bash
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
docker exec ai-receipt-app env | grep SPRING_DATASOURCE
```

Confirm `DB_PASSWORD` matches the MySQL password created by Ansible.

### Public endpoint is unreachable

```bash
curl http://localhost:7008/api/
sudo ss -tlnp | grep 7008
```

Check Oracle cloud security rules, OS firewall rules, and container port mapping.

### Docker image pull fails

Check Docker Hub credentials, repository name, and whether GitHub Actions pushed the image successfully.

## Maintenance

Weekly:

- Check `docker ps`.
- Check `docker logs ai-receipt-app`.
- Check `sudo systemctl status mysql`.

Monthly:

- Back up MySQL.
- Check disk usage with `df -h`.
- Clean unused Docker images if needed.

```bash
mysqldump -u receipt_user -p ai_receipt_db > ai_receipt_db_backup.sql
docker image prune
```
