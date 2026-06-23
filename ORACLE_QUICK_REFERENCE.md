# Oracle Deployment Quick Reference

Use this page when you already understand the deployment model and only need commands and required values.

## Architecture

```text
Developer machine
  -> git push origin master
GitHub Actions
  -> Maven build
  -> Docker multi-arch image build
  -> Docker Hub push
  -> SSH to Oracle host
Oracle host
  -> MySQL runs on the host
  -> ai-receipt-app runs in Docker
```

Production port mapping:

```text
http://<ORACLE_HOST>:7008 -> container port 8080 -> Spring context path /api
```

## First-Time Setup

### 1. Provision MySQL

```bash
cd ansible
ansible-vault create vars/vault.yml
bash deploy.sh mysql
```

Required vault values:

```yaml
vault_mysql_root_password: "strong-root-password"
vault_db_password: "strong-receipt-user-password"
vault_jwt_secret: "long-random-jwt-secret"
```

### 2. Configure GitHub Actions Secrets

Required for image build and remote deployment:

```text
DOCKER_USERNAME
DOCKER_PASSWORD
ORACLE_HOST
ORACLE_USERNAME
SSH_PRIVATE_KEY
SSH_PORT
DB_PASSWORD
JWT_SECRET
GOOGLE_CLIENT_ID
GEMINI_API_KEY
AWS_S3_BUCKET
AWS_S3_REGION
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
MAIL_PASSWORD
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
STRIPE_PRICE_PRO_MONTHLY
STRIPE_PRICE_PRO_YEARLY
```

`DB_PASSWORD` must match `vault_db_password`. `JWT_SECRET` should match the secret used for JWT signing.

### 3. Deploy the App

```bash
git push origin master
```

## Verify on the Oracle Host

```bash
ssh ubuntu@<ORACLE_HOST>

sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"

docker ps
docker logs -f ai-receipt-app
curl http://localhost:7008/api/
```

## Manual Container Restart

```bash
ssh ubuntu@<ORACLE_HOST>
docker restart ai-receipt-app
docker logs -f ai-receipt-app
```

## Common Failures

| Symptom | Likely cause | Check |
| --- | --- | --- |
| Container exits immediately | Missing or wrong environment variable | `docker logs ai-receipt-app` |
| Database authentication fails | `DB_PASSWORD` does not match MySQL user password | Compare GitHub secret with Ansible vault |
| App cannot reach MySQL | Host gateway mapping or MySQL bind/firewall issue | Check workflow `--add-host=host.docker.internal:host-gateway` and MySQL status |
| Docker pull fails | Docker Hub credentials or image name issue | Check `DOCKER_USERNAME` and `DOCKER_PASSWORD` |
| Public URL unavailable | Port 7008 blocked | Check Oracle firewall and cloud ingress rules |

## Related Docs

- `ORACLE_DEPLOYMENT.md`
- `DEPLOYMENT_CHECKLIST.md`
- `ANSIBLE_DEPLOYMENT.md`
- `.github/workflows/deploy.yml`
