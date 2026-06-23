# Deployment Checklist

Use this checklist before and after deploying AI Receipt Backend to the Oracle host.

## Local Preflight

- [ ] Git is installed.
- [ ] Ansible is installed.
- [ ] SSH key can connect to the Oracle host.
- [ ] `ansible/hosts.ini` points to the correct host.
- [ ] `ansible/vars/vault.yml` exists and is encrypted.
- [ ] `.env` is not committed.

Test SSH:

```bash
ssh -i ~/.ssh/oracle_key ubuntu@<ORACLE_HOST> "echo SSH OK"
```

Test Ansible:

```bash
cd ansible
ansible -i hosts.ini all -m ping
```

## MySQL Provisioning

- [ ] `vault_mysql_root_password` is set.
- [ ] `vault_db_password` is set.
- [ ] `vault_jwt_secret` is set.
- [ ] `bash ansible/deploy.sh mysql` completes successfully.
- [ ] MySQL service is running on the Oracle host.
- [ ] `ai_receipt_db` exists.
- [ ] `receipt_user` can connect.

Verify:

```bash
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

## GitHub Secrets

- [ ] `DOCKER_USERNAME`
- [ ] `DOCKER_PASSWORD`
- [ ] `ORACLE_HOST`
- [ ] `ORACLE_USERNAME`
- [ ] `SSH_PRIVATE_KEY`
- [ ] `SSH_PORT`
- [ ] `DB_PASSWORD`
- [ ] `JWT_SECRET`
- [ ] `GOOGLE_CLIENT_ID`
- [ ] `GEMINI_API_KEY`
- [ ] `AWS_S3_BUCKET`
- [ ] `AWS_S3_REGION`
- [ ] `AWS_ACCESS_KEY_ID`
- [ ] `AWS_SECRET_ACCESS_KEY`
- [ ] `MAIL_PASSWORD`
- [ ] `STRIPE_SECRET_KEY`
- [ ] `STRIPE_WEBHOOK_SECRET`
- [ ] `STRIPE_PRICE_PRO_MONTHLY`
- [ ] `STRIPE_PRICE_PRO_YEARLY`

Important:

- [ ] `DB_PASSWORD` matches `vault_db_password`.
- [ ] `JWT_SECRET` is long and production-safe.
- [ ] AWS, Stripe, Gemini, Google, and mail values are production values where required.

## GitHub Actions Deployment

- [ ] Changes are pushed to `master`.
- [ ] The build job succeeds.
- [ ] The Docker image is pushed to Docker Hub.
- [ ] The deploy job connects to the Oracle host.
- [ ] The old `ai-receipt-app` container is removed.
- [ ] The new `ai-receipt-app` container starts.

## Runtime Verification

On the Oracle host:

```bash
docker ps | grep ai-receipt-app
docker logs --tail 100 ai-receipt-app
curl http://localhost:7008/api/
```

From your machine:

```bash
curl http://<ORACLE_HOST>:7008/api/
```

Database:

```bash
mysql -u receipt_user -p ai_receipt_db -e "SELECT NOW();"
```

## Troubleshooting Checklist

If GitHub Actions build fails:

- [ ] Check Maven output.
- [ ] Check Java version is 17.
- [ ] Check Docker Hub login step.

If SSH deploy fails:

- [ ] Confirm `ORACLE_HOST`.
- [ ] Confirm `ORACLE_USERNAME`.
- [ ] Confirm `SSH_PRIVATE_KEY` includes the full private key.
- [ ] Confirm the Oracle host allows SSH from GitHub Actions runners.

If the container fails:

- [ ] Run `docker logs ai-receipt-app`.
- [ ] Check required environment variables.
- [ ] Check `DB_PASSWORD`.
- [ ] Check MySQL is running.
- [ ] Check port `7008` is free.

If the API is unreachable:

- [ ] Check `docker ps` port mapping.
- [ ] Check Oracle cloud ingress rules.
- [ ] Check OS firewall rules.
- [ ] Use `/api/` as the base path.

## Maintenance Checklist

Weekly:

- [ ] `docker ps` shows `ai-receipt-app`.
- [ ] `sudo systemctl status mysql` is healthy.
- [ ] Logs do not show recurring errors.

Monthly:

- [ ] Back up MySQL.
- [ ] Check disk usage.
- [ ] Prune unused Docker images if needed.
- [ ] Review GitHub secrets for rotation needs.

Backup command:

```bash
mysqldump -u receipt_user -p ai_receipt_db > ai_receipt_db_backup.sql
```
